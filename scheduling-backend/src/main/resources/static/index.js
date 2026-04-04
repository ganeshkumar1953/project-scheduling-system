// index.js — Optimized Project Scheduling System
const API = 'https://project-scheduling-system-production.up.railway.app/api';

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
  // Disable/enable all buttons
  document.querySelectorAll('button').forEach(btn => btn.disabled = loading);
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
    el.innerHTML = '';
  });
}

function showError(inputId, message) {
  const errEl = document.getElementById(`${inputId}Err`);
  const inputEl = document.getElementById(inputId);
  if (errEl) errEl.textContent = message;
  if (inputEl) inputEl.classList.add('input-error');
}

function handleApiErrors(err) {
  // If it's a validation error object mapping fields to issues
  if (err && typeof err === 'object' && !err.error && Object.keys(err).length > 0) {
    for (const [field, msg] of Object.entries(err)) {
      showError(field, msg);
    }
    showToast('Please fix the errors in the form.', 'error');
  } else {
    showToast(err.error || err.message || 'An unexpected error occurred.', 'error');
  }
}

async function apiFetch(url, options = {}) {
  setLoading(true);
  clearErrors();
  try {
    const res = await fetch(url, {
      headers: { 'Content-Type': 'application/json' },
      ...options
    });
    
    if (!res.ok) {
      let errPayload;
      try { errPayload = await res.json(); } catch(e) { errPayload = { error: 'Server error' }; }
      throw errPayload;
    }
    
    if (res.status === 204) return null;
    return await res.json();
  } catch (err) {
    if (err instanceof TypeError && err.message === 'Failed to fetch') {
      throw { error: 'Network failure. Cannot connect to server.' };
    }
    throw err;
  } finally {
    setLoading(false);
  }
}

function badge(status) {
  return `<span class="badge badge-${status.toLowerCase()}">${status}</span>`;
}

function emptyMsg(text) {
  return `<p class="empty-msg">${text}</p>`;
}

// ══════════════════════════════════════════════════════════════════════
// NAVIGATION
// ══════════════════════════════════════════════════════════════════════

function showModule(moduleId) {
  document.querySelectorAll('.module').forEach(m => m.style.display = 'none');
  if (moduleId) {
    const el = document.getElementById(moduleId);
    if (el) el.style.display = 'block';
  }
  clearErrors();
}

function showSubmenu(name) {
  document.querySelectorAll('.submenu').forEach(s => s.style.display = 'none');
  const el = document.getElementById(name);
  if (el) el.style.display = 'block';
  clearErrors();
  
  if (name === 'schedule')    { loadDemoDates(); loadAdminSlots(); }
  if (name === 'teams')       { loadAllTeams(); loadAllBookings(); loadWaitingList(); }
  if (name === 'slotBooking') { loadAvailableSlots(); }
}

function openRegistrationForm() {
  document.getElementById('registrationForm').style.display = 'block';
  clearErrors();
}

// ══════════════════════════════════════════════════════════════════════
// STUDENT REGISTRATION
// ══════════════════════════════════════════════════════════════════════

async function submitRegistration(event) {
  event.preventDefault();
  const payload = {
    projectName: document.getElementById('projectName').value.trim(),
    members:     document.getElementById('members').value ? parseInt(document.getElementById('members').value) : null,
    leaderName:  document.getElementById('leaderName').value.trim(),
    email:       document.getElementById('email').value.trim(),
    description: document.getElementById('description').value.trim()
  };

  try {
    const team = await apiFetch(`${API}/students/register`, { method: 'POST', body: JSON.stringify(payload) });
    const msgDiv = document.getElementById('regMessage');
    msgDiv.innerHTML = `<div class="msg-success">Team "<strong>${team.projectName}</strong>" registered successfully! (Team ID: ${team.id})</div>`;
    showToast('Team registered successfully!', 'success');
    document.querySelector('#registrationForm form').reset();
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
    // Passing a custom flag or just wrapping manually so loader doesn't flash if we background refresh
    const dates = await apiFetch(`${API}/admin/dates`);
    const c = document.getElementById('datesContainer');
    if (!dates.length) { c.innerHTML = emptyMsg('No demo dates added yet.'); populateDateSelects([]); return; }
    c.innerHTML = `<table>
      <thead><tr><th>ID</th><th>Date</th><th>Action</th></tr></thead>
      <tbody>${dates.map(d => `<tr>
        <td>${d.id}</td><td>${d.demoDate}</td>
        <td><button onclick="deleteDemoDate(${d.id})" class="btn-danger" style="margin:0;padding:4px 8px">Delete</button></td>
      </tr>`).join('')}</tbody></table>`;
    populateDateSelects(dates);
  } catch (err) { /* silent fail on auto-load to avoid spam, or handle custom */ }
}

