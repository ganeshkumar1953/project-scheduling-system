// index.js — Optimized Project Scheduling System (Production)
// Auto-detect API URL: use same origin when served from Spring Boot, fallback to Railway
const API = (function() {
  const loc = window.location;
  if (loc.hostname !== '' && loc.protocol.startsWith('http')) {
    return loc.origin + '/api';
  }
  return 'https://project-scheduling-system-production.up.railway.app/api';
})();

// Session state
let currentStudentEmail = sessionStorage.getItem('studentEmail');
let isAdminLoggedIn = sessionStorage.getItem('role') === 'ADMIN';

// Initialize view on load
window.addEventListener('DOMContentLoaded', () => {
  const role = sessionStorage.getItem('role');
  if (role === 'ADMIN') {
    showModule('admin');
  } else if (role === 'STUDENT' && currentStudentEmail) {
    document.getElementById('studentEmailDisplay').textContent = currentStudentEmail;
    showModule('student');
  } else {
    sessionStorage.clear();
    currentStudentEmail = null;
    isAdminLoggedIn = false;
    showModule('registration');
  }
});

// Prevent back-button access to protected pages
window.addEventListener('popstate', () => {
  const role = sessionStorage.getItem('role');
  if (!role) showModule('registration');
});

// ══════════════════════════════════════════════════════════════════════
// XSS PROTECTION — escape all user data before rendering
// ══════════════════════════════════════════════════════════════════════

function esc(str) {
  if (str == null) return '';
  const div = document.createElement('div');
  div.textContent = String(str);
  return div.innerHTML;
}

// ══════════════════════════════════════════════════════════════════════
// UTILITIES (Loading, Errors, Toasts)
// ══════════════════════════════════════════════════════════════════════

let isLoading = false;

function setLoading(loading) {
  isLoading = loading;
  const overlay = document.getElementById('loadingOverlay');
  if (loading) {
    overlay.classList.add('active');
  } else {
    overlay.classList.remove('active');
  }
  document.querySelectorAll('button[type="submit"], .btn-primary, .btn-danger, .btn-warning').forEach(btn => btn.disabled = loading);
}

function showToast(message, type = 'success') {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.className = `toast ${type} show`;
  setTimeout(() => { toast.className = 'toast'; }, 3500);
}

function clearErrors() {
  document.querySelectorAll('.error-text').forEach(el => el.textContent = '');
  document.querySelectorAll('.input-error').forEach(el => el.classList.remove('input-error'));
  document.querySelectorAll('.msg-success, .msg-error').forEach(el => {
    el.style.display = 'none';
    el.textContent = '';
  });
}

function showError(inputId, message) {
  const errEl = document.getElementById(`${inputId}Err`);
  const inputEl = document.getElementById(inputId);
  if (errEl) errEl.textContent = message;
  if (inputEl) inputEl.classList.add('input-error');
}

function handleApiErrors(err) {
  if (err && typeof err === 'object' && !err.error && Object.keys(err).length > 0) {
    for (const [field, msg] of Object.entries(err)) {
      showError(field, msg);
    }
    showToast('Please fix the errors in the form.', 'error');
  } else {
    showToast(err.error || err.message || 'An unexpected error occurred.', 'error');
  }
}

function getRoleHeaders() {
  const role = sessionStorage.getItem('role');
  const headers = { 'Content-Type': 'application/json' };
  if (role) headers['X-Role'] = role;
  return headers;
}

async function apiFetch(url, options = {}) {
  if (isLoading) return; // Prevent double-click / spam
  setLoading(true);
  clearErrors();
  try {
    const res = await fetch(url, {
      headers: getRoleHeaders(),
      ...options
    });

    if (res.status === 403) {
      let errPayload;
      try { errPayload = await res.json(); } catch(e) { errPayload = { error: 'Access denied.' }; }
      // Force logout on 403
      sessionStorage.clear();
      currentStudentEmail = null;
      isAdminLoggedIn = false;
      showModule('registration');
      throw errPayload;
    }

    if (!res.ok) {
      let errPayload;
      try { errPayload = await res.json(); } catch(e) { errPayload = { error: 'Server error (' + res.status + ')' }; }
      throw errPayload;
    }

    if (res.status === 204) return null;
    return await res.json();
  } catch (err) {
    if (err instanceof TypeError && err.message === 'Failed to fetch') {
      throw { error: 'Network error. Please check your connection and try again.' };
    }
    throw err;
  } finally {
    setLoading(false);
  }
}

