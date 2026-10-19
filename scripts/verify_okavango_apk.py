#!/usr/bin/env python3
"""Verify the actual distributed APK, not just Gradle's declared settings."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('apk', type=Path)
parser.add_argument('output', type=Path)
parser.add_argument('--authenticated', action='store_true')
args = parser.parse_args()
tools = Path(os.environ['ANDROID_HOME']) / 'build-tools/36.0.0'
badging = subprocess.check_output([str(tools / 'aapt'), 'dump', 'badging', str(args.apk)], text=True)
package = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging)
assert package, 'Missing package identity'
app_id, code, version = package.groups()
assert app_id == 'com.shihab.diplay.okavango', 'Wrong package ID'
assert "sdkVersion:'22'" in badging, 'Wrong minimum Android API'
assert re.fullmatch(r'\d+\.\d+\.\d+', version), 'Release version must use x.y.z'
assert 'application-debuggable' not in badging, 'Distributed APK must not be debuggable'
assert int(code) > 71, 'Release must upgrade the previous Okavango versionCode 71'
with zipfile.ZipFile(args.apk) as apk:
    names = apk.namelist()
    abis = {name.split('/')[1] for name in names if name.startswith('lib/') and name.endswith('.so')}
    assert abis == {'armeabi-v7a', 'arm64-v8a'}, f'Unexpected native ABIs: {abis}'
    if args.authenticated:
        for name in ('identity.pk8', 'certificate.p7b'):
            assert apk.getinfo('assets/offline-mfi/' + name).file_size > 0, 'Missing authentication asset'
certs = subprocess.check_output([
    str(tools / 'apksigner'), 'verify', '--min-sdk-version', '22', '--verbose', '--print-certs', str(args.apk)
], text=True)
assert 'Verified using v1 scheme (JAR signing): true' in certs, 'Android 5.1 needs V1 signing'
actual = re.search(r'Signer #1 certificate SHA-256 digest: ([a-fA-F0-9]+)', certs).group(1).lower()
expected = Path('signing-cert.sha256').read_text().strip().lower()
assert actual == expected, 'Signing identity changed; APK cannot safely replace this release line'
args.output.mkdir(parents=True, exist_ok=True)
name = f'DiPlay-v{version}-okavango-release.apk'
shutil.copyfile(args.apk, args.output / name)
digest = hashlib.sha256(args.apk.read_bytes()).hexdigest()
(args.output / (name + '.sha256')).write_text(f'{digest}  {name}\n')
(args.output / 'signing-cert.sha256').write_text(expected + '\n')
(args.output / 'release.json').write_text(json.dumps({
    'applicationId': app_id, 'versionCode': int(code), 'versionName': version,
    'minSdk': 22, 'signerSha256': actual, 'authenticated': args.authenticated,
}, indent=2) + '\n')
print(f'Verified {name}: API22, release, V1 signature, fixed signer, ARMv7/ARM64')
