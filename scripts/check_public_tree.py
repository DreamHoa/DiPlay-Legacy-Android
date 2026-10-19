#!/usr/bin/env python3
"""Fail CI if credential containers or private-key blocks enter the source tree."""
from pathlib import Path
import hashlib
import re
import subprocess

root = Path(__file__).resolve().parents[1]
blocked_suffixes = {'.pk8', '.p7b', '.pem', '.key', '.p12', '.pfx', '.jks', '.keystore', '.apk', '.aab'}
private_block = re.compile(rb'-----BEGIN (?:[A-Z0-9 ]+ )?PRIVATE KEY-----\s+[A-Za-z0-9+/=\r\n]{40,}')
# This upstream file contains public CA certificates, not an accessory identity.
# Pin its complete content; a different PEM or a changed bundle still fails review.
public_certificate_bundles = {
    'common/src/main/res/raw/update_ca_bundle.pem':
        '3e68a959493625de324485c7e0314e68688af96b6efdfd80fb7f99e804493459',
}


def forbidden(name, content):
    path = Path(name)
    if '.private' in path.parts or private_block.search(content):
        return True
    if path.suffix.lower() not in blocked_suffixes:
        return False
    expected = public_certificate_bundles.get(name)
    return expected is None or hashlib.sha256(content.replace(b'\r\n', b'\n')).hexdigest() != expected


def main():
    names = subprocess.check_output(['git', 'ls-files', '-z'], cwd=root).decode().split('\0')
    failures = [name for name in filter(None, names)
                if (root / name).is_file() and forbidden(name, (root / name).read_bytes())]
    if failures:
        raise SystemExit('Credential or distribution files are forbidden in the public tree:\n' + '\n'.join(failures))
    print('Public tree check passed: no credential containers or private-key blocks.')


if __name__ == '__main__':
    main()
