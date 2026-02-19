const express = require('express');
const Database = require('better-sqlite3');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// Initialize SQLite database
const db = new Database(path.join(__dirname, 'links.db'));

db.exec(`
  CREATE TABLE IF NOT EXISTS links (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    description TEXT,
    preview_image TEXT,
    preview_title TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
  )
`);

// Migrate existing databases that lack new columns
try { db.exec('ALTER TABLE links ADD COLUMN preview_image TEXT'); } catch (_) {}
try { db.exec('ALTER TABLE links ADD COLUMN preview_title TEXT'); } catch (_) {}

/**
 * Fetch og:image and og:title meta tags from a URL.
 * Returns { image, title } with string values or nulls.
 */
async function fetchOgMeta(url) {
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 10000);
    const res = await fetch(url, {
      signal: controller.signal,
      redirect: 'follow',
      headers: {
        'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
      }
    });
    clearTimeout(timeout);
    if (!res.ok) return { image: null, title: null };
    // Only scan the first 50 KB — og meta tags are always in <head>
    const html = (await res.text()).substring(0, 50000);

    let image = null;
    let title = null;

    for (const m of html.matchAll(/<meta\s[^>]*?(?:property|name)\s*=\s*["']og:(image|title)["'][^>]*?>/gi)) {
      const which = m[1].toLowerCase();
      const content = m[0].match(/content\s*=\s*["']([^"']+)["']/i);
      if (!content) continue;
      if (which === 'image' && !image) image = content[1];
      if (which === 'title' && !title) title = content[1];
      if (image && title) break;
    }

    // Fallback: use <title> if no og:title found
    if (!title) {
      const titleMatch = html.match(/<title[^>]*>([^<]+)<\/title>/i);
      if (titleMatch) title = titleMatch[1].trim();
    }

    return { image, title };
  } catch (_) {
    return { image: null, title: null };
  }
}

app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// GET /api/links - fetch all links in chronological order (newest first)
app.get('/api/links', (req, res) => {
  const { order = 'desc', username } = req.query;
  const sortDir = order === 'asc' ? 'ASC' : 'DESC';

  let query = 'SELECT * FROM links';
  const params = [];

  if (username) {
    query += ' WHERE username = ?';
    params.push(username);
  }

  query += ` ORDER BY created_at ${sortDir}`;

  const links = db.prepare(query).all(...params);
  res.json(links);
});

// POST /api/links - submit a new link
app.post('/api/links', (req, res) => {
  const { username, title, url, description } = req.body;

  if (!username || !title || !url) {
    return res.status(400).json({ error: 'username, title, and url are required' });
  }

  // Basic URL validation
  try {
    new URL(url);
  } catch {
    return res.status(400).json({ error: 'Invalid URL format' });
  }

  const stmt = db.prepare(
    'INSERT INTO links (username, title, url, description) VALUES (?, ?, ?, ?)'
  );
  const result = stmt.run(username, title.trim(), url.trim(), description?.trim() || null);
  const linkId = result.lastInsertRowid;

  const link = db.prepare('SELECT * FROM links WHERE id = ?').get(linkId);
  res.status(201).json(link);

  // Fetch og metadata in the background and persist it so it appears on next load
  fetchOgMeta(url.trim()).then(({ image, title }) => {
    if (image || title) {
      db.prepare('UPDATE links SET preview_image = ?, preview_title = ? WHERE id = ?')
        .run(image, title, linkId);
    }
  });
});

// DELETE /api/links/:id - delete a link (only by the submitting user)
app.delete('/api/links/:id', (req, res) => {
  const { id } = req.params;
  const { username } = req.body;

  if (!username) {
    return res.status(400).json({ error: 'username is required' });
  }

  const link = db.prepare('SELECT * FROM links WHERE id = ?').get(id);

  if (!link) {
    return res.status(404).json({ error: 'Link not found' });
  }

  if (link.username !== username) {
    return res.status(403).json({ error: 'You can only delete your own links' });
  }

  db.prepare('DELETE FROM links WHERE id = ?').run(id);
  res.json({ message: 'Link deleted' });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Linkel running at http://0.0.0.0:${PORT}`);

  // Backfill: fetch missing og metadata for existing links
  const stale = db.prepare(
    'SELECT id, url FROM links WHERE preview_title IS NULL'
  ).all();
  for (const row of stale) {
    fetchOgMeta(row.url).then(({ image, title }) => {
      if (image || title) {
        db.prepare(
          'UPDATE links SET preview_image = COALESCE(preview_image, ?), preview_title = ? WHERE id = ?'
        ).run(image, title, row.id);
      }
    });
  }
});
