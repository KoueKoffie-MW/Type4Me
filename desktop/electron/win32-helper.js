/**
 * Type4Me Desktop Win32 Helper
 * Provides zero-dependency native Windows window tracking and SendInput simulation
 * using pure Windows PowerShell P/Invoke.
 * Implements ADR-0006: Target Window Binding and 1-Click Process Pinning.
 */

const { exec } = require('child_process');
const util = require('util');
const execPromise = util.promisify(exec);

class Win32Helper {
  constructor() {
    this.lastForegroundHwnd = null;
    this.lastWindowTitle = '';
    this.lastProcessName = '';
    this.isPinned = false;
    this.pinnedHwnd = null;
    this.pinnedTitle = '';
    this.pinnedProcess = '';
  }

  /**
   * Toggles target window pin lock
   */
  togglePin() {
    if (this.isPinned) {
      this.isPinned = false;
      this.pinnedHwnd = null;
      this.pinnedTitle = '';
      this.pinnedProcess = '';
    } else if (this.lastForegroundHwnd) {
      this.isPinned = true;
      this.pinnedHwnd = this.lastForegroundHwnd;
      this.pinnedTitle = this.lastWindowTitle;
      this.pinnedProcess = this.lastProcessName;
    }
    return {
      isPinned: this.isPinned,
      hwnd: this.isPinned ? this.pinnedHwnd : this.lastForegroundHwnd,
      title: this.isPinned ? this.pinnedTitle : this.lastWindowTitle,
      process: this.isPinned ? this.pinnedProcess : this.lastProcessName,
    };
  }

  /**
   * Captures the current foreground window handle and metadata.
   * If pinned, preserves the pinned target.
   */
  async captureForegroundWindow() {
    if (this.isPinned && this.pinnedHwnd) {
      return {
        Hwnd: this.pinnedHwnd,
        Title: this.pinnedTitle,
        Process: this.pinnedProcess,
        isPinned: true,
      };
    }

    const psScript = `
      $sig = @'
        [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
        [DllImport("user32.dll")] public static extern int GetWindowText(IntPtr hWnd, System.Text.StringBuilder text, int count);
        [DllImport("user32.dll", SetLastError=true)] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);
'@
      $type = Add-Type -MemberDefinition $sig -Name "Win32Util" -Namespace "Type4Me" -PassThru -ErrorAction SilentlyContinue
      $hwnd = [Type4Me.Win32Util]::GetForegroundWindow()
      $sb = New-Object System.Text.StringBuilder 256
      [void][Type4Me.Win32Util]::GetWindowText($hwnd, $sb, 256)
      $pid = 0
      [void][Type4Me.Win32Util]::GetWindowThreadProcessId($hwnd, [ref]$pid)
      $pname = ""
      if ($pid -gt 0) { $pname = (Get-Process -Id $pid -ErrorAction SilentlyContinue).ProcessName }
      [PSCustomObject]@{ Hwnd = $hwnd.ToInt64(); Title = $sb.ToString(); Process = $pname } | ConvertTo-Json -Compress
    `;

    try {
      const { stdout } = await execPromise(`powershell.exe -NoProfile -NonInteractive -Command "${psScript.replace(/\r?\n/g, ' ')}"`);
      if (stdout && stdout.trim()) {
        const data = JSON.parse(stdout.trim());
        this.lastForegroundHwnd = data.Hwnd;
        this.lastWindowTitle = data.Title || 'Unknown';
        this.lastProcessName = data.Process || 'Unknown';
        return { ...data, isPinned: this.isPinned };
      }
    } catch (err) {
      console.warn('Could not capture foreground window via P/Invoke:', err.message);
    }
    return {
      Hwnd: this.lastForegroundHwnd,
      Title: this.lastWindowTitle,
      Process: this.lastProcessName,
      isPinned: this.isPinned,
    };
  }

  /**
   * Restores focus to the recorded (or pinned) window and simulates Ctrl+V paste
   */
  async pasteToPreviousWindow(text) {
    const targetHwnd = this.isPinned && this.pinnedHwnd ? this.pinnedHwnd : this.lastForegroundHwnd;
    if (!targetHwnd) return false;

    const psScript = `
      $sig = @'
        [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
        [DllImport("user32.dll")] public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, int dwExtraInfo);
'@
      Add-Type -MemberDefinition $sig -Name "Win32Paste" -Namespace "Type4Me" -ErrorAction SilentlyContinue
      $hwnd = [IntPtr]${targetHwnd}
      [void][Type4Me.Win32Paste]::SetForegroundWindow($hwnd)
      Start-Sleep -Milliseconds 120
      # VK_CONTROL = 0x11, VK_V = 0x56, KEYEVENTF_KEYUP = 0x0002
      [Type4Me.Win32Paste]::keybd_event(0x11, 0, 0, 0)
      [Type4Me.Win32Paste]::keybd_event(0x56, 0, 0, 0)
      Start-Sleep -Milliseconds 20
      [Type4Me.Win32Paste]::keybd_event(0x56, 0, 2, 0)
      [Type4Me.Win32Paste]::keybd_event(0x11, 0, 2, 0)
    `;

    try {
      await execPromise(`powershell.exe -NoProfile -NonInteractive -Command "${psScript.replace(/\r?\n/g, ' ')}"`);
      return true;
    } catch (err) {
      console.error('Failed to paste to previous window:', err.message);
      return false;
    }
  }
}

module.exports = new Win32Helper();
