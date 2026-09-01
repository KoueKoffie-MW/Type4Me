<#
.SYNOPSIS
    Type4Me Zero-Install Desktop Context Companion (PowerShell)
.DESCRIPTION
    A zero-external-dependency PowerShell script for Windows workstations.
    Broadcasts active window title, process name, and selected text over local HTTP
    to enrich Type4Me's Gemini prompt engineering. Runs on Windows PowerShell 5.1
    and PowerShell Core 7+ without administrative rights.
.PARAMETER Port
    The TCP port to listen on. Default is 8765.
.PARAMETER HostAddress
    The hostname/IP prefix to listen on. Default is "localhost".
.PARAMETER Token
    Optional authentication bearer token to require for requests.
.PARAMETER Debug
    Enable verbose diagnostic logging.
.EXAMPLE
    .\type4me_companion.ps1 -Port 8765
.EXAMPLE
    .\type4me_companion.ps1 -Port 8765 -Token "my-secret-token"
#>

[CmdletBinding()]
param(
    [int]$Port = 8765,
    [string]$HostAddress = "localhost",
    [string]$Token = $null,
    [switch]$DebugMode
)

$VERSION = "2.0.0"

function Write-Log {
    param([string]$Message)
    if ($DebugMode) {
        $timestamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss.fff")
        Write-Host "[DEBUG $timestamp] $Message" -ForegroundColor Cyan
    }
}

# ----------------------------------------------------------------------
# 1. Register Win32 P/Invoke APIs (Safe, No Admin Rights Needed)
# ----------------------------------------------------------------------
$win32Signature = @"
using System;
using System.Text;
using System.Runtime.InteropServices;

public class Type4MeWin32 {
    [DllImport("user32.dll")]
    public static extern IntPtr GetForegroundWindow();

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    public static extern int GetWindowTextW(IntPtr hWnd, StringBuilder text, int count);

    [DllImport("user32.dll")]
    public static extern int GetWindowTextLengthW(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

    [DllImport("user32.dll")]
    public static extern bool OpenClipboard(IntPtr hWndNewOwner);

    [DllImport("user32.dll")]
    public static extern bool CloseClipboard();

    [DllImport("user32.dll")]
    public static extern IntPtr GetClipboardData(uint uFormat);

    [DllImport("user32.dll")]
    public static extern bool IsClipboardFormatAvailable(uint format);

    [DllImport("kernel32.dll")]
    public static extern IntPtr GlobalLock(IntPtr hMem);

    [DllImport("kernel32.dll")]
    public static extern bool GlobalUnlock(IntPtr hMem);
}
"@

if (-not ([System.Management.Automation.PSTypeName]'Type4MeWin32').Type) {
    try {
        Add-Type -TypeDefinition $win32Signature -Language CSharp
        Write-Log "Compiled Type4MeWin32 P/Invoke signatures successfully."
    } catch {
        Write-Warning "Failed to compile Win32 P/Invoke types: $_"
    }
}

# ----------------------------------------------------------------------
# 2. Desktop Context Extraction Function
# ----------------------------------------------------------------------
function Get-DesktopContext {
    $windowTitle = ""
    $processName = ""
    $selectedText = ""
    $timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

    try {
        $hwnd = [Type4MeWin32]::GetForegroundWindow()
        if ($hwnd -and ($hwnd -ne [IntPtr]::Zero)) {
            # Window Title
            $len = [Type4MeWin32]::GetWindowTextLengthW($hwnd)
            if ($len -gt 0) {
                $sb = New-Object System.Text.StringBuilder ($len + 1)
                $null = [Type4MeWin32]::GetWindowTextW($hwnd, $sb, $sb.Capacity)
                $windowTitle = $sb.ToString().Trim()
            }

            # Process Name
            $pidOut = [uint32]0
            $null = [Type4MeWin32]::GetWindowThreadProcessId($hwnd, [ref]$pidOut)
            if ($pidOut -gt 0) {
                try {
                    $proc = [System.Diagnostics.Process]::GetProcessById($pidOut)
                    if ($proc) {
                        $processName = $proc.ProcessName
                    }
                } catch {
                    Write-Log "Unable to retrieve ProcessName for PID $($pidOut) - $($_)"
                }
            }
        }
    } catch {
        Write-Log "Error extracting active window: $($_)"
    }

    # Selected text / Clipboard fallback
    try {
        if ([Type4MeWin32]::OpenClipboard([IntPtr]::Zero)) {
            $CF_UNICODETEXT = 13
            if ([Type4MeWin32]::IsClipboardFormatAvailable($CF_UNICODETEXT)) {
                $hMem = [Type4MeWin32]::GetClipboardData($CF_UNICODETEXT)
                if ($hMem -and ($hMem -ne [IntPtr]::Zero)) {
                    $ptr = [Type4MeWin32]::GlobalLock($hMem)
                    if ($ptr -and ($ptr -ne [IntPtr]::Zero)) {
                        try {
                            $selectedText = [System.Runtime.InteropServices.Marshal]::PtrToStringUni($ptr)
                        } finally {
                            $null = [Type4MeWin32]::GlobalUnlock($hMem)
                        }
                    }
                }
            }
            $null = [Type4MeWin32]::CloseClipboard()
        }
    } catch {
        Write-Log "Error extracting clipboard text: $($_)"
    }

    return @{
        window_title  = if ($windowTitle) { $windowTitle } else { "" }
        process_name  = if ($processName) { $processName } else { "" }
        selected_text = if ($selectedText) { $selectedText.Trim() } else { "" }
        timestamp     = $timestamp
    }
}

# ----------------------------------------------------------------------
# 3. HTTP Server Setup using System.Net.HttpListener
# ----------------------------------------------------------------------
$listener = New-Object System.Net.HttpListener

# Determine prefixes: Try localhost and 127.0.0.1 (user-mode, no admin registration required)
$prefixList = @(
    "http://localhost:${Port}/",
    "http://127.0.0.1:${Port}/"
)

if ($HostAddress -ne "localhost" -and $HostAddress -ne "127.0.0.1") {
    $prefixList += "http://${HostAddress}:${Port}/"
}

foreach ($p in $prefixList) {
    try {
        $listener.Prefixes.Add($p)
        Write-Log "Registered prefix: $($p)"
    } catch {
        Write-Log "Could not register prefix $($p) - $($_)"
    }
}

try {
    $listener.Start()
} catch {
    Write-Error "Failed to start HttpListener on port ${Port}. Error: $($_)"
    exit 1
}

Write-Host "=================================================================" -ForegroundColor Green
Write-Host " Type4Me Desktop Context Companion (PowerShell) v$VERSION" -ForegroundColor Green
Write-Host " Listening on: http://${HostAddress}:${Port}" -ForegroundColor Green
Write-Host " Endpoints:    http://localhost:${Port}/context" -ForegroundColor Yellow
Write-Host "               http://localhost:${Port}/health" -ForegroundColor Yellow
if ($Token) {
    Write-Host " Authentication: REQUIRED (Bearer token configured)" -ForegroundColor Magenta
} else {
    Write-Host " Authentication: None (Open local access)" -ForegroundColor DarkGray
}
Write-Host " Press Ctrl+C to terminate." -ForegroundColor DarkGray
Write-Host "=================================================================" -ForegroundColor Green

# ----------------------------------------------------------------------
# 4. Request Handling Loop
# ----------------------------------------------------------------------
function Send-JsonResponse {
    param(
        [System.Net.HttpListenerResponse]$Response,
        [int]$StatusCode,
        [object]$Data
    )

    $json = $Data | ConvertTo-Json -Compress
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)

