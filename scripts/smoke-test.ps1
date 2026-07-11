$ErrorActionPreference = "Stop"
$BASE = "http://127.0.0.1:8090"
$PHONE = "13800000011"
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}
$OutputEncoding = [System.Text.Encoding]::UTF8

function Jp($obj, $path) {
    if ($null -eq $obj) { return $null }
    $parts = $path.Split('.')
    $cur = $obj
    foreach ($p in $parts) {
        if ($null -eq $cur) { return $null }
        if ($cur -is [System.Collections.IEnumerable] -and -not ($cur -is [string])) { return $null }
        if ($cur.PSObject.Properties.Name -contains $p) { $cur = $cur.$p } else { return $null }
    }
    return $cur
}

function Len($obj) {
    if ($null -eq $obj) { return 0 }
    if ($obj -is [System.Collections.IEnumerable] -and -not ($obj -is [string])) { return @($obj).Count }
    return 1
}

function Hit($method, $path, $body, $token) {
    $headers = @{ "Content-Type" = "application/json" }
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $params = @{
        Uri = "$BASE$path"
        Method = $method
        Headers = $headers
        TimeoutSec = 20
    }
    if ($body) { $params.Body = ($body | ConvertTo-Json -Depth 10 -Compress) }
    try {
        $r = Invoke-WebRequest @params -UseBasicParsing
        $resp = $r.Content | ConvertFrom-Json
        return @{ ok = $true; status = $r.StatusCode; body = $resp }
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $raw = $reader.ReadToEnd()
        $obj = $null
        try { $obj = $raw | ConvertFrom-Json } catch {}
        return @{ ok = $false; status = $code; body = $obj; raw = $raw }
    }
}

function ShowCount {
    param($label, $arr)
    Write-Host ("  {0} count={1}" -f $label, (Len $arr))
}

Write-Host "`n=== [1] auth/sms ===" -ForegroundColor Cyan
$r = Hit POST "/api/auth/sms" @{ phone = $PHONE } $null
Write-Host ("  status={0} code={1} msg={2}" -f $r.status, (Jp $r.body 'code'), (Jp $r.body 'message'))
Write-Host ("  traceId={0} ttl={1}" -f (Jp $r.body 'data.traceId'), (Jp $r.body 'data.ttlSeconds'))

Write-Host "`n=== [2] auth/login ===" -ForegroundColor Cyan
$r = Hit POST "/api/auth/login" @{ phone = $PHONE; code = "123456" } $null
Write-Host ("  status={0} code={1} msg={2}" -f $r.status, (Jp $r.body 'code'), (Jp $r.body 'message'))
$access = Jp $r.body 'data.token'
$refresh = Jp $r.body 'data.refreshToken'
$userId = Jp $r.body 'data.userId'
Write-Host ("  userId={0} hasAccess={1} hasRefresh={2}" -f $userId, [bool]$access, [bool]$refresh)

if (-not $access) {
    Write-Host "LOGIN FAILED, body:" -ForegroundColor Red
    Write-Host ($r.body | ConvertTo-Json -Depth 10)
    exit 1
}

Write-Host "`n=== [3] auth/me ===" -ForegroundColor Cyan
$r = Hit GET "/api/auth/me" $null $access
Write-Host ("  status={0} code={1} userId={2}" -f $r.status, (Jp $r.body 'code'), (Jp $r.body 'data.userId'))

Write-Host "`n=== [4] auth/refresh ===" -ForegroundColor Cyan
$r = Hit POST "/api/auth/refresh" @{ refreshToken = $refresh } $null
Write-Host ("  status={0} code={1} newAccess={2} newRefresh={3}" -f $r.status, (Jp $r.body 'code'), [bool](Jp $r.body 'data.token'), [bool](Jp $r.body 'data.refreshToken'))
$access = Jp $r.body 'data.token'
$refresh = Jp $r.body 'data.refreshToken'

