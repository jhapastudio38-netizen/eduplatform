#!/usr/bin/env python3
"""Set a GitHub Actions secret using the repo's public key."""
import sys
import json
import base64
import urllib.request
from nacl import public

TOKEN = sys.argv[1]
REPO = sys.argv[2]
SECRET_NAME = sys.argv[3]
SECRET_VALUE = sys.argv[4]

# 1. Fetch the repo's public key
req = urllib.request.Request(
    f"https://api.github.com/repos/{REPO}/actions/secrets/public-key",
    headers={"Authorization": f"token {TOKEN}", "Accept": "application/vnd.github+json"},
)
with urllib.request.urlopen(req) as resp:
    pubkey_data = json.loads(resp.read())

pubkey = public.PublicKey(base64.b64decode(pubkey_data["key"]))
sealed = public.SealedBox(pubkey)
encrypted = sealed.encrypt(SECRET_VALUE.encode("utf-8"))
encrypted_b64 = base64.b64encode(encrypted).decode("utf-8")

# 2. PUT the encrypted secret
put_req = urllib.request.Request(
    f"https://api.github.com/repos/{REPO}/actions/secrets/{SECRET_NAME}",
    method="PUT",
    headers={"Authorization": f"token {TOKEN}", "Accept": "application/vnd.github+json", "Content-Type": "application/json"},
    data=json.dumps({
        "encrypted_value": encrypted_b64,
        "key_id": pubkey_data["key_id"],
    }).encode("utf-8"),
)
with urllib.request.urlopen(put_req) as resp:
    print(f"Set {SECRET_NAME}: HTTP {resp.status}")