function badge(status) {
  return `<span class="badge badge-${esc(status).toLowerCase()}">${esc(status)}</span>`;
}

function emptyMsg(text) {
  return `<p class="empty-msg">${esc(text)}</p>`;
}

function wrapTable(html) {
  return `<div class="table-wrapper">${html}</div>`;
}

function isValidEmail(email) {
  return /^[a-zA-Z0-9._%+-]+@gmail\.com$/.test(email);
}

// Clear old messages on input change
document.addEventListener('input', () => {
  document.querySelectorAll('.error-text').forEach(el => el.textContent = '');
  document.querySelectorAll('.input-error').forEach(el => el.classList.remove('input-error'));
});

// ══════════════════════════════════════════════════════════════════════
// NAVIGATION & AUTH
// ══════════════════════════════════════════════════════════════════════

function toggleRequired(el, isVisible) {
  if (!el) return;
  if (isVisible) {
    el.querySelectorAll('[data-was-required="true"]').forEach(i => i.setAttribute('required', 'required'));
  } else {
    el.querySelectorAll('[required]').forEach(i => {
      i.setAttribute('data-was-required', 'true');
      i.removeAttribute('required');
    });
  }
}

function hideAllSections() {
  document.querySelectorAll('.module').forEach(m => {
    m.style.display = 'none';
    toggleRequired(m, false);
  });
  document.querySelectorAll('.submenu').forEach(s => {
    s.style.display = 'none';
    toggleRequired(s, false);
  });
}

function setActiveNav(id) {
  document.querySelectorAll('#mainNav button').forEach(btn => btn.classList.remove('active'));
  if (id) {
    const btn = document.getElementById('nav-' + id);
    if (btn) btn.classList.add('active');
  }
}

function showModule(moduleId) {
  const role = sessionStorage.getItem('role');
  
  // Guard Admin
  if (moduleId === 'admin' && role !== 'ADMIN') {
    if (role === 'STUDENT') { showToast('Student cannot access admin module.', 'error'); return showModule('student'); }
    return openAdminLogin();
  }
  
  // Guard Student
  if (moduleId === 'student' && role !== 'STUDENT') {
    if (role === 'ADMIN') { showToast('Admin cannot access student module.', 'error'); return showModule('admin'); }
    return openStudentLogin();
  }

  // Hide nav items based on role
  document.getElementById('nav-admin').style.display = role === 'STUDENT' ? 'none' : '';
  document.getElementById('nav-student').style.display = role === 'ADMIN' ? 'none' : '';
  document.getElementById('nav-registration').style.display = (role === 'ADMIN' || role === 'STUDENT') ? 'none' : '';

  hideAllSections();
  if (moduleId) {
    const el = document.getElementById(moduleId);
    if (el) {
      el.style.display = 'block';
      toggleRequired(el, true);
      if (moduleId === 'registration') {
        const regForm = document.getElementById('registrationForm');
        if (regForm) {
          regForm.style.display = 'none';
          toggleRequired(regForm, false);
        }
      }
      setActiveNav(moduleId);
    }
  }
  clearErrors();
}

function showSubmenu(name) {
  document.querySelectorAll('.submenu').forEach(s => {
    s.style.display = 'none';
    toggleRequired(s, false);
  });
  const el = document.getElementById(name);
  if (el) {
    el.style.display = 'block';
    toggleRequired(el, true);
  }
  clearErrors();

  if (name === 'schedule')    { loadDemoDates(); loadAdminSlots(); }
  if (name === 'teams')       { loadAllTeams(); loadAllBookings(); loadWaitingList(); }
  if (name === 'slotBooking') { loadAvailableSlots(); }
  if (name === 'teamBooking') { loadMyTeam(); loadMyBookings(); }
  if (name === 'scheduleView') { loadPublicSchedule(); }
  if (name === 'reports')     { loadFullSchedule(); loadBookingSummary(); }
}

// ── Admin Auth ─────────────────────────────────────────────────────

function openAdminLogin() {
  if (isAdminLoggedIn) {
    showModule('admin');
    return;
  }
  document.getElementById('adminLoginModal').classList.add('active');
  document.getElementById('adminUsername').focus();
}

function closeAdminLogin() {
  document.getElementById('adminLoginModal').classList.remove('active');
  document.getElementById('adminUsername').value = '';
  document.getElementById('adminPassword').value = '';
  clearErrors();
}

