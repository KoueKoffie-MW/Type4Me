"""
AI Text Rewriter Pipeline Simulator.
Implements Google GenAI SDK (gemini-3.7-flash / gemini-3.5-flash-lite) and
on-device LiteRT-LM contracts with built-in and custom prompt presets,
and HybridAiOrchestrator with air-gap fallback policies.
"""
from dataclasses import dataclass
from enum import Enum
from typing import Optional, Dict, Any, Callable, List, Tuple
import re


class AiEngineType(Enum):
    GEMINI_3_7_FLASH = "gemini-3.7-flash"
    GEMINI_3_5_FLASH_LITE = "gemini-3.5-flash-lite"
    LITERT_ON_DEVICE = "gemma-2b-it-q4"


class AiPolicy(Enum):
    AIR_GAP_STRICT = "AIR_GAP_STRICT"   # Local SLM only, zero network egress
    LOCAL_PREF = "LOCAL_PREF"           # Try Local, fallback to Cloud if allowed
    CLOUD_PREF = "CLOUD_PREF"           # Try Cloud, fallback to Local


@dataclass
class PromptPreset:
    id: str
    title: str = ""
    description: str = ""
    system_prompt: str = ""
    user_prompt_template: str = "{INPUT_TEXT}"
    temperature: float = 0.2
    is_builtin: bool = False
    order_index: int = 0

    def format_user_prompt(self, text: str) -> str:
        if "{INPUT_TEXT}" in self.user_prompt_template:
            return self.user_prompt_template.replace("{INPUT_TEXT}", text)
        return f"{self.user_prompt_template}\n\n{text}"


# Built-in Prompt Presets Definitions
BUILTIN_CLEAN_POLISH = PromptPreset(
    id="builtin_clean_polish",
    title="✨ Clean & Polish",
    description="Fixes grammar, punctuation, and removes filler words while preserving original meaning.",
    system_prompt=(
        "You are an expert speech transcription editor. Your task is to clean up raw spoken transcripts. "
        "Fix grammar, punctuation, capitalization, and spelling. Remove speech disfluencies, filler words "
        "(um, uh, like, you know, ehm, halt, quasi), stuttering, and accidental word repetitions. "
        "Preserve the original language (German, English, Afrikaans), meaning, and tone. "
        "Output ONLY the polished text without any conversational filler or preambles."
    ),
    is_builtin=True,
    order_index=1
)

BUILTIN_BUSINESS_GERMAN = PromptPreset(
    id="builtin_business_german",
    title="💼 Business German",
    description="Transforms speech into formal, polite business German (Höflichkeitsform 'Sie').",
    system_prompt=(
        "Du bist ein professioneller Assistent für geschäftliche Korrespondenz auf Deutsch. "
        "Wandle den diktierten Text in elegantes, präzises und fehlerfreies Geschäftsdeutsch "
        "(Höflichkeitsform 'Sie') um. Korrigiere Grammatik, Zeichensetzung und Rechtschreibung. "
        "Formuliere Sätze klar und professionell, ohne den sachlichen Inhalt zu verändern. "
        "Gib AUSSCHLIESSLICH den überarbeiteten Text ohne Einleitung oder Begleitkommentare aus."
    ),
    is_builtin=True,
    order_index=2
)

BUILTIN_CODE_COMMENTS = PromptPreset(
    id="builtin_code_comments",
    title="💻 Technical Code Comments",
    description="Converts developer explanations into standard code comments and docstrings.",
    system_prompt=(
        "You are a senior software architect. Convert the spoken developer explanation into clean, "
        "concise, standard technical documentation, code comments, or docstrings (supporting Javadoc, "
        "KDoc, Python docstrings, or MATLAB). Use clear technical terminology, standard comment "
        "formatting (e.g. //, /** */, #, %%), and imperative mood. Output ONLY the comment text."
    ),
    is_builtin=True,
    order_index=3
)


class TextRewriter:
    """Base interface for text rewriting engines."""
    engine_type: AiEngineType

    def is_available(self) -> bool:
        raise NotImplementedError

    def rewrite(self, text: str, preset: PromptPreset) -> Tuple[bool, str, Optional[str]]:
        """Returns (success, result_text, error_message)"""
        raise NotImplementedError


