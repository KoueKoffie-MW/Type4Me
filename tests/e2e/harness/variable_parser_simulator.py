"""
Single-Pass Mustache Variable AST Parser & Interpolation Engine Simulator.
Handles:
- {{timestamp}}, {{iso_date}}, {{uuid}}, {{clipboard}}, {{prompt:LABEL}}, {{prompt:LABEL:DEFAULT}}
- Escaping via \\{\\{, \\}\\} or \\{{, \\}}
- Nested braces and code-point safe token resolution
- Prompt parameter extraction for dynamic UI modal dialogs
"""
from dataclasses import dataclass, field
from typing import List, Dict, Optional, Tuple, Any
import datetime
import uuid
import time
import re


@dataclass(frozen=True)
class PromptDescriptor:
    key: str
    label: str
    default_value: str = ""


@dataclass
class InterpolationContext:
    clipboard_text: str = ""
    prompt_values: Dict[str, str] = field(default_factory=dict)
    custom_variables: Dict[str, str] = field(default_factory=dict)
    fixed_timestamp: Optional[int] = None
    fixed_iso_date: Optional[str] = None
    fixed_uuid: Optional[str] = None


class VariableParser:
    """
    Single-pass AST Tokenizer & Interpolation Engine.
    """

    @staticmethod
    def extract_prompts(template: str) -> List[PromptDescriptor]:
        """
        Extracts all {{prompt:...}} placeholders from the template without executing full interpolation.
        Returns unique PromptDescriptors in order of appearance.
        """
        prompts: List[PromptDescriptor] = []
        seen_keys = set()

        pattern = re.compile(r"(?<!\\)\{\{\s*prompt:([^\}:]+)(?::([^\}]+))?\s*\}\}")
        for match in pattern.finditer(template):
            raw_label = match.group(1).strip()
            raw_default = match.group(2).strip() if match.group(2) else ""
            key = raw_label.lower().replace(" ", "_")
            if key not in seen_keys:
                seen_keys.add(key)
                prompts.append(PromptDescriptor(key=key, label=raw_label, default_value=raw_default))

        return prompts

    @classmethod
    def parse(cls, template: str, context: Optional[InterpolationContext] = None) -> str:
        """
        Parses and interpolates template in a single pass with escaping and code-point safety.
        """
        ctx = context or InterpolationContext()
        chars = list(template)
        n = len(chars)
        i = 0
        out: List[str] = []

        while i < n:
            # 1. Check for escaped opening braces:
            # Format A: \{\{ (4 chars: '\', '{', '\', '{')
            if i + 3 < n and chars[i] == '\\' and chars[i+1] == '{' and chars[i+2] == '\\' and chars[i+3] == '{':
                out.append("{{")
                i += 4
                continue
            # Format B: \{{ (3 chars: '\', '{', '{')
            if i + 2 < n and chars[i] == '\\' and chars[i+1] == '{' and chars[i+2] == '{':
                out.append("{{")
                i += 3
                continue

            # 2. Check for escaped closing braces:
            # Format A: \}\} (4 chars: '\', '}', '\', '}')
            if i + 3 < n and chars[i] == '\\' and chars[i+1] == '}' and chars[i+2] == '\\' and chars[i+3] == '}':
                out.append("}}")
                i += 4
                continue
            # Format B: \}} (3 chars: '\', '}', '}')
            if i + 2 < n and chars[i] == '\\' and chars[i+1] == '}' and chars[i+2] == '}':
                out.append("}}")
                i += 3
                continue

            # 3. Check for opening variable tag: {{
            if i + 1 < n and chars[i] == '{' and chars[i+1] == '{':
                close_idx = -1
                j = i + 2
                while j + 1 < n:
                    if chars[j] == '}' and chars[j+1] == '}' and (j == 0 or chars[j-1] != '\\'):
                        close_idx = j
                        break
                    j += 1

                if close_idx != -1:
                    raw_token = "".join(chars[i+2:close_idx]).strip()
                    resolved = cls._resolve_token(raw_token, ctx)
                    out.append(resolved)
                    i = close_idx + 2
                    continue
                else:
                    # Unclosed opening brace -> preserve as literal
                    out.append(chars[i])
                    i += 1
                    continue

            out.append(chars[i])
            i += 1

        return "".join(out)

    @classmethod
    def _resolve_token(cls, token: str, ctx: InterpolationContext) -> str:
        # Strip potential inner braces for nested structures
        clean_token = token.lstrip("{").rstrip("}").strip()
        token_lower = clean_token.lower()

        # 1. Standard Built-in Variables
        if token_lower == "timestamp":
            if ctx.fixed_timestamp is not None:
                return str(ctx.fixed_timestamp)
            return str(int(time.time()))

        if token_lower in ["iso_date", "isodate", "date"]:
            if ctx.fixed_iso_date is not None:
                return ctx.fixed_iso_date
            return datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

        if token_lower == "uuid":
            if ctx.fixed_uuid is not None:
                return ctx.fixed_uuid
            return str(uuid.uuid4())

        if token_lower == "clipboard":
            return ctx.clipboard_text

        # 2. Prompt Parameters: prompt:LABEL or prompt:LABEL:DEFAULT
        if token_lower.startswith("prompt:"):
            parts = clean_token[7:].split(":", 1)
            label = parts[0].strip()
            default_val = parts[1].strip() if len(parts) > 1 else ""
            key = label.lower().replace(" ", "_")

            if key in ctx.prompt_values and ctx.prompt_values[key]:
                return ctx.prompt_values[key]
            if label in ctx.prompt_values and ctx.prompt_values[label]:
                return ctx.prompt_values[label]
            return default_val

        # 3. Custom Variable Dictionary
        if clean_token in ctx.custom_variables:
            return ctx.custom_variables[clean_token]
        if token_lower in ctx.custom_variables:
            return ctx.custom_variables[token_lower]

        # 4. Unknown variable -> return placeholder as-is: {{clean_token}}
        return f"{{{{{clean_token}}}}}"