async function handleAdminLogin(event) {
  event.preventDefault();
  const username = document.getElementById('adminUsername').value.trim();
  const password = document.getElementById('adminPassword').value;

  if (!username) { showError('adminUsername', 'Username is required.'); return; }
  if (!password) { showError('adminPassword', 'Password is required.'); return; }

  try {
    const result = await apiFetch(`${API}/auth/admin/login`, {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });
    if (result.success) {
      isAdminLoggedIn = true;
      sessionStorage.setItem('role', 'ADMIN');
      closeAdminLogin();
      showModule('admin');
      showToast('Admin login successful!', 'success');
    } else {
      showError('adminPassword', result.error || 'Invalid credentials.');
    }
  } catch (err) {
    handleApiErrors(err);
  }
}

function adminLogout() {
  isAdminLoggedIn = false;
  sessionStorage.clear();
  showModule('registration');
  showToast('Logged out successfully.', 'success');
}

// ── Student Auth ───────────────────────────────────────────────────

function openStudentLogin() {
  if (currentStudentEmail) {
    showModule('student');
    return;
  }
  document.getElementById('studentLoginModal').classList.add('active');
  document.getElementById('studentLoginEmail').focus();
}

function closeStudentLogin() {
  document.getElementById('studentLoginModal').classList.remove('active');
  document.getElementById('studentLoginEmail').value = '';
  clearErrors();
}

async function handleStudentLogin(event) {
  event.preventDefault();
  const email = document.getElementById('studentLoginEmail').value.trim();

  if (!email) { showError('studentLoginEmail', 'Email is required.'); return; }
  if (!isValidEmail(email)) { showError('studentLoginEmail', 'Only Gmail addresses are allowed (e.g. name@gmail.com)'); return; }

  try {
    const team = await apiFetch(`${API}/students/${encodeURIComponent(email)}/team`);
    currentStudentEmail = email;
    sessionStorage.setItem('role', 'STUDENT');
    sessionStorage.setItem('studentEmail', email);
    closeStudentLogin();
    document.getElementById('studentEmailDisplay').textContent = email;
    showModule('student');
    showToast(`Welcome, ${team.projectName}!`, 'success');
  } catch (err) {
    if (err.error && err.error.includes('Team not found')) {
      closeStudentLogin();
      document.getElementById('notRegisteredModal').classList.add('active');
    } else {
      handleApiErrors(err);
    }
  }
}

function closeNotRegistered() {
  document.getElementById('notRegisteredModal').classList.remove('active');
}

function goToRegistration() {
  closeNotRegistered();
  showModule('registration');
  openRegistrationForm();
}

function studentLogout() {
  currentStudentEmail = null;
  sessionStorage.clear();
  showModule('registration');
  showToast('Logged out successfully.', 'success');
}

// ══════════════════════════════════════════════════════════════════════
// REGISTRATION — Dynamic Member Fields
// ══════════════════════════════════════════════════════════════════════

function openRegistrationForm() {
  const el = document.getElementById('registrationForm');
  el.style.display = 'block';
  toggleRequired(el, true);
  clearErrors();
  resetMemberFields();
}

function resetMemberFields() {
  const container = document.getElementById('memberFieldsContainer');
  container.innerHTML = `<div class="member-row">
    <input type="text" name="memberName" required placeholder="Member 1 name">
  </div>`;
  updateMemberCount();
}

function addMemberField() {
  const container = document.getElementById('memberFieldsContainer');
  const count = container.querySelectorAll('.member-row').length;
  if (count >= 5) {
    showToast('Maximum 5 members allowed.', 'error');
    return;
  }
  const row = document.createElement('div');
  row.className = 'member-row';
  row.innerHTML = `<input type="text" name="memberName" required placeholder="Member ${count + 1} name">
    <button type="button" class="btn-remove-member" onclick="removeMemberField(this)">✕</button>`;
  container.appendChild(row);
  updateMemberCount();
  row.querySelector('input').focus();
}

function removeMemberField(btn) {
  const container = document.getElementById('memberFieldsContainer');
  if (container.querySelectorAll('.member-row').length <= 1) {
    showToast('At least 1 member is required.', 'error');
    return;
  }
  btn.closest('.member-row').remove();
  updateMemberCount();
  container.querySelectorAll('.member-row input').forEach((input, i) => {
    input.placeholder = `Member ${i + 1} name`;
  });
}

