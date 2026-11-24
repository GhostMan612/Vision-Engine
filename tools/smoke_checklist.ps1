#!/usr/bin/env pwsh
# ============================================================
# As Above, So Below. As Within, So Without.
# The Future Dictates the Past and the Past is Always Present.
# ============================================================
# tools/smoke_checklist.ps1 — read the debug-overlay checklist via adb.
# Usage: tools/smoke_checklist.ps1 [serial] [authority]
# Exit: 0 = all items checked-off (no OPEN/FAIL), 1 = some item OPEN/FAIL or adb error.

param([string]$Serial = "", [string]$Authority = "com.visionengine.debugoverlay.checklist")

$adb = "adb"
$args = @("shell", "content", "query", "--uri", "content://$Authority/items")
if ($Serial) { $args = @("-s", $Serial) + $args }
Write-Host "=== adb $($args -join ' ') ==="
$rows = & $adb @args 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "content query failed (exit $LASTEXITCODE)"
    exit 1
}
$rows | Select-Object -First 40

$total = 0
$open = 0
$fail = 0
foreach ($line in $rows) {
    if ($line -match "(?i)status\s*=\s*(\w+)") {
        $total++
        $status = $Matches[1]
        if ($status -eq "OPEN") { $open++ }
        elseif ($status -eq "FAIL") { $fail++ }
    }
}
Write-Host "items=$total open=$open fail=$fail"
if ($open -gt 0 -or $fail -gt 0) { exit 1 }
exit 0
