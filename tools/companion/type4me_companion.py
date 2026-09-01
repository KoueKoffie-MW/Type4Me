#!/usr/bin/env python3
"""
Type4Me Zero-Install Desktop Context Companion
===============================================
A lightweight, zero-external-dependency background companion for Type4Me.
Broadcasts active window metadata, process name, and selected text over
local HTTP (default: 0.0.0.0:8765) to enrich Type4Me's Gemini prompt engineering.

Standard Library Only:
- http.server, socketserver, json, ctypes, subprocess, platform, argparse, sys, time, threading, urllib.parse

Dual-Tier Fallback Guarantee:
- If this companion is not running or unreachable, Type4Me on Android seamlessly
  falls back to pure voice transcription with 100% air-gap security.
"""

import argparse
import json
import os
import platform
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, HTTPServer
from socketserver import ThreadingMixIn
from typing import Any, Dict, Optional, Tuple
from urllib.parse import parse_qs, urlparse

VERSION = "2.0.0"
DEFAULT_HOST = "0.0.0.0"
DEFAULT_PORT = 8765


class DesktopContextExtractor:
    """Cross-platform active window and selected text extractor using OS APIs."""

    def __init__(self, debug: bool = False):
        self.debug = debug
        self.os_type = platform.system().lower()

    def _log(self, message: str) -> None:
        if self.debug:
            print(f"[DEBUG] [DesktopContext] {message}", file=sys.stderr)

    def extract_context(self) -> Dict[str, Any]:
        """Extracts active window title, process name, selected text, and timestamp."""
        timestamp_ms = int(time.time() * 1000)
        window_title = ""
        process_name = ""
        selected_text = ""

        try:
            if self.os_type == "windows":
                window_title, process_name, selected_text = self._extract_windows()
            elif self.os_type == "darwin":
                window_title, process_name, selected_text = self._extract_macos()
            elif self.os_type == "linux":
                window_title, process_name, selected_text = self._extract_linux()
            else:
                self._log(f"Unsupported OS platform: {self.os_type}")
        except Exception as e:
            self._log(f"Error during context extraction: {e}")

        return {
            "window_title": window_title.strip(),
            "process_name": process_name.strip(),
            "selected_text": selected_text.strip(),
            "timestamp": timestamp_ms,
        }

    # --------------------------------------------------------------------------
    # Windows Implementation (Win32 ctypes)
    # --------------------------------------------------------------------------
    def _extract_windows(self) -> Tuple[str, str, str]:
        import ctypes
        from ctypes import wintypes

        user32 = ctypes.windll.user32
        kernel32 = ctypes.windll.kernel32

        window_title = ""
        process_name = ""
        selected_text = ""

        # 1. Active Window Handle
        hwnd = user32.GetForegroundWindow()
        if not hwnd:
            return window_title, process_name, selected_text

        # 2. Window Title
        length = user32.GetWindowTextLengthW(hwnd)
        if length > 0:
            buff = ctypes.create_unicode_buffer(length + 1)
            user32.GetWindowTextW(hwnd, buff, length + 1)
            window_title = buff.value

        # 3. Process ID and Name
        pid = wintypes.DWORD()
        user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))
        if pid.value > 0:
            process_name = self._get_windows_process_name(pid.value)

        # 4. Selected Text (Clipboard fallback)
        selected_text = self._get_windows_clipboard_text()

        return window_title, process_name, selected_text

    def _get_windows_process_name(self, pid: int) -> str:
        import ctypes
        from ctypes import wintypes

        kernel32 = ctypes.windll.kernel32
        PROCESS_QUERY_LIMITED_INFORMATION = 0x1000
        PROCESS_QUERY_INFORMATION = 0x0400
        PROCESS_VM_READ = 0x0010

        h_process = kernel32.OpenProcess(
            PROCESS_QUERY_LIMITED_INFORMATION | PROCESS_QUERY_INFORMATION | PROCESS_VM_READ,
            False,
            pid
        )
        if not h_process:
            # Fallback to query limited information only
            h_process = kernel32.OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, False, pid)

        if h_process:
            try:
                # Try QueryFullProcessImageNameW
                buff = ctypes.create_unicode_buffer(1024)
                size = wintypes.DWORD(1024)
                if hasattr(kernel32, "QueryFullProcessImageNameW") and kernel32.QueryFullProcessImageNameW(
                    h_process, 0, buff, ctypes.byref(size)
                ):
                    full_path = buff.value
                    return os.path.basename(full_path)

                # Fallback to psapi.GetModuleFileNameExW
                psapi = ctypes.windll.psapi
                if psapi and hasattr(psapi, "GetModuleFileNameExW"):
                    if psapi.GetModuleFileNameExW(h_process, 0, buff, 1024) > 0:
                        return os.path.basename(buff.value)
            finally:
                kernel32.CloseHandle(h_process)

        # Fallback via tasklist if ctypes process query failed
        try:
            cmd = f'tasklist /FI "PID eq {pid}" /FO CSV /NH'
            out = os.popen(cmd).read()
            if out and "," in out:
                parts = out.strip().split(",")
                if parts:
                    return parts[0].strip('"\r\n')
        except Exception:
            pass

        return ""

    def _get_windows_clipboard_text(self) -> str:
        import ctypes

        user32 = ctypes.windll.user32
        kernel32 = ctypes.windll.kernel32
        CF_UNICODETEXT = 13

        if not user32.OpenClipboard(None):
            return ""

        text = ""
        try:
            if user32.IsClipboardFormatAvailable(CF_UNICODETEXT):
                h_glb = user32.GetClipboardData(CF_UNICODETEXT)
                if h_glb:
                    ptr = kernel32.GlobalLock(h_glb)
                    if ptr:
                        try:
                            text = ctypes.c_wchar_p(ptr).value or ""
                        finally:
                            kernel32.GlobalUnlock(h_glb)
        except Exception as e:
            self._log(f"Windows clipboard extraction error: {e}")
        finally:
            user32.CloseClipboard()

        return text

    # --------------------------------------------------------------------------
    # macOS Implementation (AppleScript / osascript)
    # --------------------------------------------------------------------------
    def _extract_macos(self) -> Tuple[str, str, str]:
        import subprocess

        window_title = ""
        process_name = ""
        selected_text = ""

        # AppleScript for active app and front window title
        script = """
        tell application "System Events"
            set frontApp to first application process whose frontmost is true
            set frontAppName to name of frontApp
            set winTitle to ""
            try
                tell frontApp
                    set winTitle to name of front window
                end tell
            end try
            return frontAppName & "\n" & winTitle
        end tell
        """
        try:
            proc = subprocess.run(
                ["osascript", "-e", script],
                capture_output=True,
                text=True,
                timeout=1.5
            )
            if proc.returncode == 0 and proc.stdout:
                lines = proc.stdout.strip().split("\n")
                if len(lines) >= 1:
                    process_name = lines[0].strip()
                if len(lines) >= 2:
                    window_title = lines[1].strip()
        except Exception as e:
            self._log(f"macOS window extraction error: {e}")

        # Selected text via pbpaste
        try:
            pb_proc = subprocess.run(
                ["pbpaste"],
                capture_output=True,
                text=True,
                timeout=1.0
            )
            if pb_proc.returncode == 0:
                selected_text = pb_proc.stdout.strip()
        except Exception as e:
            self._log(f"macOS clipboard extraction error: {e}")

        return window_title, process_name, selected_text

    # --------------------------------------------------------------------------
    # Linux Implementation (X11 / Wayland)
    # --------------------------------------------------------------------------
    def _extract_linux(self) -> Tuple[str, str, str]:
        import subprocess

        window_title = ""
        process_name = ""
        selected_text = ""

        # Check for X11 / Wayland
        is_wayland = bool(os.environ.get("WAYLAND_DISPLAY"))

        if not is_wayland:
            # X11 extraction using xdotool / xprop
            try:
                # Active window ID
                xdo_win = subprocess.run(
                    ["xdotool", "getactivewindow"],
                    capture_output=True,
                    text=True,
                    timeout=1.0
                )
                if xdo_win.returncode == 0 and xdo_win.stdout.strip():
                    win_id = xdo_win.stdout.strip()

                    # Window title
                    xdo_name = subprocess.run(
                        ["xdotool", "getwindowname", win_id],
                        capture_output=True,
                        text=True,
                        timeout=1.0
                    )
                    if xdo_name.returncode == 0:
                        window_title = xdo_name.stdout.strip()

                    # Process PID
                    xdo_pid = subprocess.run(
                        ["xdotool", "getwindowpid", win_id],
                        capture_output=True,
                        text=True,
                        timeout=1.0
                    )
                    if xdo_pid.returncode == 0 and xdo_pid.stdout.strip():
                        pid = xdo_pid.stdout.strip()
                        comm_path = f"/proc/{pid}/comm"
                        if os.path.exists(comm_path):
                            with open(comm_path, "r", encoding="utf-8", errors="ignore") as f:
                                process_name = f.read().strip()
            except Exception as e:
                self._log(f"Linux X11 extraction error: {e}")

            # Selected text (primary selection / clipboard)
            try:
                for tool, args in [
                    ("xclip", ["xclip", "-o", "-selection", "primary"]),
                    ("xclip", ["xclip", "-o", "-selection", "clipboard"]),
                    ("xsel", ["xsel", "-o", "-p"]),
                ]:
                    try:
                        clip = subprocess.run(args, capture_output=True, text=True, timeout=0.8)
                        if clip.returncode == 0 and clip.stdout.strip():
                            selected_text = clip.stdout.strip()
                            break
                    except FileNotFoundError:
                        continue
            except Exception as e:
                self._log(f"Linux X11 selection error: {e}")

        else:
            # Wayland extraction
            try:
                # wl-paste for selection
                wl = subprocess.run(["wl-paste", "-p"], capture_output=True, text=True, timeout=0.8)
                if wl.returncode == 0 and wl.stdout.strip():
                    selected_text = wl.stdout.strip()
                else:
                    wl2 = subprocess.run(["wl-paste"], capture_output=True, text=True, timeout=0.8)
                    if wl2.returncode == 0:
                        selected_text = wl2.stdout.strip()
            except Exception as e:
                self._log(f"Linux Wayland selection error: {e}")

        return window_title, process_name, selected_text


