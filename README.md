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
- **Frontend**: Vanilla HTML / CSS / JavaScript

## Getting started

```bash
npm install
npm start
```

Open [http://localhost:3000](http://localhost:3000).

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
