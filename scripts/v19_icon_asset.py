from pathlib import Path
import base64

root = Path('scripts/icon256_parts')
parts = [(root / f'p{i:02d}.txt').read_text(encoding='utf-8').strip() for i in range(4)]
data = ''.join(parts)
raw = base64.b64decode(data, validate=True)

old = Path('app/src/main/res/drawable/app_icon.xml')
if old.exists():
    old.unlink()

out = Path('app/src/main/res/drawable-nodpi/app_icon.webp')
out.parent.mkdir(parents=True, exist_ok=True)
out.write_bytes(raw)

if out.stat().st_size != 11464:
    raise SystemExit(f'Unexpected icon size: {out.stat().st_size}')

print(f'Installed v19 rendered app icon: {out.stat().st_size} bytes')
