# Local smoke test for photo uploads

Runs the full photo-upload flow end-to-end against a local Postgres +
Tomcat — no Supabase access needed. Useful for verifying the feature
before your teammates pull it down.

## One-time setup

```
brew install openjdk@25 maven
brew install --cask docker        # if Docker isn't already installed
open -a Docker                    # start Docker Desktop
```

## Three terminal windows

### 1. Start Postgres + schema

```
./scripts/setup-db.sh
```

Brings up `csci201_postgres` on port 5432 and applies:
- `scripts/seed.sql` (minimum `users`, `restaurants`, `reviews`, `rankings` tables, plus a test restaurant with `restaurant_id = 1`)
- `sql/photos.sql`

Re-running is safe; it's idempotent.

### 2. Build the WAR and run Tomcat

```
./scripts/run-server.sh
```

First run downloads Tomcat 10 into `scripts/.tomcat/`. Subsequent runs
reuse it. The server runs in the foreground and uses these env vars:

- `DB_URL=jdbc:postgresql://localhost:5432/postgres`
- `DB_USER=postgres`
- `DB_PASSWORD=pg`
- `PHOTO_UPLOAD_DIR=scripts/.upload` (override by exporting the var)

Home page: `http://localhost:8080/CSCI201_Project/`
Photos page: `http://localhost:8080/CSCI201_Project/photos.html`

### 3. Run the automated smoke test

```
./scripts/smoke-test.sh
```

Registers a random user, logs in, uploads a 1×1 PNG to restaurant `1`,
verifies it's listed, fetches the served image bytes, edits the caption,
deletes the photo, and confirms both the list entry and the file are
gone.

Success:

```
OK — upload, fetch, serve, edit, delete all passed.
```

## Clean up

```
cd scripts && docker compose down -v     # stop + wipe Postgres
rm -rf scripts/.tomcat scripts/.upload    # remove downloaded Tomcat + uploaded files
```

## Troubleshooting

- **Postgres port already in use** → `lsof -i :5432` and stop the other process, or edit `docker-compose.yml`.
- **`mvn` not found** → `brew install maven`.
- **`JAVA_HOME` not set / wrong Java version** → `export JAVA_HOME=/opt/homebrew/opt/openjdk@25`.
- **Tomcat won't start** → check `scripts/.tomcat/logs/catalina.out`.
- **Smoke test fails at "Server not reachable"** → `./scripts/run-server.sh` isn't running yet or still starting up.
- **Upload returns "Restaurant not found"** → `setup-db.sh` didn't run, or the seed didn't insert restaurant 1. Re-run setup.