function updateMemberCount() {
  const count = document.getElementById('memberFieldsContainer').querySelectorAll('.member-row').length;
  document.getElementById('memberCountInfo').textContent = `${count} / 5 members`;
  const addBtn = document.getElementById('btnAddMember');
  if (addBtn) addBtn.style.display = count >= 5 ? 'none' : '';
}

function getMemberNames() {
  const inputs = document.querySelectorAll('#memberFieldsContainer input[name="memberName"]');
  return Array.from(inputs).map(i => i.value.trim()).filter(n => n.length > 0);
}

// ══════════════════════════════════════════════════════════════════════
// STUDENT REGISTRATION
// ══════════════════════════════════════════════════════════════════════

async function submitRegistration(event) {
  event.preventDefault();

  const projectName = document.getElementById('projectName').value.trim();
  const leaderName = document.getElementById('leaderName').value.trim();
  const email = document.getElementById('email').value.trim();
  const description = document.getElementById('description').value.trim();
  const memberNames = getMemberNames();

  // Frontend validation
  let hasError = false;
  if (!projectName) { showError('projectName', 'Project name is required.'); hasError = true; }
  if (!leaderName) { showError('leaderName', 'Leader name is required.'); hasError = true; }
  if (!email) { showError('email', 'Email is required.'); hasError = true; }
  else if (!isValidEmail(email)) { showError('email', 'Only Gmail addresses are allowed (e.g. name@gmail.com)'); hasError = true; }
  if (memberNames.length === 0) { showError('members', 'At least 1 member name is required.'); hasError = true; }
  if (memberNames.length > 5) { showError('members', 'Maximum 5 members allowed.'); hasError = true; }
  if (hasError) return;

  const payload = {
    projectName,
    members: memberNames.length,
    leaderName,
    email,
    description: description || null,
    memberNames
  };

  try {
    const team = await apiFetch(`${API}/students/register`, { method: 'POST', body: JSON.stringify(payload) });
    const msgDiv = document.getElementById('regMessage');
    msgDiv.textContent = `Team "${team.projectName}" registered successfully! (Team ID: ${team.id})`;
    msgDiv.className = 'msg-success';
    msgDiv.style.display = 'block';
    showToast('Team registered successfully!', 'success');
    document.querySelector('#registrationForm form').reset();
    resetMemberFields();
  } catch (err) {
    handleApiErrors(err);
  }
}

// ══════════════════════════════════════════════════════════════════════
// ADMIN MODULE
// ══════════════════════════════════════════════════════════════════════

async function addDemoDate(event) {
  event.preventDefault();
  const dateVal = document.getElementById('newDemoDate').value;
  try {
    await apiFetch(`${API}/admin/dates`, { method: 'POST', body: JSON.stringify({ demoDate: dateVal }) });
    showToast('Demo date added!', 'success');
    document.getElementById('newDemoDate').value = '';
    const successDiv = document.getElementById('dateSuccessMsg');
    successDiv.textContent = 'Date successfully added.';
    successDiv.style.display = 'block';
    await loadDemoDates();
  } catch (err) { handleApiErrors(err); }
}

async function loadDemoDates() {
  try {
    const dates = await apiFetch(`${API}/admin/dates`);
    const c = document.getElementById('datesContainer');
    if (!dates || !dates.length) { c.innerHTML = emptyMsg('No demo dates added yet.'); populateDateSelects([]); return; }
    c.innerHTML = wrapTable(`<table>
      <thead><tr><th>ID</th><th>Date</th><th>Action</th></tr></thead>
      <tbody>${dates.map(d => `<tr>
        <td>${esc(d.id)}</td><td>${esc(d.demoDate)}</td>
        <td><button onclick="deleteDemoDate(${d.id})" class="btn-danger">Delete</button></td>
      </tr>`).join('')}</tbody></table>`);
    populateDateSelects(dates);
  } catch (err) { showToast('Failed to load dates.', 'error'); }
}

async function deleteDemoDate(id) {
  if (!confirm('Warning: Deleting this date removes ALL its slots and related bookings. Proceed?')) return;
  try {
    await apiFetch(`${API}/admin/dates/${id}`, { method: 'DELETE' });
    showToast('Date deleted.', 'success');
    await loadDemoDates();
    await loadAdminSlots();
  } catch (err) { handleApiErrors(err); }
}

