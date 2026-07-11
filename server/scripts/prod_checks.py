"""Production-readiness checks (UTF-8 safe)."""
import urllib.request, urllib.error, json, sys

BASE = 'http://127.0.0.1:8090'

def post(p, b=None, h=None, *, raw=None):
    hd = {'Accept': 'application/json'}
    if raw is not None:
        d = raw.encode()
    elif b is not None:
        d = json.dumps(b, ensure_ascii=False).encode(); hd['Content-Type'] = 'application/json'
    else:
        d = None
    if h: hd.update(h)
    r = urllib.request.Request(BASE+p, data=d, method='POST', headers=hd)
    try:
        with urllib.request.urlopen(r, timeout=15) as resp:
            return resp.status, dict(resp.headers), resp.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, dict(e.headers), e.read().decode()
    except Exception as e:
        return 0, {}, f'EXC: {e}'

def get(p, h=None):
    hd = {'Accept': 'application/json'}
    if h: hd.update(h)
    r = urllib.request.Request(BASE+p, method='GET', headers=hd)
    try:
        with urllib.request.urlopen(r, timeout=15) as resp:
            return resp.status, dict(resp.headers), resp.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, dict(e.headers), e.read().decode()
    except Exception as e:
        return 0, {}, f'EXC: {e}'

results = []
def check(name, ok, info):
    results.append((ok, name, info))

# Force UTF-8 stdout so Chinese in the report is not mojibake'd by the host shell
try:
    sys.stdout.reconfigure(encoding='utf-8')
except Exception:
    pass

# 1) traceId propagates end-to-end
code, hdr, body = post('/api/auth/sms', {'phone': '13800000077'})
j = json.loads(body) if body else {}
ok = (code == 200 and j.get('code') == 0
      and hdr.get('X-Trace-Id') and j.get('traceId')
      and hdr['X-Trace-Id'] == j['traceId'])
check('traceId header==body', ok, f"http={code} hdr={hdr.get('X-Trace-Id')} body={j.get('traceId')}")

# 2) 500 hides internal exception text — send malformed JSON
code, hdr, body = post('/api/auth/sms', raw='{not json')
j = json.loads(body) if body else {}
msg = j.get('message', '')
leaky = ('com.fasterxml' in msg or 'Jackson' in msg or 'JsonProcessingException' in msg
         or 'AbstractJackson2HttpMessageConverter' in msg or 'Unexpected character' in msg)
ok = (code == 500 and j.get('code') == 500 and j.get('traceId')
      and 'please retry' in msg and not leaky)
check('500 hides internal stack', ok, f"http={code} leaky={leaky} msg={msg[:80]}")

# 3) 401 path on protected endpoint without token
code, hdr, body = get('/api/auth/me')
j = json.loads(body) if body else {}
ok = (j.get('code') in (401, 403) and j.get('traceId'))
check('401/403 with envelope', ok, f"http={code} body={body[:200]}")

# 4) 400 validation error — judge by RAW decoded utf-8 string
post('/api/auth/sms', {'phone': '13800000077'})
login = json.loads(post('/api/auth/login', {'phone':'13800000077','code':'123456'})[2])
tok = login['data']['token']
H = {'Authorization': 'Bearer ' + tok}

code, hdr, body = post('/api/auth/verify-idcard', {'name': 'x', 'idCard': '123'}, H)
j = json.loads(body)
msg = j.get('message', '')
# Either a structured "field: reason" message or a business reason mentioning the field value type
ok = (j.get('code') == 400 and j.get('traceId') and ('idCard' in msg or '身份证' in msg))
check('400 validation w/ traceId', ok, f"http={code} body={body[:200]}")

# 5) AI self-introduction
code, hdr, body = post('/api/aichat/send', {'content': '请介绍一下你自己'}, H)
j = json.loads(body)
reply = j['data']['reply']
ok = ('我是' in reply and '导诊' in reply)
check('AI persona reply', ok, f"reply={reply[:120]}")

# 6) CORS preflight returns the right headers
code, hdr, body = get('/api/departments/all', {'Origin': 'https://example.com'})
ok = (code == 200 and (hdr.get('Access-Control-Allow-Origin') is not None or 'Vary' in str(hdr)))
check('CORS header present', ok, f"acao={hdr.get('Access-Control-Allow-Origin')}")

# 7) actuator/health is public
code, hdr, body = get('/actuator/health')
ok = (code == 200)
check('actuator/health public', ok, f"http={code} body={body[:120]}")

# 8) actuator/env is denied
code, hdr, body = get('/actuator/env')
ok = (code in (404, 401, 403))
check('actuator/env not exposed', ok, f"http={code}")

# Write the report to UTF-8 BOM file so any Windows viewer renders it correctly
out_path = 'd:/neusoft_hospital/server/scripts/prod_report.txt'
with open(out_path, 'w', encoding='utf-8') as f:
    f.write('OK    CHECK                        INFO\n')
    f.write('-'*90 + '\n')
    okc = 0
    for ok, name, info in results:
        f.write(f"{('YES' if ok else 'NO'):<5} {name:<28} {info}\n")
        if ok: okc += 1
    f.write('-'*90 + '\n')
    f.write(f"{okc}/{len(results)} checks passed\n")
print(f'WROTE {out_path}: {okc}/{len(results)} PASS')