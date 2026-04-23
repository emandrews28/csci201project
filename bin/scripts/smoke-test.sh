#!/usr/bin/env bash
# End-to-end HTTP test of the photo-uploads feature. Assumes:
#   - ./scripts/setup-db.sh has been run
#   - ./scripts/run-server.sh is running in another terminal
#
# Exits non-zero on any failure.

set -euo pipefail

BASE="${BASE:-http://localhost:8080/CSCI201_Project}"
COOKIES="$(mktemp)"
TMPDIR_SMOKE="$(mktemp -d)"
trap 'rm -rf "$COOKIES" "$TMPDIR_SMOKE"' EXIT

fail() { echo "FAIL: $1" >&2; exit 1; }

echo "== Check server is up"
if ! curl -fsS -o /dev/null "$BASE/login.html"; then
    fail "Server not reachable at $BASE — start it with ./scripts/run-server.sh"
fi

USERNAME="smoke_$RANDOM$RANDOM"
EMAIL="${USERNAME}@example.com"
PASSWORD="smokepass123"

echo "== Register user ($USERNAME)"
# RegisterServlet redirects to index.html on success (302); -f would fail on 3xx.
REG_STATUS=$(curl -sS -o /dev/null -w "%{http_code}" -c "$COOKIES" \
    --data-urlencode "username=$USERNAME" \
    --data-urlencode "email=$EMAIL" \
    --data-urlencode "password=$PASSWORD" \
    "$BASE/register")
if [ "$REG_STATUS" != "302" ] && [ "$REG_STATUS" != "303" ] && [ "$REG_STATUS" != "200" ]; then
    fail "register returned $REG_STATUS"
fi

echo "== Log in"
LOGIN_STATUS=$(curl -sS -o /dev/null -w "%{http_code}" -c "$COOKIES" \
    --data-urlencode "username=$USERNAME" \
    --data-urlencode "password=$PASSWORD" \
    "$BASE/login")
if [ "$LOGIN_STATUS" != "302" ] && [ "$LOGIN_STATUS" != "303" ]; then
    fail "login returned $LOGIN_STATUS (expected redirect)"
fi
grep -q JSESSIONID "$COOKIES" || fail "no session cookie after login"

echo "== Generate test image (1x1 PNG)"
IMG="$TMPDIR_SMOKE/test.png"
printf 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/VscvDQAAAABJRU5ErkJggg==' \
    | base64 -d > "$IMG"
[ -s "$IMG" ] || fail "test image not generated"

echo "== Upload photo to restaurant 1"
UPLOAD_RESP=$(curl -fsS -b "$COOKIES" \
    -F "image=@$IMG;type=image/png" \
    "$BASE/api/photos?restaurantId=1&caption=hello%20world")
echo "   $UPLOAD_RESP"
PHOTO_ID=$(echo "$UPLOAD_RESP" | grep -o '"photoId":[0-9]*' | head -1 | cut -d: -f2)
[ -n "$PHOTO_ID" ] || fail "upload returned no photoId"
IMAGE_URL=$(echo "$UPLOAD_RESP" | grep -o '"imageUrl":"[^"]*"' | head -1 | cut -d\" -f4)
[ -n "$IMAGE_URL" ] || fail "upload returned no imageUrl"

echo "== Fetch photos for restaurant 1"
LIST=$(curl -fsS "$BASE/api/photos?restaurantId=1")
echo "$LIST" | grep -q "\"photoId\":$PHOTO_ID" || fail "uploaded photo not in restaurant list"

echo "== Fetch the served image bytes"
IMG_BYTES="$TMPDIR_SMOKE/fetched.png"
curl -fsS "http://localhost:8080$IMAGE_URL" -o "$IMG_BYTES"
[ -s "$IMG_BYTES" ] || fail "image file fetch returned empty body"

echo "== Edit caption"
EDIT_BODY="{\"photoId\":$PHOTO_ID,\"caption\":\"edited caption\"}"
curl -fsS -b "$COOKIES" -X PUT \
    -H "Content-Type: application/json" \
    -d "$EDIT_BODY" \
    "$BASE/api/photos" > /dev/null

AFTER_EDIT=$(curl -fsS "$BASE/api/photos?restaurantId=1")
echo "$AFTER_EDIT" | grep -q '"caption":"edited caption"' || fail "caption was not updated"

echo "== Delete photo"
curl -fsS -b "$COOKIES" -X DELETE "$BASE/api/photos?photoId=$PHOTO_ID" > /dev/null

FINAL=$(curl -fsS "$BASE/api/photos?restaurantId=1")
if echo "$FINAL" | grep -q "\"photoId\":$PHOTO_ID"; then
    fail "photo still in list after delete"
fi

echo "== Verify image file is gone"
sleep 1
FETCH_STATUS=$(curl -sS -o /dev/null -w "%{http_code}" "http://localhost:8080$IMAGE_URL")
if [ "$FETCH_STATUS" = "200" ]; then
    fail "image file still served after delete (status 200)"
fi

echo
echo "OK — upload, fetch, serve, edit, delete all passed."