function populateDateSelects(dates) {
  ['slotDateSelect', 'filterDateSelect'].forEach(sid => {
    const sel = document.getElementById(sid);
    if (!sel) return;
    const prev = sel.value;
    const isFilter = sid === 'filterDateSelect';
    sel.innerHTML = isFilter ? '<option value="">All Dates</option>' : '<option value="">-- Select --</option>';
    dates.forEach(d => {
      const opt = document.createElement('option');
      opt.value = d.id;
      opt.textContent = d.demoDate;
      if (String(prev) === String(d.id)) opt.selected = true;
      sel.appendChild(opt);
    });
  });
}

// ── ADMIN SLOTS ────────────────────────────────────────────────────────

async function createSlot(event) {
  event.preventDefault();
  const dateId = document.getElementById('slotDateSelect').value;
  if (!dateId) { showError('slotDateSelect', 'Date is required.'); return; }
  try {
    await apiFetch(`${API}/admin/slots`, {
      method: 'POST',
      body: JSON.stringify({
        scheduleDateId: parseInt(dateId),
        startTime: document.getElementById('slotStart').value,
        endTime:   document.getElementById('slotEnd').value
      })
    });
    showToast('Slot created!', 'success');
    document.getElementById('slotStart').value = '';
    document.getElementById('slotEnd').value = '';
    const successDiv = document.getElementById('slotSuccessMsg');
    successDiv.textContent = 'Slot created successfully.';
    successDiv.style.display = 'block';
    await loadAdminSlots();
  } catch (err) { handleApiErrors(err); }
}

async function loadAdminSlots() {
  const dateId = document.getElementById('filterDateSelect')?.value || '';
  const url = dateId ? `${API}/admin/slots?dateId=${dateId}` : `${API}/admin/slots`;
  try {
    const slots = await apiFetch(url);
    const c = document.getElementById('adminSlotsContainer');
    if (!slots || !slots.length) { c.innerHTML = emptyMsg('No slots available.'); return; }
    c.innerHTML = wrapTable(`<table>
      <thead><tr><th>ID</th><th>Date</th><th>Start</th><th>End</th><th>Status</th><th>Action</th></tr></thead>
      <tbody>${slots.map(s => `<tr>
        <td>${esc(s.id)}</td><td>${esc(s.demoDate)}</td><td>${esc(s.startTime)}</td><td>${esc(s.endTime)}</td>
        <td>${badge(s.status)}</td>
        <td><button onclick="deleteSlot(${s.id})" class="btn-danger">Delete</button></td>
      </tr>`).join('')}</tbody></table>`);
  } catch (err) { showToast('Failed to load slots.', 'error'); }
}

async function deleteSlot(id) {
  if (!confirm('Delete this slot? Related bookings will be removed.')) return;
  try {
    await apiFetch(`${API}/admin/slots/${id}`, { method: 'DELETE' });
    showToast('Slot deleted.', 'success');
    await loadAdminSlots();
  } catch (err) { handleApiErrors(err); }
}

// ── ADMIN TEAMS & BOOKINGS ─────────────────────────────────────────────

async function loadAllTeams() {
  try {
    const teams = await apiFetch(`${API}/admin/teams`);
    const c = document.getElementById('allTeamsContainer');
    if (!teams || !teams.length) { c.innerHTML = emptyMsg('No teams registered yet.'); return; }
    c.innerHTML = wrapTable(`<table>
      <thead><tr><th>ID</th><th>Project</th><th>Leader</th><th>Email</th><th>Members</th><th>Action</th></tr></thead>
      <tbody>${teams.map(t => `<tr>
        <td>${esc(t.id)}</td><td>${esc(t.projectName)}</td><td>${esc(t.leaderName)}</td><td>${esc(t.email)}</td>
        <td>${t.memberNames && t.memberNames.length ? t.memberNames.map(esc).join(', ') : esc(t.members) + ' member(s)'}</td>
        <td><button onclick="deleteTeam(${t.id}, '${esc(t.projectName).replace(/'/g, "\\'")}')" class="btn-danger">Delete</button></td>
      </tr>`).join('')}</tbody></table>`);
  } catch (err) { showToast('Failed to load teams.', 'error'); }
}