async function deleteDemoDate(id) {
  if (!confirm('Warning: Deleting this date removes ALL its slots and cancels related bookings. Proceed?')) return;
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
      sel.innerHTML += `<option value="${d.id}" ${prev == d.id ? 'selected' : ''}>${d.demoDate}</option>`;
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
    c.innerHTML = `<table>
      <thead><tr><th>ID</th><th>Date</th><th>Start</th><th>End</th><th>Status</th><th>Action</th></tr></thead>
      <tbody>${slots.map(s => `<tr>
        <td>${s.id}</td><td>${s.demoDate}</td><td>${s.startTime}</td><td>${s.endTime}</td>
        <td>${badge(s.status)}</td>
        <td><button onclick="deleteSlot(${s.id})" class="btn-danger" style="margin:0;padding:4px 8px">Delete</button></td>
      </tr>`).join('')}</tbody></table>`;
  } catch (err) {}
}

async function deleteSlot(id) {
  if (!confirm('Delete this slot? Related bookings will be cancelled.')) return;
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
    if (!teams.length) { c.innerHTML = emptyMsg('No teams registered yet.'); return; }
    c.innerHTML = `<table>
      <thead><tr><th>ID</th><th>Project</th><th>Leader</th><th>Email</th><th>Members</th></tr></thead>
      <tbody>${teams.map(t => `<tr>
        <td>${t.id}</td><td>${t.projectName}</td><td>${t.leaderName}</td><td>${t.email}</td><td>${t.members}</td>
      </tr>`).join('')}</tbody></table>`;
  } catch (err) {}
}

async function loadAllBookings() {
  try {
    const bookings = await apiFetch(`${API}/admin/bookings`);
    const c = document.getElementById('allBookingsContainer');
    if (!bookings.length) { c.innerHTML = emptyMsg('No bookings found.'); return; }
    c.innerHTML = `<table>
      <thead><tr><th>ID</th><th>Team</th><th>Date</th><th>Time</th><th>Status</th><th>Action</th></tr></thead>
      <tbody>${bookings.map(b => `<tr>
        <td>${b.id}</td><td>${b.teamProjectName}</td><td>${b.slotDate}</td>
        <td>${b.slotStartTime} – ${b.slotEndTime}</td><td>${badge(b.status)}</td>
        <td>${b.status !== 'CANCELLED' ? `<button onclick="adminCancelBookingById(${b.id})" class="btn-danger" style="margin:0;padding:4px 8px">Cancel</button>` : '—'}</td>
      </tr>`).join('')}</tbody></table>`;
  } catch (err) {}
}

async function adminCancelBookingById(id) {
  if (!confirm(`Cancel booking #${id}? Process will automatically promote waitlisted bookings if applicable.`)) return;
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
    if (!list.length) { c.innerHTML = emptyMsg('No bookings found in waiting list.'); return; }
    c.innerHTML = `<table>
      <thead><tr><th>Booking</th><th>Team</th><th>Date</th><th>Time</th></tr></thead>
      <tbody>${list.map(b => `<tr>
        <td>${b.id}</td><td>${b.teamProjectName} (${b.teamEmail})</td>
        <td>${b.slotDate}</td><td>${b.slotStartTime} – ${b.slotEndTime}</td>
      </tr>`).join('')}</tbody></table>`;
  } catch (err) {}
}

// ── ADMIN REPORTS ──────────────────────────────────────────────────────

async function loadFullSchedule() {
  try {
    const slots = await apiFetch(`${API}/admin/reports/schedule`);
    const c = document.getElementById('fullScheduleContainer');
    if (!slots.length) { c.innerHTML = emptyMsg('No schedule data found.'); return; }
    c.innerHTML = `<table>
      <thead><tr><th>ID</th><th>Date</th><th>Start</th><th>End</th><th>Status</th></tr></thead>
      <tbody>${slots.map(s => `<tr>
        <td>${s.id}</td><td>${s.demoDate}</td><td>${s.startTime}</td><td>${s.endTime}</td><td>${badge(s.status)}</td>
      </tr>`).join('')}</tbody></table>`;
  } catch (err) {}
}

