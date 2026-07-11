[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function MakeUtf8String {
    param([int[]]$codepoints)
    $sb = New-Object System.Text.StringBuilder
    foreach ($cp in $codepoints) {
        [void]$sb.Append([char]::ConvertFromUtf32($cp))
    }
    return $sb.ToString()
}

function MakePayload {
    param([int[][]]$symptoms)
    $items = @()
    foreach ($sym in $symptoms) {
        $s = MakeUtf8String -codepoints $sym
        $items += ('"' + $s + '"')
    }
    $json = '{"symptoms":[' + ($items -join ",") + ']}'
    return [System.Text.Encoding]::UTF8.GetBytes($json)
}

$phone = "139" + (Get-Random -Min 10000000 -Max 99999999)
Invoke-WebRequest -Uri "http://127.0.0.1:8090/api/auth/sms" -Method POST -Headers @{ "Content-Type" = "application/json" } -Body ('{"phone":"' + $phone + '"}') -UseBasicParsing | Out-Null
Start-Sleep -Seconds 1
$tok = (Invoke-WebRequest -Uri "http://127.0.0.1:8090/api/auth/login" -Method POST -Headers @{ "Content-Type" = "application/json" } -Body ('{"phone":"' + $phone + '","code":"123456"}') -UseBasicParsing | ConvertFrom-Json).data.token

$cases = @(
    @{ name = "胸痛 alone"; arr = @( ,@(0x80F8, 0x75DB) ) },
    @{ name = "胸痛 + 胸闷"; arr = @( ,@(0x80F8, 0x75DB), ,@(0x80F8, 0x95FE) ) },
    @{ name = "胸痛 + 胸闷 + 气短"; arr = @( ,@(0x80F8, 0x75DB), ,@(0x80F8, 0x95FE), ,@(0x6C14, 0x77ED) ) },
    @{ name = "胸痛 + 气短"; arr = @( ,@(0x80F8, 0x75DB), ,@(0x6C14, 0x77ED) ) },
    @{ name = "胃痛 alone"; arr = @( ,@(0x80C3, 0x75DB) ) },
    @{ name = "心悸 + 气短"; arr = @( ,@(0x5FC3, 0x60FA), ,@(0x6C14, 0x77ED) ) },
    @{ name = "咳嗽 + 哮喘"; arr = @( ,@(0x54B3, 0x55FD), ,@(0x54EE, 0x5598) ) },
    @{ name = "unknown 头痛"; arr = @( ,@(0x5934, 0x75DB) ) }
)

foreach ($c in $cases) {
    $bytes = MakePayload -symptoms $c.arr
    [System.IO.File]::WriteAllBytes("d:\neusoft_hospital\server\triage-payload.bin", $bytes)
    $text = [System.Text.Encoding]::UTF8.GetString($bytes)
    Write-Host "---- payload: $text ----"
    $r = Invoke-WebRequest -Uri "http://127.0.0.1:8090/api/preconsult/triage" -Method POST -Headers @{ "Content-Type" = "application/json; charset=utf-8"; "Authorization" = "Bearer $tok" } -InFile "d:\neusoft_hospital\server\triage-payload.bin" -UseBasicParsing
    $obj = $r.Content | ConvertFrom-Json
    $rec = ($obj.data.recommendedDepartments | ForEach-Object { "$($_.departmentName):$([math]::Round($_.confidence,2))" }) -join ", "
    $dis = ($obj.data.possibleDiseases | ForEach-Object { "$($_.name):$($_.probability)" }) -join ", "
    Write-Host ("  rec: {0}" -f $rec)
    Write-Host ("  dis: {0}" -f $dis)
    Write-Host ""
}

Remove-Item "d:\neusoft_hospital\server\triage-payload.bin" -ErrorAction SilentlyContinue