async function deleteTeam(teamId, teamName) {
  if (!confirm(`⚠️ Are you sure you want to delete team "${teamName}" and ALL its data (members, bookings)?\\n\\nThis action cannot be undone.`)) return;
  try {
    await apiFetch(`${API}/admin/teams/${teamId}`, { method: 'DELETE' });
    showToast('Team deleted successfully!', 'success');
    await loadAllTeams();
    await loadAllBookings();
    await loadWaitingList();
    await loadAdminSlots();
  } catch (err) { handleApiErrors(err); }
}


async function loadAllBookings() {
  try {
    const bookings = await apiFetch(`${API}/admin/bookings`);
    const c = document.getElementById('allBookingsContainer');
    if (!bookings || !bookings.length) { c.innerHTML = emptyMsg('No bookings found.'); return; }
    c.innerHTML = wrapTable(`<table>
      <thead><tr><th>ID</th><th>Team</th><th>Date</th><th>Time</th><th>Status</th><th>Action</th></tr></thead>
      <tbody>${bookings.map(b => `<tr>
        <td>${esc(b.id)}</td><td>${esc(b.teamProjectName)}</td><td>${esc(b.slotDate)}</td>
        <td>${esc(b.slotStartTime)} – ${esc(b.slotEndTime)}</td><td>${badge(b.status)}</td>
        <td>${b.status !== 'CANCELLED' ? `<button onclick="adminCancelBookingById(${b.id})" class="btn-danger">Cancel</button>` : '—'}</td>
      </tr>`).join('')}</tbody></table>`);
  } catch (err) { showToast('Failed to load bookings.', 'error'); }
}

async function adminCancelBookingById(id) {
  if (!confirm(`Cancel booking #${id}? Waitlisted bookings will be auto-promoted if applicable.`)) return;
  try {
    await apiFetch(`${API}/admin/bookings/${id}`, { method: 'DELETE' });
    showToast('Booking cancelled successfully.', 'success');
    await loadAllBookings();
    await loadAdminSlots();
    await loadWaitingList();
  } catch (err) { handleApiErrors(err); }
}

async function loadWaitingList() {
  try {
    const list = await apiFetch(`${API}/admin/bookings/waitlist`);
    const c = document.getElementById('waitlistContainer');
    if (!list || !list.length) { c.innerHTML = emptyMsg('No bookings in waiting list.'); return; }
    c.innerHTML = wrapTable(`<table>
      <thead><tr><th>Booking</th><th>Team</th><th>Date</th><th>Time</th></tr></thead>
      <tbody>${list.map(b => `<tr>
        <td>${esc(b.id)}</td><td>${esc(b.teamProjectName)} (${esc(b.teamEmail)})</td>
        <td>${esc(b.slotDate)}</td><td>${esc(b.slotStartTime)} – ${esc(b.slotEndTime)}</td>
      </tr>`).join('')}</tbody></table>`);
  } catch (err) { showToast('Failed to load waitlist.', 'error'); }
}

// ── ADMIN REPORTS ──────────────────────────────────────────────────────

async function loadFullSchedule() {
  try {
    const slots = await apiFetch(`${API}/admin/reports/schedule`);
    const c = document.getElementById('fullScheduleContainer');
    if (!slots || !slots.length) { c.innerHTML = emptyMsg('No schedule data found.'); return; }
    c.innerHTML = wrapTable(`<table>
      <thead><tr><th>ID</th><th>Date</th><th>Start</th><th>End</th><th>Status</th></tr></thead>
      <tbody>${slots.map(s => `<tr>
        <td>${esc(s.id)}</td><td>${esc(s.demoDate)}</td><td>${esc(s.startTime)}</td><td>${esc(s.endTime)}</td><td>${badge(s.status)}</td>
      </tr>`).join('')}</tbody></table>`);
  } catch (err) { showToast('Failed to load schedule.', 'error'); }
}

async function loadBookingSummary() {
  try {
    const bookings = await apiFetch(`${API}/admin/reports/summary`);
    const c = document.getElementById('bookingSummaryContainer');
    if (!bookings || !bookings.length) { c.innerHTML = emptyMsg('No bookings found.'); return; }
    c.innerHTML = wrapTable(`<table>
      <thead><tr><th>ID</th><th>Team</th><th>Leader</th><th>Date</th><th>Time</th><th>Status</th></tr></thead>
      <tbody>${bookings.map(b => `<tr>
        <td>${esc(b.id)}</td><td>${esc(b.teamProjectName)}</td><td>${esc(b.teamLeaderName)}</td>
        <td>${esc(b.slotDate)}</td><td>${esc(b.slotStartTime)} – ${esc(b.slotEndTime)}</td><td>${badge(b.status)}</td>
      </tr>`).join('')}</tbody></table>`);
  } catch (err) { showToast('Failed to load summary.', 'error'); }
}

