import urllib.request, json
def post(p, b, h=None):
    d = json.dumps(b, ensure_ascii=False).encode()
    hd = {'Content-Type': 'application/json', 'Accept': 'application/json'}
    if h: hd.update(h)
    r = urllib.request.Request('http://127.0.0.1:8090' + p, data=d, method='POST', headers=hd)
    try:
        return r, urllib.request.urlopen(r, timeout=15).read().decode()
    except urllib.error.HTTPError as e:
        return r, f"[HTTP {e.code}] " + e.read().decode('utf-8')

post('/api/auth/sms', {'phone': '13800000088'})
L = json.loads(post('/api/auth/login', {'phone': '13800000088', 'code': '123456'})[1])
H = {'Authorization': 'Bearer ' + L['data']['token']}
req, body = post('/api/auth/verify-idcard', {'name': 'x', 'idCard': '123'}, H)
print('body=', body)