    $Response.StatusCode = $StatusCode
    $Response.ContentType = "application/json; charset=utf-8"
    $Response.ContentLength64 = $bytes.Length
    $Response.Headers.Add("Access-Control-Allow-Origin", "*")
    $Response.Headers.Add("Access-Control-Allow-Methods", "GET, OPTIONS")
    $Response.Headers.Add("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept")

    $Response.OutputStream.Write($bytes, 0, $bytes.Length)
    $Response.OutputStream.Close()
}

try {
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response

        Write-Log "HTTP $($request.HttpMethod) $($request.Url.AbsolutePath)"

        # Handle CORS preflight OPTIONS
        if ($request.HttpMethod -eq "OPTIONS") {
            $response.StatusCode = 204
            $response.Headers.Add("Access-Control-Allow-Origin", "*")
            $response.Headers.Add("Access-Control-Allow-Methods", "GET, OPTIONS")
            $response.Headers.Add("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept")
            $response.OutputStream.Close()
            continue
        }

        # Route matching
        $path = $request.Url.AbsolutePath.ToLowerInvariant()
        if ($path -eq "/health") {
            Send-JsonResponse -Response $response -StatusCode 200 -Data @{
                status  = "ok"
                version = $VERSION
            }
            continue
        }

        # Token Validation (if token configured)
        if ($Token) {
            $authHeader = $request.Headers["Authorization"]
            $isAuthorized = $false

            if ($authHeader -and $authHeader.StartsWith("Bearer ")) {
                $providedToken = $authHeader.Substring(7).Trim()
                if ($providedToken -eq $Token) {
                    $isAuthorized = $true
                }
            }

            if (-not $isAuthorized -and $request.QueryString["token"]) {
                if ($request.QueryString["token"] -eq $Token) {
                    $isAuthorized = $true
                }
            }

            if (-not $isAuthorized) {
                Write-Log "Unauthorized access attempt"
                Send-JsonResponse -Response $response -StatusCode 401 -Data @{
                    error   = "Unauthorized"
                    message = "Invalid or missing authorization token"
                }
                continue
            }
        }

        if ($path -eq "/context") {
            $data = Get-DesktopContext
            Write-Log "Sending context: window='$($data.window_title)', process='$($data.process_name)'"
            Send-JsonResponse -Response $response -StatusCode 200 -Data $data
        } else {
            Send-JsonResponse -Response $response -StatusCode 404 -Data @{
                error   = "Not Found"
                message = "Endpoint '$path' not recognized. Available: /context, /health"
            }
        }
    }
} finally {
    if ($listener -and $listener.IsListening) {
        $listener.Stop()
        $listener.Close()
    }
    Write-Host "`n[INFO] Type4Me PowerShell Companion stopped." -ForegroundColor Yellow
}