class ThreadedHTTPServer(ThreadingMixIn, HTTPServer):
    """Multi-threaded HTTP Server handling concurrent context queries without blocking."""
    daemon_threads = True
    allow_reuse_address = True


def make_request_handler(extractor: DesktopContextExtractor, auth_token: Optional[str] = None, debug: bool = False):
    """Factory creating HTTP Request Handler with bound extractor and authentication config."""

    class CompanionRequestHandler(BaseHTTPRequestHandler):
        def _log(self, message: str) -> None:
            if debug:
                print(f"[DEBUG] [HTTP] {message}", file=sys.stderr)

        def log_message(self, format: str, *args: Any) -> None:
            if debug:
                super().log_message(format, *args)

        def _send_cors_headers(self) -> None:
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Access-Control-Allow-Methods", "GET, OPTIONS")
            self.send_header("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept")

        def _is_authorized(self) -> bool:
            if not auth_token:
                return True

            # 1. Check Authorization: Bearer <token> header
            auth_header = self.headers.get("Authorization", "")
            if auth_header.startswith("Bearer "):
                token_val = auth_header[7:].strip()
                if token_val == auth_token:
                    return True

            # 2. Check query parameter ?token=<token>
            parsed_url = urlparse(self.path)
            query_params = parse_qs(parsed_url.query)
            token_params = query_params.get("token", [])
            if token_params and token_params[0] == auth_token:
                return True

            return False

        def do_OPTIONS(self) -> None:
            self.send_response(204)
            self._send_cors_headers()
            self.end_headers()

        def do_GET(self) -> None:
            parsed_url = urlparse(self.path)
            path = parsed_url.path

            if path == "/health":
                self.send_response(200)
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self._send_cors_headers()
                self.end_headers()
                health_payload = json.dumps({
                    "status": "ok",
                    "version": VERSION
                }).encode("utf-8")
                self.wfile.write(health_payload)
                return

            if not self._is_authorized():
                self._log(f"Unauthorized request to {path}")
                self.send_response(401)
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self._send_cors_headers()
                self.end_headers()
                err_payload = json.dumps({
                    "error": "Unauthorized",
                    "message": "Invalid or missing authorization token"
                }).encode("utf-8")
                self.wfile.write(err_payload)
                return

            if path == "/context":
                context_data = extractor.extract_context()
                self._log(f"Serving context: window='{context_data.get('window_title')}', process='{context_data.get('process_name')}'")
                self.send_response(200)
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self._send_cors_headers()
                self.end_headers()
                context_payload = json.dumps(context_data).encode("utf-8")
                self.wfile.write(context_payload)
                return

            # Default 404 for unknown endpoints
            self.send_response(404)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self._send_cors_headers()
            self.end_headers()
            not_found_payload = json.dumps({
                "error": "Not Found",
                "message": f"Endpoint '{path}' not recognized. Available: /context, /health"
            }).encode("utf-8")
            self.wfile.write(not_found_payload)

    return CompanionRequestHandler


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Type4Me Zero-Install Desktop Context Companion (Zero external pip dependencies)"
    )
    parser.add_argument("--host", default=DEFAULT_HOST, help=f"Host address to bind (default: {DEFAULT_HOST})")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT, help=f"Port to listen on (default: {DEFAULT_PORT})")
    parser.add_argument("--token", default=None, help="Optional authentication bearer token")
    parser.add_argument("--debug", action="store_true", help="Enable verbose debug logging")
    parser.add_argument("--version", action="version", version=f"Type4Me Companion v{VERSION}")

    args = parser.parse_args()

    extractor = DesktopContextExtractor(debug=args.debug)
    handler_class = make_request_handler(extractor, auth_token=args.token, debug=args.debug)

    server_address = (args.host, args.port)
    try:
        httpd = ThreadedHTTPServer(server_address, handler_class)
    except Exception as e:
        print(f"[ERROR] Failed to start HTTP server on {args.host}:{args.port}: {e}", file=sys.stderr)
        sys.exit(1)

    print(f"=================================================================")
    print(f" Type4Me Desktop Context Companion v{VERSION}")
    print(f" Listening on http://{args.host}:{args.port}")
    print(f" Endpoints: http://{args.host}:{args.port}/context")
    print(f"            http://{args.host}:{args.port}/health")
    if args.token:
        print(f" Authentication: REQUIRED (Bearer token configured)")
    else:
        print(f" Authentication: None (Open local subnet access)")
    print(f" Press Ctrl+C to terminate.")
    print(f"=================================================================")

    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n[INFO] Shutting down Type4Me Desktop Companion...")
    finally:
        httpd.server_close()
        print("[INFO] Server terminated cleanly.")


if __name__ == "__main__":
    main()
