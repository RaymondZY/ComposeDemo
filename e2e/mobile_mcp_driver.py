"""
Mobile MCP Driver for ComposeDemo E2E Testing.
Wraps @mobilenext/mobile-mcp JSON-RPC stdio protocol.
"""

import json
import os
import re
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple


@dataclass
class UiElement:
    type: str
    text: str
    label: str
    identifier: str
    x: int
    y: int
    width: int
    height: int

    @property
    def center(self) -> Tuple[int, int]:
        return (self.x + self.width // 2, self.y + self.height // 2)

    def __repr__(self) -> str:
        return (
            f"UiElement({self.type!r}, text={self.text!r}, label={self.label!r}, "
            f"id={self.identifier!r}, rect=({self.x},{self.y},{self.width},{self.height}))"
        )


class MobileMcpError(Exception):
    pass


class ElementNotFoundError(MobileMcpError):
    pass


class MobileMcpDriver:
    """Manages mobile-mcp subprocess and provides high-level device automation API."""

    def __init__(self, device: str = "emulator-5554", report_dir: Optional[str] = None):
        self.device = device
        self.proc: Optional[subprocess.Popen] = None
        self.request_id = 0
        self._buffer = ""

        # Report setup
        if report_dir is None:
            ts = datetime.now().strftime("%Y%m%d_%H%M%S")
            report_dir = Path(__file__).parent / "reports" / ts
        self.report_dir = Path(report_dir)
        self.report_dir.mkdir(parents=True, exist_ok=True)
        self.step_counter = 0
        self.log_entries: List[Dict[str, Any]] = []

    # ------------------------------------------------------------------
    # Lifecycle
    # ------------------------------------------------------------------

    def start(self) -> None:
        """Launch mobile-mcp stdio server."""
        cmd = ["npx", "-y", "@mobilenext/mobile-mcp@latest"]
        self.proc = subprocess.Popen(
            cmd,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            bufsize=1,
        )
        # Initialize MCP handshake
        self._send({
            "jsonrpc": "2.0",
            "id": self._next_id(),
            "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "composedemo-e2e", "version": "1.0.0"},
            },
        })
        resp = self._read_response()
        if resp is None or "result" not in resp:
            raise MobileMcpError(f"MCP initialize failed: {resp}")
        # Send initialized notification
        self._send({
            "jsonrpc": "2.0",
            "method": "notifications/initialized",
        })
        print(f"[MCP] Connected: {resp['result']['serverInfo']}")

    def stop(self) -> None:
        if self.proc:
            self.proc.terminate()
            try:
                self.proc.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self.proc.kill()
            self.proc = None

    def __enter__(self):
        self.start()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.stop()
        return False

    # ------------------------------------------------------------------
    # Low-level JSON-RPC
    # ------------------------------------------------------------------

    def _next_id(self) -> int:
        self.request_id += 1
        return self.request_id

    def _send(self, msg: dict) -> None:
        raw = json.dumps(msg, ensure_ascii=False) + "\n"
        if self.proc and self.proc.stdin:
            self.proc.stdin.write(raw)
            self.proc.stdin.flush()

    def _read_response(self, timeout: float = 30.0) -> Optional[dict]:
        """Read next JSON-RPC response line from stdout."""
        start = time.time()
        while time.time() - start < timeout:
            if self.proc and self.proc.poll() is not None:
                stderr = self.proc.stderr.read() if self.proc.stderr else ""
                raise MobileMcpError(f"mobile-mcp exited unexpectedly. stderr: {stderr[:500]}")
            line = self.proc.stdout.readline() if self.proc else ""
            if not line:
                time.sleep(0.05)
                continue
            line = line.strip()
            if not line:
                continue
            try:
                return json.loads(line)
            except json.JSONDecodeError:
                # Some tools output plain text; try to wrap or skip
                if line.startswith("Screenshot saved") or line.startswith("Clicked"):
                    return {"_text": line}
                continue
        raise MobileMcpError(f"Timeout waiting for response after {timeout}s")

    def _call_tool(self, name: str, arguments: dict, timeout: float = 30.0) -> dict:
        req_id = self._next_id()
        self._send({
            "jsonrpc": "2.0",
            "id": req_id,
            "method": "tools/call",
            "params": {"name": name, "arguments": arguments},
        })
        # Some tools emit multiple lines (notifications). We need the response with matching id.
        start = time.time()
        while time.time() - start < timeout:
            resp = self._read_response(timeout=timeout)
            if resp is None:
                continue
            if resp.get("id") == req_id:
                if "error" in resp:
                    raise MobileMcpError(f"Tool {name} error: {resp['error']}")
                return resp.get("result", {})
        raise MobileMcpError(f"Timeout waiting for tool {name} response")

    # ------------------------------------------------------------------
    # High-level API
    # ------------------------------------------------------------------

    def list_elements(self) -> List[UiElement]:
        """Get view hierarchy via mobile_list_elements_on_screen."""
        result = self._call_tool("mobile_list_elements_on_screen", {"device": self.device})
        content = result.get("content", [{}])[0].get("text", "")
        prefix = "Found these elements on screen: "
        if content.startswith(prefix):
            content = content[len(prefix):]
        try:
            raw_list = json.loads(content)
        except json.JSONDecodeError as e:
            raise MobileMcpError(f"Failed to parse view tree: {e}\nRaw: {content[:500]}")

        elements = []
        for item in raw_list:
            c = item.get("coordinates", {})
            elements.append(UiElement(
                type=item.get("type", ""),
                text=item.get("text", ""),
                label=item.get("label", ""),
                identifier=item.get("identifier", ""),
                x=c.get("x", 0),
                y=c.get("y", 0),
                width=c.get("width", 0),
                height=c.get("height", 0),
            ))
        return elements

    def find_element(
        self,
        text: Optional[str] = None,
        label: Optional[str] = None,
        identifier: Optional[str] = None,
        type_: Optional[str] = None,
        partial_text: bool = False,
    ) -> UiElement:
        """Find first matching element in view hierarchy."""
        elements = self.list_elements()
        for el in elements:
            match = True
            if text is not None:
                if partial_text:
                    if text not in el.text:
                        match = False
                else:
                    if el.text != text:
                        match = False
            if label is not None:
                if partial_text:
                    if label not in el.label:
                        match = False
                else:
                    if el.label != label:
                        match = False
            if identifier is not None:
                if identifier not in el.identifier:
                    match = False
            if type_ is not None:
                if el.type != type_:
                    match = False
            if match:
                return el
        raise ElementNotFoundError(
            f"Element not found: text={text!r}, label={label!r}, id={identifier!r}, type={type_!r}"
        )

    def find_elements(
        self,
        text: Optional[str] = None,
        label: Optional[str] = None,
        identifier: Optional[str] = None,
        type_: Optional[str] = None,
        partial_text: bool = False,
    ) -> List[UiElement]:
        """Find all matching elements."""
        elements = self.list_elements()
        matches = []
        for el in elements:
            match = True
            if text is not None:
                if partial_text:
                    if text not in el.text:
                        match = False
                else:
                    if el.text != text:
                        match = False
            if label is not None:
                if partial_text:
                    if label not in el.label:
                        match = False
                else:
                    if el.label != label:
                        match = False
            if identifier is not None:
                if identifier not in el.identifier:
                    match = False
            if type_ is not None:
                if el.type != type_:
                    match = False
            if match:
                matches.append(el)
        return matches

    def click(self, x: int, y: int) -> None:
        """Click at screen coordinates."""
        self._call_tool("mobile_click_on_screen_at_coordinates", {
            "device": self.device,
            "x": x,
            "y": y,
        })

    def click_element(self, el: UiElement) -> None:
        """Click center of an element."""
        cx, cy = el.center
        self.click(cx, cy)

    def type_text(self, text: str) -> None:
        """Type text into focused input field."""
        self._call_tool("mobile_type_keys", {
            "device": self.device,
            "text": text,
            "submit": False,
        })

    def type_text_real(self, text: str) -> None:
        """
        Type text via adb input text + ENTER keyevent.
        This properly triggers Compose TextField onValueChange.
        Requires the input field to already have focus.
        NOTE: Only supports ASCII/English text (adb input text limitation).
        """
        # adb input text to inject characters
        subprocess.run(
            ["adb", "-s", self.device, "shell", "input", "text", text],
            check=True, capture_output=True, text=True
        )
        time.sleep(0.3)
        # KEYCODE_ENTER (66) to commit the text and trigger onValueChange
        subprocess.run(
            ["adb", "-s", self.device, "shell", "input", "keyevent", "66"],
            check=True, capture_output=True, text=True
        )
        time.sleep(0.5)

    def dismiss_keyboard(self) -> None:
        """Dismiss soft keyboard by tapping the down-arrow button at bottom-left."""
        # Coordinates estimated from Pixel 8 Pro API 35 emulator keyboard layout
        self.click(80, 2850)
        time.sleep(0.8)

    def press_button(self, button: str) -> None:
        """Press a physical button: BACK, HOME, VOLUME_UP, VOLUME_DOWN, ENTER."""
        self._call_tool("mobile_press_button", {
            "device": self.device,
            "button": button,
        })

    def swipe(self, direction: str, x: Optional[int] = None, y: Optional[int] = None, distance: Optional[int] = None) -> None:
        """Swipe in direction: up, down, left, right."""
        params = {"device": self.device, "direction": direction}
        if x is not None:
            params["x"] = x
        if y is not None:
            params["y"] = y
        if distance is not None:
            params["distance"] = distance
        self._call_tool("mobile_swipe_on_screen", params)

    def screenshot(self, filename: Optional[str] = None) -> str:
        """Take screenshot and save to report dir. Returns saved path."""
        if filename is None:
            self.step_counter += 1
            filename = f"step_{self.step_counter:03d}.png"
        path = self.report_dir / filename
        self._call_tool("mobile_save_screenshot", {
            "device": self.device,
            "saveTo": str(path),
        })
        return str(path)

    def save_viewtree(self, filename: Optional[str] = None) -> str:
        """Save current view tree as JSON."""
        if filename is None:
            self.step_counter += 1
            filename = f"viewtree_{self.step_counter:03d}.json"
        path = self.report_dir / filename
        elements = self.list_elements()
        data = [
            {
                "type": e.type,
                "text": e.text,
                "label": e.label,
                "identifier": e.identifier,
                "coordinates": {"x": e.x, "y": e.y, "width": e.width, "height": e.height},
            }
            for e in elements
        ]
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        return str(path)

    def launch_app(self, package: str, activity: Optional[str] = None) -> None:
        """Launch app via adb am start."""
        cmd = f"adb -s {self.device} shell am start -n {package}"
        if activity:
            cmd += f"/{activity}"
        else:
            cmd += "/.MainActivity"
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        if result.returncode != 0 and "Warning" not in result.stderr:
            pass
        time.sleep(1.5)  # Wait for app to render

    def terminate_app(self, package: str) -> None:
        """Force-stop app."""
        subprocess.run(
            f"adb -s {self.device} shell am force-stop {package}",
            shell=True, capture_output=True, text=True
        )
        time.sleep(0.5)

    def clear_app_data(self, package: str) -> None:
        """Clear app data."""
        subprocess.run(
            f"adb -s {self.device} shell pm clear {package}",
            shell=True, capture_output=True, text=True
        )
        time.sleep(0.5)

    def go_home(self) -> None:
        """Press HOME button."""
        self.press_button("HOME")
        time.sleep(0.5)

    # ------------------------------------------------------------------
    # Assertions / helpers
    # ------------------------------------------------------------------

    def assert_element_exists(
        self,
        text: Optional[str] = None,
        label: Optional[str] = None,
        identifier: Optional[str] = None,
        type_: Optional[str] = None,
        partial_text: bool = False,
        timeout: float = 5.0,
    ) -> UiElement:
        """Poll view tree until element is found or timeout."""
        start = time.time()
        last_err = None
        while time.time() - start < timeout:
            try:
                return self.find_element(text, label, identifier, type_, partial_text)
            except ElementNotFoundError as e:
                last_err = e
                time.sleep(0.3)
        raise ElementNotFoundError(f"assert_element_exists timeout ({timeout}s): {last_err}")

    def assert_element_not_exists(
        self,
        text: Optional[str] = None,
        label: Optional[str] = None,
        identifier: Optional[str] = None,
        type_: Optional[str] = None,
        partial_text: bool = False,
        timeout: float = 3.0,
    ) -> None:
        """Poll view tree to ensure element disappears."""
        start = time.time()
        while time.time() - start < timeout:
            try:
                self.find_element(text, label, identifier, type_, partial_text)
                time.sleep(0.3)
            except ElementNotFoundError:
                return
        raise MobileMcpError(
            f"assert_element_not_exists failed: element still present after {timeout}s "
            f"(text={text!r}, label={label!r})"
        )

    def assert_text_in_viewtree(self, expected: str, timeout: float = 5.0) -> None:
        """Assert expected text appears somewhere in view tree."""
        start = time.time()
        while time.time() - start < timeout:
            elements = self.list_elements()
            for el in elements:
                if expected in el.text or expected in el.label:
                    return
            time.sleep(0.3)
        raise ElementNotFoundError(f"assert_text_in_viewtree timeout: text={expected!r}")

    def assert_text_not_in_viewtree(self, expected: str, timeout: float = 3.0) -> None:
        """Assert text is NOT in view tree."""
        start = time.time()
        while time.time() - start < timeout:
            elements = self.list_elements()
            found = any(expected in el.text or expected in el.label for el in elements)
            if not found:
                return
            time.sleep(0.3)
        raise MobileMcpError(f"assert_text_not_in_viewtree failed: text={expected!r} still present")

    # ------------------------------------------------------------------
    # Logging & Reporting
    # ------------------------------------------------------------------

    def log_step(self, step_num: int, action: str, result: str = "", screenshot_path: str = "", viewtree_path: str = "") -> None:
        entry = {
            "step": step_num,
            "action": action,
            "result": result,
            "screenshot": screenshot_path,
            "viewtree": viewtree_path,
            "timestamp": datetime.now().isoformat(),
        }
        self.log_entries.append(entry)
        status = "✅" if not result.startswith("FAIL") else "❌"
        print(f"  {status} Step {step_num}: {action}")

    def generate_html_report(self, title: str = "E2E Test Report") -> str:
        """Generate HTML report with screenshots."""
        report_path = self.report_dir / "report.html"
        html = f"""<!DOCTYPE html>
<html><head><meta charset="utf-8"><title>{title}</title>
<style>
  body {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; margin: 2rem; background: #f5f5f5; }}
  h1 {{ color: #333; }}
  .meta {{ color: #666; margin-bottom: 1.5rem; }}
  table {{ width: 100%; border-collapse: collapse; background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }}
  th, td {{ padding: 12px; text-align: left; border-bottom: 1px solid #eee; }}
  th {{ background: #4CAF50; color: white; }}
  tr:hover {{ background: #f9f9f9; }}
  .pass {{ color: #4CAF50; font-weight: bold; }}
  .fail {{ color: #f44336; font-weight: bold; }}
  .ss {{ max-width: 300px; max-height: 500px; border: 1px solid #ddd; border-radius: 4px; cursor: pointer; }}
  .step-num {{ width: 60px; }}
</style></head><body>
<h1>{title}</h1>
<div class="meta">Report dir: {self.report_dir}</div>
<table>
<tr><th class="step-num">Step</th><th>Action</th><th>Result</th><th>Screenshot</th></tr>
"""
        for entry in self.log_entries:
            ss = entry.get("screenshot", "")
            ss_html = f'<img class="ss" src="{Path(ss).name}" onclick="window.open(this.src)">' if ss else ""
            res = entry.get("result", "")
            res_cls = "pass" if not res.startswith("FAIL") else "fail"
            html += f"""
<tr>
  <td class="step-num">{entry['step']}</td>
  <td>{entry['action']}</td>
  <td class="{res_cls}">{res}</td>
  <td>{ss_html}</td>
</tr>"""
        html += "</table></body></html>"
        with open(report_path, "w", encoding="utf-8") as f:
            f.write(html)
        return str(report_path)

    def copy_to_baseline(self, src_path: str, baseline_name: str) -> str:
        """Copy a screenshot to baseline dir."""
        baseline_dir = Path(__file__).parent / "baseline"
        baseline_dir.mkdir(exist_ok=True)
        dst = baseline_dir / baseline_name
        shutil.copy2(src_path, dst)
        return str(dst)