class GeminiRemoteRewriter(TextRewriter):
    """
    Simulates Google GenAI SDK remote calls.
    """

    def __init__(
        self,
        api_key_provider: Callable[[], Optional[str]],
        engine_type: AiEngineType = AiEngineType.GEMINI_3_7_FLASH,
        simulated_status_code: int = 200
    ):
        self.api_key_provider = api_key_provider
        self.engine_type = engine_type
        self.simulated_status_code = simulated_status_code

    def is_available(self) -> bool:
        key = self.api_key_provider()
        return bool(key and key.strip())

    def rewrite(self, text: str, preset: PromptPreset) -> Tuple[bool, str, Optional[str]]:
        if not text.strip():
            return True, "", None

        if not self.is_available():
            return False, "", "Gemini API key is not configured in settings."

        if self.simulated_status_code == 429:
            return False, "", "Google GenAI API Rate limit exceeded (HTTP 429). Please retry shortly."

        if self.simulated_status_code == 500:
            return False, "", "Google GenAI Internal Server Error (HTTP 500)."

        # Deterministic simulation of AI rewrite based on preset rules
        transformed = text.strip()

        if preset.id == "builtin_clean_polish":
            # Remove filler words: "um", "uh", "ehm", "like", "you know", "halt", "quasi"
            fillers = [r"\bum\b", r"\buh\b", r"\behm\b", r"\blike\b", r"\byou know\b", r"\bhalt\b", r"\bquasi\b"]
            for f in fillers:
                transformed = re.sub(f, "", transformed, flags=re.IGNORECASE)
            # Remove repeated words (e.g. "the the")
            transformed = re.sub(r"\b(\w+)\s+\1\b", r"\1", transformed, flags=re.IGNORECASE)
            # Normalize whitespace
            transformed = re.sub(r"\s+", " ", transformed).strip()
            # Capitalize first letter and ensure ending punctuation
            if transformed and transformed[0].islower():
                transformed = transformed[0].upper() + transformed[1:]
            if transformed and not transformed[-1] in ".!?":
                transformed += "."

        elif preset.id == "builtin_business_german":
            # Convert informal German to formal business German
            replacements = {
                "hallo": "Sehr geehrte Damen und Herren,",
                "hi": "Guten Tag,",
                "schick mir": "bitte senden Sie mir",
                "ich will": "ich möchte",
                "danke dir": "vielen Dank für Ihre Unterstützung",
                "tschuess": "Mit freundlichen Grüßen",
                "tschüss": "Mit freundlichen Grüßen",
            }
            for k, v in replacements.items():
                transformed = re.sub(r"\b" + k + r"\b", v, transformed, flags=re.IGNORECASE)
            if transformed and not transformed[-1] in ".!?":
                transformed += "."

        elif preset.id == "builtin_code_comments":
            # Convert into code comment
            lines = transformed.splitlines()
            formatted_lines = [f"// {line.strip()}" for line in lines if line.strip()]
            transformed = "\n".join(formatted_lines)

        else:
            # Custom preset transformation
            transformed = preset.format_user_prompt(transformed)

        return True, transformed, None


class LiteRtOnDeviceRewriter(TextRewriter):
    """
    Simulates on-device LiteRT-LM (Gemma 2B INT4) execution.
    """

    def __init__(self, model_file_exists: bool = True):
        self.engine_type = AiEngineType.LITERT_ON_DEVICE
        self.model_file_exists = model_file_exists

    def is_available(self) -> bool:
        return self.model_file_exists

    def rewrite(self, text: str, preset: PromptPreset) -> Tuple[bool, str, Optional[str]]:
        if not text.strip():
            return True, "", None

        if not self.is_available():
            return False, "", "LiteRT on-device model weights not installed on device."

        user_prompt = preset.format_user_prompt(text)
        gemma_prompt = (
            f"<start_of_turn>system\n{preset.system_prompt}<end_of_turn>\n"
            f"<start_of_turn>user\n{user_prompt}<end_of_turn>\n"
            f"<start_of_turn>model\n"
        )

        output = text.strip()
        fillers = [r"\bum\b", r"\buh\b", r"\behm\b"]
        for f in fillers:
            output = re.sub(f, "", output, flags=re.IGNORECASE)
        output = re.sub(r"\s+", " ", output).strip()

        return True, output, None


class HybridAiOrchestrator:
    """
    Orchestrates execution between On-Device (LiteRT-LM) and Cloud (Gemini)
    according to enterprise privacy policies.
    """

    def __init__(
        self,
        local_rewriter: LiteRtOnDeviceRewriter,
        cloud_rewriter: GeminiRemoteRewriter,
        policy: AiPolicy = AiPolicy.LOCAL_PREF
    ):
        self.local_rewriter = local_rewriter
        self.cloud_rewriter = cloud_rewriter
        self.policy = policy

    def rewrite(self, text: str, preset: PromptPreset) -> Tuple[bool, str, Optional[str]]:
        if self.policy == AiPolicy.AIR_GAP_STRICT:
            if not self.local_rewriter.is_available():
                return False, "", "Air-Gap mode strictly forbids cloud egress, but local model is unavailable."
            return self.local_rewriter.rewrite(text, preset)

        elif self.policy == AiPolicy.LOCAL_PREF:
            if self.local_rewriter.is_available():
                ok, res, err = self.local_rewriter.rewrite(text, preset)
                if ok:
                    return ok, res, err
            # Fallback to cloud
            if self.cloud_rewriter.is_available():
                return self.cloud_rewriter.rewrite(text, preset)
            return False, "", "Both local and cloud AI engines are unavailable."

        else: # CLOUD_PREF
            if self.cloud_rewriter.is_available():
                ok, res, err = self.cloud_rewriter.rewrite(text, preset)
                if ok:
                    return ok, res, err
            # Fallback to local
            if self.local_rewriter.is_available():
                return self.local_rewriter.rewrite(text, preset)
            return False, "", "Both cloud and local AI engines are unavailable."