async function loadBookingSummary() {
  try {
    const bookings = await apiFetch(`${API}/admin/reports/summary`);
    const c = document.getElementById('bookingSummaryContainer');
    if (!bookings.length) { c.innerHTML = emptyMsg('No bookings found.'); return; }
    c.innerHTML = `<table>
      <thead><tr><th>ID</th><th>Team</th><th>Leader</th><th>Date</th><th>Time</th><th>Status</th></tr></thead>
      <tbody>${bookings.map(b => `<tr>
        <td>${b.id}</td><td>${b.teamProjectName}</td><td>${b.teamLeaderName}</td>
        <td>${b.slotDate}</td><td>${b.slotStartTime}–${b.slotEndTime}</td><td>${badge(b.status)}</td>
      </tr>`).join('')}</tbody></table>`;
  } catch (err) {}
}

function exportCsv(type) {
  const url = type === 'schedule' ? `${API}/admin/reports/export/csv` : `${API}/admin/reports/export/teams-csv`;
  window.open(url, '_blank');
}

// ══════════════════════════════════════════════════════════════════════
// STUDENT MODULE
// ══════════════════════════════════════════════════════════════════════

function getEmailOrShowError() {
  const email = document.getElementById('studentEmail').value.trim();
  if (!email) {
    showError('studentEmail', 'Please enter your registered email to continue.');
    return null;
  }
  return email;
}

// ── STUDENT: AVAILABLE SLOTS & BOOK ────────────────────────────────────

async function loadAvailableSlots(forRescheduleBookingId = null) {
  try {
    // If reschedule is requested, we show ALL slots so they can see what's available
    const slots = await apiFetch(forRescheduleBookingId ? `${API}/bookings/all` : `${API}/bookings/available`);
    const c = document.getElementById('availableSlotsContainer');
    
    if (!slots.length) { 
      c.innerHTML = emptyMsg(forRescheduleBookingId ? 'No slots found.' : 'No slots available right now.'); 
      return; 
    }
    
    let tableHtml = `<table>
      <thead><tr><th>Date</th><th>Time</th><th>Status</th><th>Action</th></tr></thead><tbody>`;
      
    slots.forEach(s => {
      let actionBtn = '';
      if (forRescheduleBookingId) {
        actionBtn = `<button onclick="confirmReschedule(${forRescheduleBookingId}, ${s.id})" style="padding:4px 10px; background:#f39c12">Select New</button>`;
      } else {
        if (s.status === 'AVAILABLE' || s.status === 'BOOKED') {
          const btnClass = s.status === 'AVAILABLE' ? '' : 'style="background:#f39c12" title="You will be added to the waitlist"';
          const btnText  = s.status === 'AVAILABLE' ? 'Book Slot' : 'Join Waitlist';
          actionBtn = `<button onclick="bookSlot(${s.id})" ${btnClass}>${btnText}</button>`;
        } else {
          actionBtn = '—';
        }
      }
      
      tableHtml += `<tr>
        <td>${s.demoDate}</td><td>${s.startTime} – ${s.endTime}</td>
        <td>${badge(s.status)}</td>
        <td>${actionBtn}</td>
      </tr>`;
    });
    tableHtml += `</tbody></table>`;
    
    if (forRescheduleBookingId) {
      tableHtml = `<div style="background:#fff3cd; padding:10px; margin-bottom:10px; border-radius:5px; border:1px solid #ffeeba;">
          <strong>Reschedule Mode:</strong> Pick a new slot below for booking #${forRescheduleBookingId}.
          <button onclick="loadAvailableSlots()" class="btn-refresh" style="margin-left:10px">Cancel Reschedule</button>
        </div>` + tableHtml;
    }
    
    c.innerHTML = tableHtml;
  } catch (err) { handleApiErrors(err); }
}

