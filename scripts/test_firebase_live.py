import json, re, time, random, urllib.parse, urllib.request
from pathlib import Path

SRC = Path('app/src/main/java/dev/ryu4696/hitandblow/MainActivity.java').read_text(encoding='utf-8')
api = re.search(r'API_KEY="([^"]+)"', SRC).group(1)
db = re.search(r'DB="([^"]+)"', SRC).group(1)


def http_json(url, method='GET', body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(url, data=data, method=method, headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(req, timeout=20) as r:
        raw = r.read().decode()
        return json.loads(raw) if raw else None


def signup():
    return http_json(
        f'https://identitytoolkit.googleapis.com/v1/accounts:signUp?key={api}',
        'POST', {'returnSecureToken': True}
    )


def account_delete(token):
    try:
        http_json(
            f'https://identitytoolkit.googleapis.com/v1/accounts:delete?key={api}',
            'POST', {'idToken': token}
        )
    except Exception:
        pass


def room_url(code, token):
    return f'{db}/rooms/{code}.json?auth={urllib.parse.quote(token)}'


def judge(answer, guess):
    hit = sum(a == g for a, g in zip(answer, guess))
    ca = [0] * 6
    cg = [0] * 6
    for a, g in zip(answer, guess):
        ca[a] += 1
        cg[g] += 1
    total = sum(min(ca[i], cg[i]) for i in range(6))
    return hit, total - hit


def create_room(host_token, host_uid):
    code = None
    for _ in range(40):
        candidate = f'{random.randrange(10000):04d}'
        if http_json(room_url(candidate, host_token)) is None:
            code = candidate
            break
    assert code and len(code) == 4 and code.isdigit()

    answer = [0, 1, 2, 3]
    http_json(room_url(code, host_token), 'PUT', {
        'hostId': host_uid,
        'status': 'waiting',
        'turnUid': host_uid,
        'moveIndex': 0,
        'answer': ','.join(map(str, answer)),
        'createdAt': int(time.time() * 1000),
    })
    return code, answer


def join_like_app(code, token, uid):
    room = http_json(room_url(code, token))
    if room is None:
        return 'missing'
    host_id = room.get('hostId', '')
    if not host_id:
        return 'broken'
    guest_id = room.get('guestId', '')
    if guest_id and guest_id != uid:
        return 'full'
    if room.get('status') == 'ended':
        return 'ended'

    patch = {'guestId': uid, 'status': 'playing'}
    if not room.get('turnUid'):
        patch['turnUid'] = host_id
    if 'moveIndex' not in room:
        patch['moveIndex'] = 0
    http_json(room_url(code, token), 'PATCH', patch)
    return 'joined'


def send_move_like_app(code, token, uid, role, guess):
    room = http_json(room_url(code, token))
    assert room is not None
    assert room.get('status') == 'playing'
    host_id = room['hostId']
    guest_id = room['guestId']
    assert room.get('turnUid', host_id) == uid

    index = int(room.get('moveIndex', 0))
    assert 0 <= index < 8
    moves = room.get('moves') or {}
    assert f'm{index}' not in moves

    answer = list(map(int, room['answer'].split(',')))
    hit, blow = judge(answer, guess)
    move = {
        'playerUid': uid,
        'player': role,
        'guess': ','.join(map(str, guess)),
        'hit': hit,
        'blow': blow,
        'at': int(time.time() * 1000),
    }
    patch = {
        f'moves/m{index}': move,
        'moveIndex': index + 1,
        'lastMoveAt': int(time.time() * 1000),
    }
    if hit == 4:
        patch.update({'winnerUid': uid, 'winner': role, 'status': 'ended'})
    elif index >= 7:
        patch.update({'winner': 'draw', 'status': 'ended'})
    else:
        patch['turnUid'] = guest_id if uid == host_id else host_id
    http_json(room_url(code, token), 'PATCH', patch)
    return index, hit, blow


def assert_shared_view(code, ta, tb, expected_count, expected_turn=None):
    ra = http_json(room_url(code, ta))
    rb = http_json(room_url(code, tb))
    assert ra == rb, 'host and guest fetched different room state'
    assert ra['moveIndex'] == expected_count
    moves = ra.get('moves') or {}
    assert len(moves) == expected_count
    for i in range(expected_count):
        assert f'm{i}' in moves
    if expected_turn is not None:
        assert ra['turnUid'] == expected_turn
    return ra


def main():
    host = signup()
    guest = signup()
    third = signup()
    ta, tb, tc = host['idToken'], guest['idToken'], third['idToken']
    ha, gb, th = host['localId'], guest['localId'], third['localId']
    code = None

    try:
        # 1. Host creates a four-digit room exactly like the app.
        code, answer = create_room(ta, ha)
        room = http_json(room_url(code, ta))
        assert room['status'] == 'waiting'
        assert room['turnUid'] == ha
        assert room['moveIndex'] == 0
        assert 'guestId' not in room
        print(f'PASS 1: host created room {code}; waiting with host as first turn')

        # 2. A wrong room number is not joinable.
        wrong = '9999' if code != '9999' else '9998'
        # Avoid colliding with a real existing room.
        if http_json(room_url(wrong, tb)) is None:
            assert join_like_app(wrong, tb, gb) == 'missing'
            print('PASS 2: nonexistent room number is rejected')
        else:
            print('SKIP 2: chosen wrong-code probe happened to exist')

        # 3. Guest joins using only the room number. There is no separate password.
        assert join_like_app(code, tb, gb) == 'joined'
        room = assert_shared_view(code, ta, tb, 0, ha)
        assert room['status'] == 'playing'
        assert room['guestId'] == gb
        print('PASS 3: second anonymous user joined by four-digit room number')

        # 4. A third user sees the room as full under the same app-side rules.
        assert join_like_app(code, tc, th) == 'full'
        print('PASS 4: third user is rejected as room full')

        # 5. Host starts. Run all eight turns and verify both clients see identical state each time.
        guesses = [
            [0, 2, 1, 3],
            [4, 5, 0, 1],
            [0, 1, 3, 2],
            [5, 4, 3, 2],
            [1, 0, 2, 4],
            [2, 3, 4, 5],
            [3, 2, 5, 4],
            [5, 3, 1, 0],
        ]
        players = [
            (ta, ha, 'host'),
            (tb, gb, 'guest'),
        ]

        for i, guess in enumerate(guesses):
            token, uid, role = players[i % 2]
            idx, hit, blow = send_move_like_app(code, token, uid, role, guess)
            assert idx == i
            expected_turn = None if i == 7 else players[(i + 1) % 2][1]
            room = assert_shared_view(code, ta, tb, i + 1, expected_turn)
            m = room['moves'][f'm{i}']
            assert m['playerUid'] == uid
            assert m['guess'] == ','.join(map(str, guess))
            assert m['hit'] == hit and m['blow'] == blow
            if i < 7:
                assert room['status'] == 'playing'
            print(f'PASS {5+i}: move {i+1} synchronized to both clients; next turn correct')

        room = assert_shared_view(code, ta, tb, 8)
        assert room['status'] == 'ended'
        assert room['winner'] == 'draw'
        assert len(room['moves']) == 8
        print('PASS 13: all eight shared-board turns completed with no overwrite or reset')

        # 6. Host cleanup behaves like leaving the online room.
        http_json(room_url(code, ta), 'DELETE')
        assert http_json(room_url(code, ta)) is None
        assert http_json(room_url(code, tb)) is None
        print('PASS 14: room deletion is visible to both clients')
        code = None

        print('PASS: live Firebase end-to-end multiplayer flow succeeded')

    finally:
        if code:
            try:
                http_json(room_url(code, ta), 'DELETE')
            except Exception:
                pass
        account_delete(ta)
        account_delete(tb)
        account_delete(tc)


if __name__ == '__main__':
    main()