function exportCsv(type) {
  const url = type === 'schedule' ? `${API}/admin/reports/export/csv` : `${API}/admin/reports/export/teams-csv`;
  window.open(url, '_blank');
}

// ══════════════════════════════════════════════════════════════════════
// STUDENT MODULE
// ══════════════════════════════════════════════════════════════════════

function getStudentEmail() {
  if (!currentStudentEmail) {
    showToast('Session expired. Please login again.', 'error');
    studentLogout();
    return null;
  }
  return currentStudentEmail;
}

// ── STUDENT: AVAILABLE SLOTS & BOOK ────────────────────────────────────

async function loadAvailableSlots(forRescheduleBookingId = null) {
  try {
    const slots = await apiFetch(forRescheduleBookingId ? `${API}/bookings/all` : `${API}/bookings/available`);
    const c = document.getElementById('availableSlotsContainer');

    if (!slots || !slots.length) {
      c.innerHTML = emptyMsg(forRescheduleBookingId ? 'No slots found.' : 'No slots available right now.');
      return;
    }

    let tableHtml = `<table>
      <thead><tr><th>Date</th><th>Time</th><th>Status</th><th>Action</th></tr></thead><tbody>`;

    slots.forEach(s => {
      let actionBtn = '';
      if (forRescheduleBookingId) {
        actionBtn = `<button onclick="confirmReschedule(${forRescheduleBookingId}, ${s.id})" class="btn-warning">Select</button>`;
      } else {
        if (s.status === 'AVAILABLE' || s.status === 'BOOKED') {
          const isWaitlist = s.status === 'BOOKED';
          actionBtn = `<button onclick="bookSlot(${s.id})" class="${isWaitlist ? 'btn-warning' : 'btn-primary'}" style="margin-top:0;padding:6px 14px;">${isWaitlist ? 'Join Waitlist' : 'Book Slot'}</button>`;
        } else {
          actionBtn = '—';
        }
      }

      tableHtml += `<tr>
        <td>${esc(s.demoDate)}</td><td>${esc(s.startTime)} – ${esc(s.endTime)}</td>
        <td>${badge(s.status)}</td>
        <td>${actionBtn}</td>
      </tr>`;
    });
    tableHtml += `</tbody></table>`;

    if (forRescheduleBookingId) {
      tableHtml = `<div class="reschedule-banner">
          <span><strong>Reschedule Mode:</strong> Pick a new slot for booking #${esc(forRescheduleBookingId)}.</span>
          <button onclick="loadAvailableSlots()" class="btn-secondary" style="margin:0;">Cancel</button>
        </div>` + wrapTable(tableHtml);
    } else {
      tableHtml = wrapTable(tableHtml);
    }

    c.innerHTML = tableHtml;
  } catch (err) { handleApiErrors(err); }
}

async function bookSlot(slotId) {
  const email = getStudentEmail();
  if (!email) return;

  try {
    const team = await apiFetch(`${API}/students/${encodeURIComponent(email)}/team`);
    const booking = await apiFetch(`${API}/bookings`, {
      method: 'POST',
      body: JSON.stringify({ teamId: team.id, slotId: slotId })
    });
    
    if (booking.status === 'WAITLISTED') {
      showToast('Slot full. You have been added to waitlist', 'warning');
    } else if (booking.status === 'CONFIRMED') {
      showToast('Slot booked successfully', 'success');
    } else {
      showToast(`Slot booked! Status: ${booking.status}`, 'success');
    }
    
    await loadAvailableSlots();
  } catch (err) { handleApiErrors(err); }
}

// ── STUDENT: MY TEAM & BOOKINGS ────────────────────────────────────────