async function bookSlot(slotId) {
  const email = getEmailOrShowError();
  if (!email) return;

  try {
    const team = await apiFetch(`${API}/students/${encodeURIComponent(email)}/team`);
    const booking = await apiFetch(`${API}/bookings`, {
      method: 'POST',
      body: JSON.stringify({ teamId: team.id, slotId: slotId })
    });
    showToast(`Slot booked! Status: ${booking.status}. Booking ID: ${booking.id}`, 'success');
    await loadAvailableSlots(); // Re-render available slots immediately
  } catch (err) { handleApiErrors(err); }
}

// ── STUDENT: MY TEAM & BOOKINGS ────────────────────────────────────────

async function loadMyTeam() {
  const email = getEmailOrShowError();
  if (!email) return;
  try {
    const t = await apiFetch(`${API}/students/${encodeURIComponent(email)}/team`);
    document.getElementById('myTeamContainer').innerHTML = `<table>
      <thead><tr><th>Field</th><th>Value</th></tr></thead>
      <tbody>
        <tr><td><strong>Team ID</strong></td><td>${t.id}</td></tr>
        <tr><td><strong>Project</strong></td><td>${t.projectName}</td></tr>
        <tr><td><strong>Leader</strong></td><td>${t.leaderName}</td></tr>
        <tr><td><strong>Members</strong></td><td>${t.members}</td></tr>
      </tbody></table>`;
  } catch (err) { handleApiErrors(err); }
}

async function loadMyBookings() {
  const email = getEmailOrShowError();
  if (!email) return;
  try {
    const bookings = await apiFetch(`${API}/students/${encodeURIComponent(email)}/bookings`);
    const c = document.getElementById('myBookingsContainer');
    if (!bookings.length) { c.innerHTML = emptyMsg('No bookings found for this email.'); return; }
    
    let tableHtml = `<table>
      <thead><tr><th>ID</th><th>Date</th><th>Time</th><th>Status</th><th>Actions</th></tr></thead><tbody>`;
      
    bookings.forEach(b => {
      let actions = '';
      if (b.status !== 'CANCELLED') {
        actions = `<button onclick="startReschedule(${b.id})" style="background:#f39c12; margin-right:4px">Reschedule</button>
                   <button onclick="cancelMyBooking(${b.id})" class="btn-danger">Cancel</button>`;
      } else {
        actions = '—';
      }
      tableHtml += `<tr>
        <td>${b.id}</td><td>${b.slotDate}</td><td>${b.slotStartTime} – ${b.slotEndTime}</td>
        <td>${badge(b.status)}</td>
        <td style="white-space:nowrap">${actions}</td>
      </tr>`;
    });
    tableHtml += `</tbody></table>`;
    c.innerHTML = tableHtml;
  } catch (err) { handleApiErrors(err); }
}

// ── STUDENT: ACTIONS ───────────────────────────────────────────────────

async function cancelMyBooking(bookingId) {
  if (!confirm(`Are you sure you want to cancel booking #${bookingId}?`)) return;
  try {
    await apiFetch(`${API}/bookings/${bookingId}`, { method: 'DELETE' });
    showToast('Your booking was successfully cancelled.', 'success');
    await loadMyBookings();
    
    // Also refresh the available slots container if it's currently open
    if (document.getElementById('slotBooking').style.display === 'block') {
       await loadAvailableSlots();
    }
  } catch (err) { handleApiErrors(err); }
}

function startReschedule(bookingId) {
  // Jump to the slot selection tab in Reschedule Mode
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
    showToast(`Successfully rescheduled! Status: ${booking.status}`, 'success');
    // Exit reschedule mode and show normal slots
    await loadAvailableSlots();
  } catch (err) { handleApiErrors(err); }
}

// ── STUDENT: SCHEDULE ──────────────────────────────────────────────────

async function loadPublicSchedule() {
  try {
    const slots = await apiFetch(`${API}/bookings/all`);
    const c = document.getElementById('publicScheduleContainer');
    if (!slots.length) { c.innerHTML = emptyMsg('No schedule data found.'); return; }
    c.innerHTML = `<table>
      <thead><tr><th>Date</th><th>Start</th><th>End</th><th>Status</th></tr></thead>
      <tbody>${slots.map(s => `<tr>
        <td>${s.demoDate}</td><td>${s.startTime}</td><td>${s.endTime}</td><td>${badge(s.status)}</td>
      </tr>`).join('')}</tbody></table>`;
  } catch (err) { handleApiErrors(err); }
}
