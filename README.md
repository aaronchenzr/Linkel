# Linkel

A minimal link sharing app with a chronological timeline.

## Features

- Share links with a title, URL, and optional description
- Chronological timeline (newest-first or oldest-first)
- Filter timeline by username
- Delete your own links
- Username persisted in `localStorage` across sessions
- Backed by SQLite — no setup required

## Stack

- **Backend**: Node.js + Express
- **Database**: SQLite via `better-sqlite3`
- **Web frontend**: Vanilla HTML / CSS / JavaScript
- **Android app**: Kotlin, Retrofit, RecyclerView, Material Design 3

## Getting started

### Web

```bash
npm install
npm start
```

Open [http://localhost:3000](http://localhost:3000).

### Android

Open the `android/` folder in Android Studio (Hedgehog or newer). The app talks to the backend via the `BASE_URL` build config field.

- **Emulator**: default URL is `http://10.0.2.2:3000/` (routes to host loopback)
- **Physical device**: change `BASE_URL` in `android/app/build.gradle` to your machine's LAN IP, e.g. `http://192.168.1.x:3000/`

Make sure `JAVA_HOME` is set (or let `gradle.properties` handle it):

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64  # Linux example
```

Build and run from Android Studio, or:

```bash
cd android
./gradlew assembleDebug
```

## API

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/links?order=desc&username=alice` | List links |
| `POST` | `/api/links` | Submit a link |
| `DELETE` | `/api/links/:id` | Delete a link (body: `{ username }`) |

### Pulling links

Fetch links by sending a `GET` request to `/api/links`. The response is a JSON array of link objects.

```bash
# Fetch all links (newest first)
curl http://localhost:3000/api/links
```

#### Query parameters

| Parameter  | Default | Description                          |
|------------|---------|--------------------------------------|
| `order`    | `desc`  | Sort order: `desc` (newest) or `asc` (oldest) |
| `username` | —       | Filter links by a specific username  |

#### Examples

```bash
# Oldest first
curl "http://localhost:3000/api/links?order=asc"

# Only links from alice
curl "http://localhost:3000/api/links?username=alice"

# Combine both
curl "http://localhost:3000/api/links?order=asc&username=alice"
```

#### Response

```json
[
  {
    "id": 1,
    "username": "alice",
    "title": "Example",
    "url": "https://example.com",
    "description": "Optional",
    "created_at": "2026-02-22 12:00:00"
  }
]
```

An empty array `[]` is returned when no links match the query.

### POST body

```json
{
  "username": "alice",
  "title": "Example",
  "url": "https://example.com",
  "description": "Optional"
}
```
