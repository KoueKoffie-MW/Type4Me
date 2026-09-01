"""
Polymorphic MacroAction & Runner Engine Simulator.
Supports:
- TypeString, KeyCombination, Delay, PromptVariable, ClipboardPaste
- JSON serialization/deserialization
- Step-by-step execution with interpolation and cancellation
- Emergency release guard on failure or abort
"""
from dataclasses import dataclass, field
from enum import Enum
from typing import List, Dict, Optional, Callable, Any
import json
import time

from .dispatcher_simulator import KeystrokeDispatcher
from .variable_parser_simulator import VariableParser, InterpolationContext
from .keymap_engine import HidKeyStroke


class MacroExecutionStatus(Enum):
    IDLE = "IDLE"
    STARTING = "STARTING"
    RUNNING = "RUNNING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"


@dataclass
class MacroExecutionState:
    status: MacroExecutionStatus
    current_step: int = 0
    total_steps: int = 0
    current_action_name: str = ""
    error_message: Optional[str] = None


class MacroAction:
    action_type: str = "Base"

    def to_dict(self) -> Dict[str, Any]:
        raise NotImplementedError

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "MacroAction":
        a_type = data.get("type", "")
        if a_type == "TypeString":
            return TypeStringAction(text=data.get("text", ""))
        elif a_type == "KeyCombination":
            return KeyCombinationAction(modifier=data.get("modifier", 0), usage_id=data.get("usageId", 0))
        elif a_type == "Delay":
            return DelayAction(duration_ms=data.get("durationMs", 50))
        elif a_type == "PromptVariable":
            return PromptVariableAction(
                variable_name=data.get("variableName", ""),
                label=data.get("label", ""),
                default_value=data.get("defaultValue", "")
            )
        elif a_type == "ClipboardPaste":
            return ClipboardPasteAction(bracketed=data.get("bracketed", True))
        else:
            raise ValueError(f"Unknown MacroAction type: {a_type}")


@dataclass
class TypeStringAction(MacroAction):
    text: str
    action_type: str = "TypeString"

    def to_dict(self) -> Dict[str, Any]:
        return {"type": "TypeString", "text": self.text}


@dataclass
class KeyCombinationAction(MacroAction):
    modifier: int
    usage_id: int
    action_type: str = "KeyCombination"

    def to_dict(self) -> Dict[str, Any]:
        return {"type": "KeyCombination", "modifier": self.modifier, "usageId": self.usage_id}


@dataclass
class DelayAction(MacroAction):
    duration_ms: int
    action_type: str = "Delay"

    def to_dict(self) -> Dict[str, Any]:
        return {"type": "Delay", "durationMs": self.duration_ms}


@dataclass
class PromptVariableAction(MacroAction):
    variable_name: str
    label: str = ""
    default_value: str = ""
    action_type: str = "PromptVariable"

    def to_dict(self) -> Dict[str, Any]:
        return {
            "type": "PromptVariable",
            "variableName": self.variable_name,
            "label": self.label or self.variable_name,
            "defaultValue": self.default_value
        }


@dataclass
class ClipboardPasteAction(MacroAction):
    bracketed: bool = True
    action_type: str = "ClipboardPaste"

    def to_dict(self) -> Dict[str, Any]:
        return {"type": "ClipboardPaste", "bracketed": self.bracketed}


class MacroRunner:
    """
    Executes polymorphic MacroAction sequences with variable interpolation.
    """

    def __init__(self, dispatcher: KeystrokeDispatcher):
        self.dispatcher = dispatcher
        self.state = MacroExecutionState(status=MacroExecutionStatus.IDLE)
        self._listeners: List[Callable[[MacroExecutionState], None]] = []

    def add_listener(self, listener: Callable[[MacroExecutionState], None]):
        self._listeners.append(listener)
        listener(self.state)

    def _emit(self, **kwargs):
        data = {
            "status": self.state.status,
            "current_step": self.state.current_step,
            "total_steps": self.state.total_steps,
            "current_action_name": self.state.current_action_name,
            "error_message": self.state.error_message
        }
        data.update(kwargs)
        self.state = MacroExecutionState(**data)
        for l in self._listeners:
            l(self.state)

    @staticmethod
    def parse_actions_json(actions_json: str) -> List[MacroAction]:
        raw_list = json.loads(actions_json)
        return [MacroAction.from_dict(item) for item in raw_list]

    def execute(
        self,
        actions: List[MacroAction],
        context: Optional[InterpolationContext] = None,
        cancel_check: Optional[Callable[[], bool]] = None
    ) -> bool:
        ctx = context or InterpolationContext()
        total = len(actions)
        self._emit(status=MacroExecutionStatus.STARTING, current_step=0, total_steps=total, error_message=None)

        try:
            for idx, action in enumerate(actions):
                if cancel_check and cancel_check():
                    self._emit(status=MacroExecutionStatus.CANCELLED, current_step=idx, error_message="Cancelled by user")
                    return False

                self._emit(
                    status=MacroExecutionStatus.RUNNING,
                    current_step=idx + 1,
                    total_steps=total,
                    current_action_name=action.action_type
                )

                if isinstance(action, TypeStringAction):
                    # Interpolate template variables before typing
                    interpolated = VariableParser.parse(action.text, ctx)
                    ok = self.dispatcher.dispatch_burst(interpolated)
                    if not ok:
                        self._emit(status=MacroExecutionStatus.FAILED, error_message="Failed to type string")
                        return False

                elif isinstance(action, KeyCombinationAction):
                    ok = self.dispatcher.send_single_keystroke(action.modifier, action.usage_id)
                    if not ok:
                        self._emit(status=MacroExecutionStatus.FAILED, error_message="Failed to send key combination")
                        return False

                elif isinstance(action, DelayAction):
                    self.dispatcher._advance_time(action.duration_ms)

                elif isinstance(action, PromptVariableAction):
                    # Variable value should be present in context.prompt_values
                    val = ctx.prompt_values.get(action.variable_name, action.default_value)
                    if val:
                        ok = self.dispatcher.dispatch_burst(val)
                        if not ok:
                            self._emit(status=MacroExecutionStatus.FAILED, error_message="Failed to dispatch prompt variable")
                            return False

                elif isinstance(action, ClipboardPasteAction):
                    clip_text = ctx.clipboard_text
                    if clip_text:
                        ok = self.dispatcher.stream_clipboard_to_host(clip_text, bracketed_paste=action.bracketed)
                        if not ok:
                            self._emit(status=MacroExecutionStatus.FAILED, error_message="Failed to stream clipboard")
                            return False

            self._emit(status=MacroExecutionStatus.COMPLETED, current_step=total, total_steps=total)
            return True

        except Exception as e:
            self._emit(status=MacroExecutionStatus.FAILED, error_message=str(e))
            return False
        finally:
            self.dispatcher.send_emergency_release()
