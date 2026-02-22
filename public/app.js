/* Linkel – frontend */

const USERNAME_KEY = 'linkel_username';

let currentUser = localStorage.getItem(USERNAME_KEY) || '';
let deleteTargetId = null;
let activeFilter = '';

// ---- Element refs ----
const usernameInput    = document.getElementById('usernameInput');
const setUsernameBtn   = document.getElementById('setUsernameBtn');
const currentUserSpan  = document.getElementById('currentUser');
const changeUserBtn    = document.getElementById('changeUserBtn');
const submitSection    = document.getElementById('submitSection');
const linkForm         = document.getElementById('linkForm');
const titleInput       = document.getElementById('titleInput');
const urlInput         = document.getElementById('urlInput');
const descInput        = document.getElementById('descInput');
const formError        = document.getElementById('formError');
const submitBtn        = document.getElementById('submitBtn');
const sortOrder        = document.getElementById('sortOrder');
const filterUser       = document.getElementById('filterUser');
const filterBtn        = document.getElementById('filterBtn');
const clearFilterBtn   = document.getElementById('clearFilterBtn');
const linksList        = document.getElementById('linksList');
const emptyState       = document.getElementById('emptyState');
const loadingState     = document.getElementById('loadingState');
const modal            = document.getElementById('modal');
const confirmDelete    = document.getElementById('confirmDelete');
const cancelDelete     = document.getElementById('cancelDelete');

// ---- User management ----
function applyUser(name) {
  currentUser = name;
  localStorage.setItem(USERNAME_KEY, name);

  usernameInput.classList.add('hidden');
  setUsernameBtn.classList.add('hidden');
  currentUserSpan.textContent = `Logged in as ${name}`;
  currentUserSpan.classList.remove('hidden');
  changeUserBtn.classList.remove('hidden');
  submitSection.classList.remove('hidden');
}

function resetUserForm() {
  currentUserSpan.classList.add('hidden');
  changeUserBtn.classList.add('hidden');
  usernameInput.classList.remove('hidden');
  setUsernameBtn.classList.remove('hidden');
  submitSection.classList.add('hidden');
  usernameInput.value = '';
  usernameInput.focus();
}

setUsernameBtn.addEventListener('click', () => {
  const name = usernameInput.value.trim();
  if (!name) { usernameInput.focus(); return; }
  applyUser(name);
});

usernameInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') setUsernameBtn.click();
});

changeUserBtn.addEventListener('click', resetUserForm);

// Restore session
if (currentUser) applyUser(currentUser);

// ---- Link submission ----
linkForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  hideError();

  const body = {
    username: currentUser,
    title: titleInput.value.trim(),
    url: urlInput.value.trim(),
    description: descInput.value.trim(),
  };

  submitBtn.disabled = true;
  submitBtn.textContent = 'Sharing…';

  try {
    const res = await fetch('/api/links', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });

    const data = await res.json();

    if (!res.ok) {
      showError(data.error || 'Could not share link.');
      return;
    }

    linkForm.reset();
    await loadLinks();
  } catch {
    showError('Network error. Please try again.');
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = 'Share Link';
  }
});

// ---- Load & render links ----
async function loadLinks() {
  loadingState.classList.remove('hidden');
  emptyState.classList.add('hidden');
  linksList.innerHTML = '';

  const params = new URLSearchParams({ order: sortOrder.value });
  if (activeFilter) params.set('username', activeFilter);

  try {
    const res = await fetch(`/api/links?${params}`);
    const links = await res.json();

    loadingState.classList.add('hidden');

    if (links.length === 0) {
      emptyState.classList.remove('hidden');
      return;
    }

    linksList.innerHTML = links.map(renderCard).join('');

    // Attach delete listeners
    linksList.querySelectorAll('.delete-btn').forEach((btn) => {
      btn.addEventListener('click', () => openDeleteModal(Number(btn.dataset.id)));
    });
  } catch {
    loadingState.textContent = 'Failed to load links.';
  }
}

function renderCard(link) {
  const date = new Date(link.created_at);
  const dateStr = date.toLocaleDateString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric',
  });
  const timeStr = date.toLocaleTimeString(undefined, {
    hour: '2-digit', minute: '2-digit',
  });

  const isOwner = link.username === currentUser;
  const deleteHtml = isOwner
    ? `<button class="delete-btn" data-id="${link.id}" title="Delete this link">✕</button>`
    : '';

  const descHtml = link.description
    ? `<p class="link-desc">${escapeHtml(link.description)}</p>`
    : '';

  const siteTitleHtml = link.site_title
    ? `<p class="site-title">${escapeHtml(link.site_title)}</p>`
    : '';

  return `
    <div class="link-card" data-id="${link.id}">
      <div class="link-dot"></div>
      <div class="link-content">
        <div class="link-header">
          <h3 class="link-title">
            <a href="${escapeHtml(link.url)}" target="_blank" rel="noopener noreferrer">
              ${escapeHtml(link.title)}
            </a>
          </h3>
          ${deleteHtml}
        </div>
        <div class="link-meta">
          <span class="username">${escapeHtml(link.username)}</span>
          <span>·</span>
          <time datetime="${link.created_at}">${dateStr} at ${timeStr}</time>
        </div>
        <div class="link-url">
          <a href="${escapeHtml(link.url)}" target="_blank" rel="noopener noreferrer">
            ${escapeHtml(truncate(link.url, 80))}
          </a>
        </div>
        ${siteTitleHtml}
        ${descHtml}
      </div>
    </div>
  `;
}

// ---- Sort & filter ----
sortOrder.addEventListener('change', loadLinks);

filterBtn.addEventListener('click', () => {
  activeFilter = filterUser.value.trim();
  loadLinks();
});

filterUser.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') filterBtn.click();
});

clearFilterBtn.addEventListener('click', () => {
  activeFilter = '';
  filterUser.value = '';
  loadLinks();
});

// ---- Delete modal ----
function openDeleteModal(id) {
  deleteTargetId = id;
  modal.classList.remove('hidden');
}

function closeDeleteModal() {
  deleteTargetId = null;
  modal.classList.add('hidden');
}

confirmDelete.addEventListener('click', async () => {
  if (!deleteTargetId) return;

  confirmDelete.disabled = true;

  try {
    const res = await fetch(`/api/links/${deleteTargetId}`, {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: currentUser }),
    });

    if (res.ok) {
      closeDeleteModal();
      await loadLinks();
    }
  } catch {
    // silent – modal stays open
  } finally {
    confirmDelete.disabled = false;
  }
});

cancelDelete.addEventListener('click', closeDeleteModal);

modal.addEventListener('click', (e) => {
  if (e.target === modal) closeDeleteModal();
});

// ---- Helpers ----
function showError(msg) {
  formError.textContent = msg;
  formError.classList.remove('hidden');
}

function hideError() {
  formError.classList.add('hidden');
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function truncate(str, max) {
  return str.length > max ? str.slice(0, max) + '…' : str;
}

// ---- Init ----
loadLinks();