Write-Host "`n=== [5] auth/verify-idcard (force real-name so we can book) ===" -ForegroundColor Cyan
$r = Hit POST "/api/auth/verify-idcard" @{ name = "张三"; idCard = "110101199001011234" } $access
Write-Host ("  status={0} code={1} verified={2}" -f $r.status, (Jp $r.body 'code'), (Jp $r.body 'data.verified'))

Write-Host "`n=== [6] departments top-level (no parentId) ===" -ForegroundColor Cyan
$r = Hit GET "/api/departments" $null $null
$deps = Jp $r.body 'data'
ShowCount "top" $deps
$firstTop = $deps | Select-Object -First 1
Write-Host ("  first={0}" -f (Jp $firstTop 'name'))

Write-Host "`n=== [7] departments child of top-level ===" -ForegroundColor Cyan
$topId = Jp $firstTop 'id'
$r = Hit GET "/api/departments?parentId=$topId" $null $null
$subDeps = Jp $r.body 'data'
ShowCount "child" $subDeps
$firstSub = $subDeps | Select-Object -First 1
$subId = Jp $firstSub 'id'
Write-Host ("  firstSub={0} id={1}" -f (Jp $firstSub 'name'), $subId)

Write-Host "`n=== [8] doctors by department ===" -ForegroundColor Cyan
$r = Hit GET "/api/doctors?departmentId=$subId" $null $null
$docs = Jp $r.body 'data'
ShowCount "doctors" $docs
$firstDoc = $docs | Select-Object -First 1
$docId = Jp $firstDoc 'id'
Write-Host ("  firstDoc={0} id={1}" -f (Jp $firstDoc 'name'), $docId)

Write-Host "`n=== [9] doctor detail (7-day schedule) ===" -ForegroundColor Cyan
$r = Hit GET "/api/doctors/$docId" $null $null
$slots = Jp $r.body 'data.schedule'
ShowCount "slotDays" $slots

Write-Host "`n=== [10] book appointment ===" -ForegroundColor Cyan
$date = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")
$timeSlot = "09:00"
if ($slots) {
    $firstDay = $slots | Select-Object -First 1
    $daySlots = Jp $firstDay 'slots'
    if ($daySlots -and (Len $daySlots) -gt 0) {
        $firstSlot = $daySlots | Select-Object -First 1
        if ($firstSlot.id) { $timeSlot = [string]$firstSlot.id }
    }
}
$bookBody = @{
    doctorId = $docId
    departmentId = $subId
    date = $date
    timeSlot = $timeSlot
    duration = 30
}
Write-Host ("  body: doctorId={0} dept={1} date={2} timeSlot={3}" -f $docId, $subId, $date, $timeSlot)
$r = Hit POST "/api/appointments" $bookBody $access
Write-Host ("  status={0} code={1} apptId={2}" -f $r.status, (Jp $r.body 'code'), (Jp $r.body 'data.id'))
$apptId = Jp $r.body 'data.id'

Write-Host "`n=== [11] my appointments ===" -ForegroundColor Cyan
$r = Hit GET "/api/appointments/mine" $null $access
$mine = Jp $r.body 'data'
ShowCount "mine" $mine

Write-Host "`n=== [12] preconsult triage (chest pain) ===" -ForegroundColor Cyan
$r = Hit POST "/api/preconsult/triage" @{ symptoms = @("胸痛","胸闷","气短") } $access
$recs = Jp $r.body 'data.recommendedDepartments'
$diseases = Jp $r.body 'data.possibleDiseases'
Write-Host ("  status={0} recCount={1} diseaseCount={2}" -f $r.status, (Len $recs), (Len $diseases))
if ($recs) {
    $recs | Select-Object -First 3 | ForEach-Object { Write-Host ("    recommend: {0} conf={1}" -f $_.departmentName, $_.confidence) }
}
if ($diseases) {
    $diseases | Select-Object -First 3 | ForEach-Object { Write-Host ("    disease: {0} p={1}" -f $_.name, $_.probability) }
}

