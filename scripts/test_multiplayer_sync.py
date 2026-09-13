from copy import deepcopy
import random


def judge(answer, guess):
    used_a = [False] * 4
    used_g = [False] * 4
    hit = blow = 0
    for i in range(4):
        if guess[i] == answer[i]:
            hit += 1
            used_a[i] = used_g[i] = True
    for i in range(4):
        if used_g[i]:
            continue
        for j in range(4):
            if not used_a[j] and guess[i] == answer[j]:
                blow += 1
                used_a[j] = True
                break
    return hit, blow


def patch(room, updates):
    out = deepcopy(room)
    for path, value in updates.items():
        parts = path.split('/')
        cur = out
        for part in parts[:-1]:
            cur = cur.setdefault(part, {})
        cur[parts[-1]] = deepcopy(value)
    return out


def move_from_room(room, index):
    moves = room.get('moves')
    if isinstance(moves, dict):
        return moves.get(f'm{index}') or moves.get(str(index))
    if isinstance(moves, list) and index < len(moves):
        return moves[index]
    return None


class Client:
    def __init__(self, uid, role):
        self.uid = uid
        self.role = role
        self.remote_move_count = 0
        self.tries = []
        self.marks = []
        self.my_turn = False
        self.ended = False

    def apply(self, room):
        status = room.get('status', 'waiting')
        host = room.get('hostId', '')
        guest = room.get('guestId', '')
        turn_uid = room.get('turnUid', host)
        server_count = max(0, min(8, int(room.get('moveIndex', 0))))

        if server_count != self.remote_move_count:
            new_tries, new_marks = [], []
            for i in range(server_count):
                move = move_from_room(room, i)
                if move is None:
                    break
                guess = [int(x) for x in move['guess'].split(',')]
                new_tries.append(guess)
                new_marks.append([int(move['hit']), int(move['blow'])])
            if len(new_tries) == server_count:
                self.tries = new_tries
                self.marks = new_marks
                self.remote_move_count = server_count

        if room.get('winnerUid') or room.get('winner') or status == 'ended':
            self.ended = True
            self.my_turn = False
        else:
            self.my_turn = (
                status == 'playing'
                and bool(guest)
                and self.uid == turn_uid
                and self.remote_move_count == server_count
            )


def submit(room, client, guess):
    assert room['status'] == 'playing'
    assert room['turnUid'] == client.uid
    host = room['hostId']
    guest = room['guestId']
    index = int(room['moveIndex'])
    assert 0 <= index < 8
    assert move_from_room(room, index) is None

    answer = [int(x) for x in room['answer'].split(',')]
    hit, blow = judge(answer, guess)
    move = {
        'playerUid': client.uid,
        'player': client.role,
        'guess': ','.join(map(str, guess)),
        'hit': hit,
        'blow': blow,
    }
    updates = {
        f'moves/m{index}': move,
        'moveIndex': index + 1,
        'lastMoveAt': index + 1,
    }
    if hit == 4:
        updates.update({'winnerUid': client.uid, 'winner': client.role, 'status': 'ended'})
    elif index >= 7:
        updates.update({'winner': 'draw', 'status': 'ended'})
    else:
        updates['turnUid'] = guest if client.uid == host else host
    return patch(room, updates)


def choose_nonwinning_guess(answer, turn):
    guess = [(turn + i) % 6 for i in range(4)]
    if guess == answer:
        guess = guess[::-1]
    if guess == answer:
        guess = [(x + 1) % 6 for x in guess]
    return guess


def run_game(answer):
    room = {
        'hostId': 'HOST',
        'guestId': 'GUEST',
        'status': 'playing',
        'turnUid': 'HOST',
        'moveIndex': 0,
        'answer': ','.join(map(str, answer)),
    }
    host = Client('HOST', 'host')
    guest = Client('GUEST', 'guest')
    host.apply(room)
    guest.apply(room)
    assert host.my_turn and not guest.my_turn

    for turn in range(8):
        active = host if room['turnUid'] == 'HOST' else guest
        room = submit(room, active, choose_nonwinning_guess(answer, turn))
        host.apply(room)
        guest.apply(room)

        assert host.remote_move_count == room['moveIndex']
        assert guest.remote_move_count == room['moveIndex']
        assert host.tries == guest.tries
        assert host.marks == guest.marks
        assert len(host.tries) == turn + 1
        assert set(room.get('moves', {})) == {f'm{i}' for i in range(turn + 1)}

        if room['status'] == 'ended':
            break
        assert host.my_turn != guest.my_turn
        assert (host.my_turn and room['turnUid'] == 'HOST') or (guest.my_turn and room['turnUid'] == 'GUEST')


def main():
    random.seed(4696)
    for _ in range(1000):
        run_game(random.sample(range(6), 4))
    print('PASS: 1000 multiplayer games, shared board and alternating turns stayed synchronized')


if __name__ == '__main__':
    main()
