import urllib.request, json, sys
BASE = 'http://127.0.0.1:8090'
def post(p, b, h=None):
    d = json.dumps(b, ensure_ascii=False).encode()
    hd = {'Content-Type':'application/json','Accept':'application/json'}
    if h: hd.update(h)
    r = urllib.request.Request(BASE+p, data=d, method='POST', headers=hd)
    return urllib.request.urlopen(r, timeout=30).read().decode()
def get(p, h=None):
    hd = {'Accept':'application/json'}
    if h: hd.update(h)
    r = urllib.request.Request(BASE+p, method='GET', headers=hd)
    return urllib.request.urlopen(r, timeout=30).read().decode()

PHONE = '13800000003'
post('/api/auth/sms', {'phone': PHONE})
login = json.loads(post('/api/auth/login', {'phone': PHONE, 'code': '123456'}))
token = login['data']['token']
H = {'Authorization': 'Bearer ' + token}

ses = json.loads(post('/api/aichat/send', {'content': '请用一句话介绍你自己', 'title': 'probe'}, H))
sid = ses['data']['sessionId']
reply = ses['data']['reply']
history = json.loads(get(f'/api/aichat/sessions/{sid}/history', H))

# Force UTF-8 stdout
try:
    sys.stdout.reconfigure(encoding='utf-8')
except Exception:
    pass

with open('d:/neusoft_hospital/server/scripts/ai_probe.out.txt', 'w', encoding='utf-8') as f:
    f.write(f'sessionId = {sid}\n')
    f.write(f'reply     = {reply!r}\n')
    f.write(f'turns     = {len(history["data"])}\n')
    for it in history['data']:
        f.write(f'  - {it.get("role")} | {(it.get("content") or "")[:300]}\n')
print('WROTE d:/neusoft_hospital/server/scripts/ai_probe.out.txt')