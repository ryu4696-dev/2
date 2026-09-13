import json, re, time, random, urllib.parse, urllib.request
from pathlib import Path

SRC = Path('app/src/main/java/dev/ryu4696/hitandblow/MainActivity.java').read_text(encoding='utf-8')
api = re.search(r'API_KEY="([^"]+)"', SRC).group(1)
db = re.search(r'DB="([^"]+)"', SRC).group(1)


def http_json(url, method='GET', body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(url, data=data, method=method, headers={'Content-Type':'application/json'})
    with urllib.request.urlopen(req, timeout=20) as r:
        raw = r.read().decode()
        return json.loads(raw) if raw else None


def signup():
    return http_json(f'https://identitytoolkit.googleapis.com/v1/accounts:signUp?key={api}', 'POST', {'returnSecureToken': True})


def account_delete(token):
    try:
        http_json(f'https://identitytoolkit.googleapis.com/v1/accounts:delete?key={api}', 'POST', {'idToken': token})
    except Exception:
        pass


def room_url(code, token):
    return f'{db}/rooms/{code}.json?auth={urllib.parse.quote(token)}'


def main():
    a = signup(); b = signup()
    ta, tb = a['idToken'], b['idToken']
    ha, gb = a['localId'], b['localId']
    code = None
    try:
        for _ in range(40):
            candidate = f'{random.randrange(10000):04d}'
            if http_json(room_url(candidate, ta)) is None:
                code = candidate; break
        assert code
        answer = '0,1,2,3'
        http_json(room_url(code, ta), 'PUT', {
            'hostId': ha, 'status': 'waiting', 'turnUid': ha,
            'moveIndex': 0, 'answer': answer, 'createdAt': int(time.time()*1000)
        })
        http_json(room_url(code, tb), 'PATCH', {'guestId': gb, 'status': 'playing'})
        room = http_json(room_url(code, ta))
        assert room['turnUid'] == ha and room['moveIndex'] == 0 and room['status'] == 'playing'

        http_json(room_url(code, ta), 'PATCH', {
            'moves/m0': {'playerUid': ha, 'player': 'host', 'guess': '0,2,1,3', 'hit': 2, 'blow': 2},
            'moveIndex': 1, 'turnUid': gb
        })
        room = http_json(room_url(code, tb))
        assert room['moveIndex'] == 1 and room['turnUid'] == gb
        assert room['moves']['m0']['guess'] == '0,2,1,3'

        http_json(room_url(code, tb), 'PATCH', {
            'moves/m1': {'playerUid': gb, 'player': 'guest', 'guess': '4,5,0,1', 'hit': 0, 'blow': 2},
            'moveIndex': 2, 'turnUid': ha
        })
        room = http_json(room_url(code, ta))
        assert room['moveIndex'] == 2 and room['turnUid'] == ha
        assert room['moves']['m0']['playerUid'] == ha
        assert room['moves']['m1']['playerUid'] == gb
        print('PASS: live Firebase room create/join and two-way move synchronization')
    finally:
        if code:
            try: http_json(room_url(code, ta), 'DELETE')
            except Exception: pass
        account_delete(ta); account_delete(tb)


if __name__ == '__main__':
    main()
