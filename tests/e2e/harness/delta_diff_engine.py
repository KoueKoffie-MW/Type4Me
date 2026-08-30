"""
Live Delta-Diff Engine for Real-Time Streaming Speech Recognition.
Computes Longest Common Prefix (LCP), backspaces needed, and suffix to append,
with full 32-bit Unicode Code Point safety.
"""
from dataclasses import dataclass
from typing import List, Tuple


@dataclass(frozen=True)
class DiffResult:
    common_prefix_length: int  # in Unicode code points
    backspaces_needed: int     # number of backspace keystrokes
    text_to_append: str        # newly added suffix string


class DeltaDiffEngine:
    """
    Computes mathematical diff between old text and new speech hypothesis.
    """

    @staticmethod
    def _to_code_points(text: str) -> List[int]:
        """Converts string into list of 32-bit integer Unicode code points."""
        return [ord(c) for c in text]

    @staticmethod
    def _from_code_points(points: List[int]) -> str:
        """Reconstructs Unicode string from list of integer code points."""
        return "".join(chr(p) for p in points)

    def compute_diff(self, old_text: str, new_text: str) -> DiffResult:
        """
        Computes LCP, backspaces needed, and text to append.
        Operates strictly on Unicode code points to preserve multi-byte emoji integrity.
        """
        old_points = self._to_code_points(old_text)
        new_points = self._to_code_points(new_text)

        lcp = 0
        min_len = min(len(old_points), len(new_points))

        while lcp < min_len and old_points[lcp] == new_points[lcp]:
            lcp += 1

        backspaces_needed = len(old_points) - lcp
        append_points = new_points[lcp:]
        text_to_append = self._from_code_points(append_points)

        return DiffResult(
            common_prefix_length=lcp,
            backspaces_needed=backspaces_needed,
            text_to_append=text_to_append
        )
