"""
Dual-Tier Desktop Context Companion & AI Ingestion Simulator.
Simulates:
- Desktop Companion HTTP service (GET /context on port 8765)
- Active window title, selected text, and process name tracking
- CompanionClient HTTP polling & error handling
- Dual-tier Gemini prompt enrichment with 100% air-gap fallback
"""
from dataclasses import dataclass
from typing import Optional, Dict, Any, Tuple
import json
import time


@dataclass(frozen=True)
class DesktopContext:
    window_title: str
    selected_text: str
    process_name: str
    timestamp: int

    def to_dict(self) -> Dict[str, Any]:
        return {
            "window_title": self.window_title,
            "selected_text": self.selected_text,
            "process_name": self.process_name,
            "timestamp": self.timestamp
        }

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "DesktopContext":
        return cls(
            window_title=data.get("window_title", ""),
            selected_text=data.get("selected_text", ""),
            process_name=data.get("process_name", ""),
            timestamp=data.get("timestamp", int(time.time()))
        )


class DesktopCompanionServerSimulator:
    """
    Simulates the zero-dependency Python/PowerShell desktop companion server.
    """

    def __init__(self, port: int = 8765):
        self.port = port
        self.is_running = True
        self.window_title = "VS Code - main.py"
        self.selected_text = "def calculate_hash(data: bytes) -> str:\n    pass"
        self.process_name = "Code.exe"
        self.force_http_status: int = 200
        self.simulate_timeout: bool = False

    def get_context_response(self) -> Tuple[int, Optional[str]]:
        if not self.is_running:
            return 0, None  # Connection Refused

        if self.simulate_timeout:
            return -1, None  # Socket Timeout

        if self.force_http_status != 200:
            return self.force_http_status, json.dumps({"error": f"HTTP {self.force_http_status}"})

        payload = DesktopContext(
            window_title=self.window_title,
            selected_text=self.selected_text,
            process_name=self.process_name,
            timestamp=int(time.time())
        )
        return 200, json.dumps(payload.to_dict())


class CompanionClientSimulator:
    """
    Simulates Android CompanionClient communicating with the desktop workstation.
    """

    def __init__(self, server: Optional[DesktopCompanionServerSimulator] = None):
        self.server = server

    def fetch_active_context(
        self,
        host_ip: str = "127.0.0.1",
        port: int = 8765,
        timeout_s: float = 1.0
    ) -> Tuple[bool, Optional[DesktopContext], Optional[str]]:
        """
        Polls GET /context from the workstation.
        Returns (success, DesktopContext, error_message).
        """
        if self.server is None or not self.server.is_running:
            return False, None, "Connection refused: Companion script is not running on host workstation."

        status, body = self.server.get_context_response()
        if status == 0:
            return False, None, "Connection refused"
        if status == -1:
            return False, None, "Connection timed out"
        if status != 200:
            return False, None, f"HTTP error {status}: {body}"

        try:
            data = json.loads(body)
            context = DesktopContext.from_dict(data)
            return True, context, None
        except Exception as e:
            return False, None, f"JSON parse error: {str(e)}"

    def enrich_prompt(
        self,
        user_input: str,
        system_prompt: str,
        context: Optional[DesktopContext] = None
    ) -> str:
        """
        Injects desktop context into the system/user prompt.
        If context is None or empty, cleanly falls back without decoration (air-gap safe).
        """
        if not context or (not context.window_title and not context.selected_text):
            # Pristine air-gap fallback
            return f"{system_prompt}\n\nUser Input: {user_input}"

        context_block = f"""--- HOST WORKSTATION CONTEXT ---
Active Application: {context.process_name} ({context.window_title})
Active Selection:
```
{context.selected_text}
```
--------------------------------"""

        return f"{system_prompt}\n\n{context_block}\n\nUser Input: {user_input}"
