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

### POST body

```json
{
  "username": "alice",
  "title": "Example",
  "url": "https://example.com",
  "description": "Optional"
}
```
