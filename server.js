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
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
  )
`);

// Migrate existing databases that lack the preview_image column
try {
  db.exec('ALTER TABLE links ADD COLUMN preview_image TEXT');
} catch (_) { /* column already exists */ }

/**
 * Fetch the og:image meta tag from a URL.
 * Returns the image URL string, or null on any failure.
 */
async function fetchOgImage(url) {
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 5000);
    const res = await fetch(url, {
      signal: controller.signal,
      redirect: 'follow',
      headers: {
        'User-Agent': 'Mozilla/5.0 (compatible; Linkelbot/1.0)',
        'Accept': 'text/html'
      }
    });
    clearTimeout(timeout);
    const html = await res.text();
    // Find all <meta> tags and look for og:image property
    const metaTags = html.matchAll(/<meta\s([^>]+?)\/?>/gi);
    for (const m of metaTags) {
      const attrs = m[1];
      if (!/property\s*=\s*["']og:image["']/i.test(attrs)) continue;
      const content = attrs.match(/content\s*=\s*["']([^"']+)["']/i);
      if (content) return content[1];
    }
    return null;
  } catch (_) {
    return null;
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

  // Fetch og:image in the background and persist it so it appears on next load
  fetchOgImage(url.trim()).then(previewImage => {
    if (previewImage) {
      db.prepare('UPDATE links SET preview_image = ? WHERE id = ?').run(previewImage, linkId);
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
});