async function loadMyTeam() {
  const email = getStudentEmail();
  if (!email) return;
  try {
    const t = await apiFetch(`${API}/students/${encodeURIComponent(email)}/team`);
    const membersDisplay = t.memberNames && t.memberNames.length
      ? t.memberNames.map(esc).join(', ')
      : esc(t.members) + ' member(s)';
    document.getElementById('myTeamContainer').innerHTML = wrapTable(`<table>
      <thead><tr><th>Field</th><th>Value</th></tr></thead>
      <tbody>
        <tr><td><strong>Team ID</strong></td><td>${esc(t.id)}</td></tr>
        <tr><td><strong>Project</strong></td><td>${esc(t.projectName)}</td></tr>
        <tr><td><strong>Leader</strong></td><td>${esc(t.leaderName)}</td></tr>
        <tr><td><strong>Email</strong></td><td>${esc(t.email)}</td></tr>
        <tr><td><strong>Members</strong></td><td>${membersDisplay}</td></tr>
        ${t.description ? `<tr><td><strong>Description</strong></td><td>${esc(t.description)}</td></tr>` : ''}
      </tbody></table>`);
  } catch (err) { handleApiErrors(err); }
}

async function loadMyBookings() {
  const email = getStudentEmail();
  if (!email) return;
  try {
    const bookings = await apiFetch(`${API}/students/${encodeURIComponent(email)}/bookings`);
    const c = document.getElementById('myBookingsContainer');
    if (!bookings || !bookings.length) { c.innerHTML = emptyMsg('No bookings found.'); return; }

    let tableHtml = `<table>
      <thead><tr><th>ID</th><th>Date</th><th>Time</th><th>Status</th><th>Actions</th></tr></thead><tbody>`;

    bookings.forEach(b => {
      let actions = '';
      if (b.status !== 'CANCELLED') {
        actions = `<button onclick="startReschedule(${b.id})" class="btn-warning" style="margin-right:4px;">Reschedule</button>
                   <button onclick="cancelMyBooking(${b.id})" class="btn-danger">Cancel</button>`;
      } else {
        actions = '—';
      }
      tableHtml += `<tr>
        <td>${esc(b.id)}</td><td>${esc(b.slotDate)}</td><td>${esc(b.slotStartTime)} – ${esc(b.slotEndTime)}</td>
        <td>${badge(b.status)}</td>
        <td style="white-space:nowrap">${actions}</td>
      </tr>`;
    });
    tableHtml += `</tbody></table>`;
    c.innerHTML = wrapTable(tableHtml);
  } catch (err) { handleApiErrors(err); }
}

// ── STUDENT: ACTIONS ───────────────────────────────────────────────────

async function cancelMyBooking(bookingId) {
  if (!confirm(`Are you sure you want to cancel booking #${bookingId}?`)) return;
  try {
    await apiFetch(`${API}/bookings/${bookingId}`, { method: 'DELETE' });
    showToast('Your booking was successfully cancelled.', 'success');
    await loadMyBookings();
    if (document.getElementById('slotBooking').style.display === 'block') {
       await loadAvailableSlots();
    }
  } catch (err) { handleApiErrors(err); }
}

function startReschedule(bookingId) {
  showSubmenu('slotBooking');
  loadAvailableSlots(bookingId);
}

async function confirmReschedule(bookingId, newSlotId) {
  if (!confirm('Are you sure you want to reschedule your booking to this slot?')) return;
  try {
    const booking = await apiFetch(`${API}/bookings/${bookingId}/reschedule`, {
      method: 'PUT',
      body: JSON.stringify({ slotId: newSlotId })
    });
    
    if (booking.status === 'WAITLISTED') {
      showToast('Slot full. You have been added to waitlist', 'warning');
    } else if (booking.status === 'CONFIRMED') {
      showToast('Slot booked successfully', 'success');
    } else {
      showToast(`Successfully rescheduled! Status: ${booking.status}`, 'success');
    }
    
    await loadAvailableSlots();
  } catch (err) { handleApiErrors(err); }
}

// ── STUDENT: SCHEDULE ──────────────────────────────────────────────────

async function loadPublicSchedule() {
  try {
    const slots = await apiFetch(`${API}/bookings/all`);
    const c = document.getElementById('publicScheduleContainer');
    if (!slots || !slots.length) { c.innerHTML = emptyMsg('No schedule data found.'); return; }
    c.innerHTML = wrapTable(`<table>
      <thead><tr><th>Date</th><th>Start</th><th>End</th><th>Status</th></tr></thead>
      <tbody>${slots.map(s => `<tr>
        <td>${esc(s.demoDate)}</td><td>${esc(s.startTime)}</td><td>${esc(s.endTime)}</td><td>${badge(s.status)}</td>
      </tr>`).join('')}</tbody></table>`);
  } catch (err) { handleApiErrors(err); }
}