Write-Host "`n=== [13] aichat send ===" -ForegroundColor Cyan
$r = Hit POST "/api/aichat/send" @{ message = "我最近胃痛怎么办？" } $access
$sessId = Jp $r.body 'data.sessionId'
$reply = Jp $r.body 'data.reply'
Write-Host ("  status={0} code={1} sessionId={2} replyLen={3}" -f $r.status, (Jp $r.body 'code'), $sessId, $reply.Length)

Write-Host "`n=== [14] aichat sessions ===" -ForegroundColor Cyan
$r = Hit GET "/api/aichat/sessions" $null $access
$sess = Jp $r.body 'data'
ShowCount "sessions" $sess
if ($sess) { Write-Host ("  first={0}" -f (Jp $sess[0] 'title')) }

Write-Host "`n=== [15] aichat history ===" -ForegroundColor Cyan
if ($sessId) {
    $r = Hit GET "/api/aichat/sessions/$sessId/history" $null $access
    $hist = Jp $r.body 'data'
    ShowCount "history" $hist
}

Write-Host "`n=== [16] followup create plan (骨折术后) ===" -ForegroundColor Cyan
$surgeryDate = (Get-Date).AddDays(-7).ToString("yyyy-MM-dd")
$r = Hit POST "/api/followup/plans" @{ disease = "骨折术后"; surgeryDate = $surgeryDate; totalDays = 90 } $access
$planId = Jp $r.body 'data.id'
Write-Host ("  status={0} code={1} planId={2}" -f $r.status, (Jp $r.body 'code'), $planId)

Write-Host "`n=== [17] followup plan tasks ===" -ForegroundColor Cyan
if ($planId) {
    $r = Hit GET "/api/followup/plans/$planId/tasks" $null $access
    $tasks = Jp $r.body 'data'
    ShowCount "tasks" $tasks
    if ($tasks) {
        $tasks | ForEach-Object { Write-Host ("    Day{0} ({1}) completed={2}" -f $_.dayIndex, $_.targetDate, $_.completed) }
    }
}

Write-Host "`n=== [18] followup list plans ===" -ForegroundColor Cyan
$r = Hit GET "/api/followup/plans" $null $access
$plans = Jp $r.body 'data'
ShowCount "plans" $plans

Write-Host "`n=== [19] chronic record (high BP, dangerous) ===" -ForegroundColor Cyan
$r = Hit POST "/api/chronic/records" @{ type = "hypertension"; date = (Get-Date).ToString("yyyy-MM-dd"); systolic = 190; diastolic = 120; heartRate = 95 } $access
Write-Host ("  status={0} code={1} alertLevel={2}" -f $r.status, (Jp $r.body 'code'), (Jp $r.body 'data.alertLevel'))

Write-Host "`n=== [20] chronic alerts ===" -ForegroundColor Cyan
$r = Hit GET "/api/chronic/alerts" $null $access
$alerts = Jp $r.body 'data'
ShowCount "alerts" $alerts
if ($alerts) { Write-Host ("  first level={0} msg={1}" -f (Jp $alerts[0] 'level'), (Jp $alerts[0] 'message')) }

Write-Host "`n=== [21] chronic records list ===" -ForegroundColor Cyan
$r = Hit GET "/api/chronic/records?type=hypertension" $null $access
$recs = Jp $r.body 'data'
ShowCount "chronic" $recs

Write-Host "`n=== [22] logout ===" -ForegroundColor Cyan
$r = Hit POST "/api/auth/logout" @{ refreshToken = $refresh } $access
Write-Host ("  status={0} code={1}" -f $r.status, (Jp $r.body 'code'))

Write-Host "`n=== [23] try refresh after logout (should fail) ===" -ForegroundColor Cyan
$r = Hit POST "/api/auth/refresh" @{ refreshToken = $refresh } $null
Write-Host ("  status={0} code={1} msg={2}" -f $r.status, (Jp $r.body 'code'), (Jp $r.body 'message'))

Write-Host "`n=== DONE ===" -ForegroundColor Green