requireLogin();
const profile = getProfile();
document.getElementById('sbName').textContent = profile.name;
document.getElementById('sbDesig').textContent = profile.designation + ' • ' + profile.empCode;

const content = document.getElementById('content');
const navButtons = document.querySelectorAll('.nav-btn[data-view]');

function setActive(view) {
    navButtons.forEach(b => b.classList.toggle('active', b.dataset.view === view));
}

navButtons.forEach(btn => {
    btn.addEventListener('click', () => {
        setActive(btn.dataset.view);
        renderView(btn.dataset.view);
    });
});

document.getElementById('logoutBtn').addEventListener('click', () => {
    clearSession();
    window.location.href = '/portal/login.html';
});

function esc(s) {
    if (s == null) return '';
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function statusBadgeClass(status) {
    return { 'Open': 'badge-open', 'In Progress': 'badge-progress', 'Resolved': 'badge-resolved', 'Closed': 'badge-closed' }[status] || 'badge-closed';
}
function priorityBadgeClass(p) {
    return { 'Critical': 'badge-critical', 'High': 'badge-high', 'Medium': 'badge-medium', 'Low': 'badge-low' }[p] || 'badge-medium';
}

function renderView(view) {
    if (view === 'home') return renderHome();
    if (view === 'tickets') return renderTickets();
    if (view === 'raise') return renderRaiseTicket();
    if (view === 'pending') return renderPending();
    if (view === 'assets') return renderAssets();
    if (view === 'settings') return renderSettings();
}

// ── Dashboard home ──────────────────────────────────
function renderHome() {
     content.innerHTML = `
         <div class="page-title">Welcome, ${esc(profile.name)} 👋</div>
         <div class="page-sub">${esc(profile.designation)} • ${esc(profile.empCode)}</div>
         <div class="cards-row">
             <div class="home-card glass-panel" id="homeTickets" style="border-left:3px solid #2980b9">
                 <div class="icon" style="color:#2980b9">✉</div>
                 <div class="h">My Tickets</div><div class="s">View and track your support requests</div>
             </div>
             <div class="home-card glass-panel" id="homeRaise" style="border-left:3px solid #27ae60">
                 <div class="icon" style="color:#27ae60">＋</div>
                 <div class="h">Raise Ticket</div><div class="s">Submit a new support request</div>
             </div>
             <div class="home-card glass-panel" id="homeAssets" style="border-left:3px solid #e67e22">
                 <div class="icon" style="color:#e67e22">▣</div>
                 <div class="h">My Assets</div><div class="s">View assets assigned to you</div>
             </div>
         </div>
         <div class="page-title" style="font-size:15px">Your Information</div>
         <div class="card glass-panel">
             <div class="info-row"><span class="k">Name:</span><span class="v">${esc(profile.name)}</span></div>
             <div class="info-row"><span class="k">Employee Code:</span><span class="v">${esc(profile.empCode)}</span></div>
             <div class="info-row"><span class="k">Designation:</span><span class="v">${esc(profile.designation)}</span></div>
             <div class="info-row"><span class="k">Email:</span><span class="v">${esc(profile.email)}</span></div>
         </div>`;
     document.getElementById('homeTickets').onclick = () => { setActive('tickets'); renderTickets(); };
     document.getElementById('homeRaise').onclick = () => { setActive('raise'); renderRaiseTicket(); };
     document.getElementById('homeAssets').onclick = () => { setActive('assets'); renderAssets(); };
 }

// ── My Tickets ───────────────────────────────────────
async function renderTickets() {
    content.innerHTML = `
        <div class="page-title">My Tickets</div>
        <div class="page-sub">All support requests you have raised.</div>
        <div style="display:flex; gap:16px;">
            <div style="flex:1;"><table id="ticketsTable"><thead><tr>
                <th>Ticket No</th><th>Title</th><th>Category</th><th>Priority</th><th>Status</th><th>Created</th>
            </tr></thead><tbody><tr><td colspan="6">Loading...</td></tr></tbody></table></div>
            <div id="ticketDetail" style="width:360px; flex-shrink:0;"></div>
        </div>`;
    const resp = await api.get('/api/employee/tickets/' + profile.employeeId);
    const tickets = await resp.json();
    const tbody = document.querySelector('#ticketsTable tbody');
    if (!tickets.length) {
        tbody.innerHTML = '<tr><td colspan="6"><div class="empty-state">No tickets yet. Raise your first ticket from the sidebar.</div></td></tr>';
        return;
    }
    tbody.innerHTML = tickets.map(t => `
        <tr class="clickable" data-id="${t.id}">
            <td>${esc(t.ticketNo)}</td><td>${esc(t.title)}</td><td>${esc(t.category)}</td>
            <td><span class="badge ${priorityBadgeClass(t.priority)}">${esc(t.priority)}</span></td>
            <td><span class="badge ${statusBadgeClass(t.status)}">${esc(t.status)}</span></td>
            <td>${esc((t.createdAt || '').substring(0, 10))}</td>
        </tr>`).join('');
    tbody.querySelectorAll('tr[data-id]').forEach(row => {
        row.addEventListener('click', () => {
            const t = tickets.find(x => x.id == row.dataset.id);
            openTicketDetail(t);
        });
    });
}

async function openTicketDetail(t) {
    const panel = document.getElementById('ticketDetail');
    panel.innerHTML = `
        <div class="card glass-panel">
            <div style="display:flex; justify-content:space-between; align-items:center;">
                <b style="color:#58a6ff">${esc(t.ticketNo)}</b>
                <button class="btn-ghost" id="closeDetail">✕</button>
            </div>
            <div class="page-title" style="font-size:14px; margin-top:8px;">${esc(t.title)}</div>
            <div style="margin:8px 0;">
                <span class="badge ${statusBadgeClass(t.status)}">${esc(t.status)}</span>
                <span class="badge ${priorityBadgeClass(t.priority)}">${esc(t.priority)}</span>
            </div>
            <div class="info-row"><span class="k">Category:</span><span class="v">${esc(t.category)}</span></div>
            <div class="info-row"><span class="k">Created:</span><span class="v">${esc((t.createdAt||'').substring(0,10))}</span></div>
            <div style="margin-top:10px; font-size:12px; color:#8b949e; font-weight:bold;">Description</div>
            <div style="font-size:12px; margin-top:4px;">${esc(t.description || 'No description provided.')}</div>
            ${t.resolution ? `<div class="card solid-panel" style="background:rgba(234,250,241,0.9); border-left:3px solid #27ae60; margin-top:10px;">
                            <div style="color:#27ae60; font-size:11px; font-weight:bold;">Resolution</div>
                            <div style="font-size:12px; margin-top:4px;">${esc(t.resolution)}</div></div>` : ''}
            <hr style="border-color:#30363d; margin:14px 0;">
            <div style="font-weight:bold; color:#e6edf3; font-size:13px;">Comments</div>
            <div id="commentsFeed" style="max-height:200px; overflow-y:auto; margin:10px 0;"></div>
            <textarea id="commentInput" rows="2" placeholder="Write a comment..."></textarea>
            <button class="btn-green" id="postCommentBtn" style="margin-top:8px;">Post Comment</button>
        </div>`;
    document.getElementById('closeDetail').onclick = () => panel.innerHTML = '';
    await loadComments(t.id);
    document.getElementById('postCommentBtn').onclick = async () => {
        const text = document.getElementById('commentInput').value.trim();
        if (!text) return;
        await api.post('/api/tickets/' + t.id + '/comments', { comment: text, addedBy: String(profile.employeeId) });
        document.getElementById('commentInput').value = '';
        await loadComments(t.id);
    };
}

async function loadComments(ticketId) {
    const feed = document.getElementById('commentsFeed');
    feed.innerHTML = 'Loading...';
    const resp = await api.get('/api/tickets/' + ticketId + '/comments');
    const comments = await resp.json();
    if (!comments.length) { feed.innerHTML = '<div style="color:#484f58; font-size:12px;">No comments yet.</div>'; return; }
    feed.innerHTML = comments.map(c => `
        <div class="comment-bubble">
            <div class="name">${esc(c.addedByName || 'Unknown')}</div>
            <div>${esc(c.comment)}</div>
            <div class="time">${esc((c.addedAt||'').substring(0,16))}</div>
        </div>`).join('');
}

// ── Raise Ticket ─────────────────────────────────────
async function renderRaiseTicket() {
    content.innerHTML = `
        <center>
        <div class="card glass-panel" style="max-width:540px;">
            <label>Title *</label><input type="text" id="rtTitle" placeholder="Brief description of the issue (min 5 chars)">
            <label>Description *</label><textarea id="rtDesc" rows="5" placeholder="Describe the issue in detail... (min 10 chars)"></textarea>
            <label>Category *</label>
            <select id="rtCategory">
                ${['Hardware','Software','SAP','CCTV','Printer','Drive','Zoho Mail','Adobe acrobat','IVMS','Network','General','other']
                    .map(c => `<option ${c==='Hardware'?'selected':''}>${c}</option>`).join('')}
            </select>
            <label>Priority *</label>
            <select id="rtPriority">
                ${['Low','Medium','High','Critical'].map(p => `<option ${p==='Medium'?'selected':''}>${p}</option>`).join('')}
            </select>
            <label>Which asset has the issue?</label>
            <select id="rtAsset"><option value="0">Loading your assets...</option></select>

            <!-- Error container -->
            <div class="error-text" id="rtError" style="color: red; margin-top: 8px; min-height: 20px; font-weight: bold;"></div>

            <!-- Removed the 'disabled' attribute so it can always be clicked -->
            <button class="btn-green" id="rtSubmit" style="margin-top:12px;">Submit Ticket</button>
        </div></center>`;

    const titleEl = document.getElementById('rtTitle');
    const descEl = document.getElementById('rtDesc');
    const submitBtn = document.getElementById('rtSubmit');
    const errorEl = document.getElementById('rtError');

    // Clear the error message automatically when the user starts typing
    const clearError = () => errorEl.textContent = '';
    titleEl.addEventListener('input', clearError);
    descEl.addEventListener('input', clearError);

    const assetSelect = document.getElementById('rtAsset');
    try {
        const resp = await api.get('/api/tickets/api/employees/' + profile.employeeId + '/ticket-defaults');
        const data = await resp.json();
        const opts = ['<option value="0">No Asset / Not Applicable</option>'];
        (data.assets || []).forEach(a => {
            const label = `${a.assetTag} — ${a.name}` + (a.accessoryCount > 0 ? ` (+${a.accessoryCount} accessories)` : '');
            opts.push(`<option value="${a.id}">${esc(label)}</option>`);
        });
        assetSelect.innerHTML = opts.join('');
    } catch (ex) {
        assetSelect.innerHTML = '<option value="0">No Asset / Not Applicable</option>';
    }

    submitBtn.onclick = async () => {
        const title = titleEl.value.trim();
        const desc = descEl.value.trim();

        // 1. Validate Empty Fields
        if (!title) {
            errorEl.textContent = 'Error: Please enter a Title.';
            titleEl.focus(); // Brings the cursor to the title box
            return;
        }
        if (!desc) {
            errorEl.textContent = 'Error: Please enter a Description.';
            descEl.focus(); // Brings the cursor to the description box
            return;
        }

        // 2. Validate Minimum Length
        if (title.length < 5) {
            errorEl.textContent = 'Error: Title must be at least 5 characters long.';
            titleEl.focus();
            return;
        }
        if (desc.length < 10) {
            errorEl.textContent = 'Error: Description must be at least 10 characters long.';
            descEl.focus();
            return;
        }

        // Passed validation, build the body
        const body = {
            title: title,
            description: desc,
            category: document.getElementById('rtCategory').value,
            priority: document.getElementById('rtPriority').value,
            reportedBy: profile.employeeId,
            assetId: parseInt(assetSelect.value, 10)
        };

        // Prevent double clicking by temporarily disabling it during the API call
        submitBtn.disabled = true;
        submitBtn.textContent = 'Submitting...';
        errorEl.textContent = ''; // clear any existing errors

        try {
            const resp = await api.post('/api/employee/tickets', body);
            if (resp.status === 201) {
                const data = await resp.json().catch(() => ({}));
                showTicketCreatedStub(data.ticketNo || 'Submitted', body.title, body.category, body.priority);
            } else {
                errorEl.textContent = 'Server error: ' + resp.status;
                submitBtn.disabled = false;
                submitBtn.textContent = 'Submit Ticket';
            }
        } catch (ex) {
            errorEl.textContent = 'Cannot connect to server.';
            submitBtn.disabled = false;
            submitBtn.textContent = 'Submit Ticket';
        }
    };
}

function showTicketCreatedStub(ticketNo, title, category, priority) {
    const backdrop = document.createElement('div');
    backdrop.className = 'ticket-modal-backdrop';
    const today = new Date().toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' });

    backdrop.innerHTML = `
        <div class="ticket-stub">
            <div class="t-watermark"></div>
            <div class="t-watermark">&nbsp;&nbsp;&nbsp;&nbsp;🐘</div>
            <div class="t-watermark"></div>
            <div class="t-header">TICKET CREATED<div class="t-symbol">✁</div></div>
            <div class="t-body">
            <br>
                <em>${esc(title)}</em><br>
                ${today}<br>
                ${esc(category)} · Priority: ${esc(priority)}
            </div>
            <div class="t-footer">
                <div class="t-number">Ref <span class="bold">${esc(ticketNo)}</span></div>
                <div class="barcode"></div>
            </div>
            <div class="bg holographic"></div>
        </div>
        <svg class="filter">
            <filter id="ticket-bump">
                <feTurbulence result="noise" numOctaves="3" baseFrequency="0.7" type="fractalNoise"></feTurbulence>
                <feSpecularLighting in="noise" result="specular" lighting-color="#fffffc" specularExponent="25" specularConstant="0.8" surfaceScale="0.15">
                    <fePointLight z="210" y="100" x="100"></fePointLight>
                </feSpecularLighting>
                <feComposite result="noise2" operator="in" in="specular" in2="SourceGraphic"></feComposite>
                <feBlend mode="screen" in2="noise2" in="SourceGraphic"></feBlend>
            </filter>
        </svg>
         <button class="ticket-modal-close">our IT team will connect shortly </button>
    `;
    document.body.appendChild(backdrop);
   backdrop.addEventListener('click', (e) => {
       if (e.target === backdrop) {
           backdrop.remove();
           setActive('tickets');
           renderTickets();
       }
   });
}
// ── Pending Approval ─────────────────────────────────
async function renderPending() {
    content.innerHTML = `
        <div class="page-title">Pending Approval</div>
        <div class="page-sub">Tickets waiting for your approval to close.</div>
        <table><thead><tr><th>Ticket No</th><th>Title</th><th>Resolved At</th><th>Solution</th><th>Action</th></tr></thead>
        <tbody id="pendingBody"><tr><td colspan="5">Loading...</td></tr></tbody></table>`;
    const resp = await api.get('/api/tickets/pending-closure/reporter/' + profile.employeeId);
    const rows = await resp.json();
    const tbody = document.getElementById('pendingBody');
    if (!rows.length) {
        tbody.innerHTML = '<tr><td colspan="5"><div class="empty-state">✔ No pending approvals — all your tickets are up to date.</div></td></tr>';
        return;
    }
    tbody.innerHTML = rows.map(t => `
        <tr>
            <td>${esc(t.ticketNo)}</td><td>${esc(t.title)}</td>
            <td>${esc((t.resolvedAt||'').substring(0,10))}</td><td>${esc(t.resolution)}</td>
            <td>
                <button class="btn-green" data-approve="${t.id}" style="margin-right:6px;">✔ Approve</button>
                <button class="btn-danger" data-deny="${t.id}">✘ Deny</button>
            </td>
        </tr>`).join('');
    tbody.querySelectorAll('[data-approve]').forEach(btn => btn.onclick = () => approveClosure(btn.dataset.approve));
    tbody.querySelectorAll('[data-deny]').forEach(btn => btn.onclick = () => showDenyModal(btn.dataset.deny));
}

async function approveClosure(ticketId) {
    await api.put('/api/tickets/' + ticketId + '/approve-closure');
    alert('Ticket has been closed. Thank you.');
    renderPending();
}

function showDenyModal(ticketId) {
    const backdrop = document.createElement('div');
    backdrop.className = 'modal-backdrop';
    backdrop.innerHTML = `
        <div class="modal glass-panel">
            <h3>Deny Closure</h3>
            <p style="font-size:13px; color:#8b949e;">Why is this ticket not resolved?</p>
            <textarea id="denyReason" rows="4" placeholder="Describe what is still not working..."></textarea>
            <div class="error-text" id="denyError"></div>
            <div class="modal-actions">
                <button class="btn-ghost" id="denyCancel">Cancel</button>
                <button class="btn-primary" id="denyOk">OK</button>
            </div>
        </div>`;
    document.body.appendChild(backdrop);
    document.getElementById('denyCancel').onclick = () => backdrop.remove();
    document.getElementById('denyOk').onclick = async () => {
        const reason = document.getElementById('denyReason').value.trim();
        if (!reason) { document.getElementById('denyError').textContent = 'Please write a reason.'; return; }
        await api.put('/api/tickets/' + ticketId + '/deny-closure', { reason });
        backdrop.remove();
        renderPending();
    };
}

// ── My Assets ────────────────────────────────────────
async function renderAssets() {
    content.innerHTML = `
        <div class="page-title">My Assets</div>
        <div class="page-sub">Assets currently assigned to you.</div>
        <table><thead><tr><th>Asset Tag</th><th>Name</th><th>Category</th><th>Brand</th><th>Status</th><th>Location</th></tr></thead>
        <tbody id="assetsBody"><tr><td colspan="6">Loading...</td></tr></tbody></table>`;
    const resp = await api.get('/api/employee/assets/' + profile.employeeId);
    const assets = await resp.json();
    const tbody = document.getElementById('assetsBody');
    if (!assets.length) {
        tbody.innerHTML = '<tr><td colspan="6"><div class="empty-state">No assets assigned. Contact IT to assign assets to you.</div></td></tr>';
        return;
    }
    const statusClass = s => ({ 'Active':'badge-resolved','In Repair':'badge-high','Retired':'badge-closed' }[s] || 'badge-closed');
    tbody.innerHTML = assets.map(a => `
        <tr><td>${esc(a.assetTag)}</td><td>${esc(a.name)}</td><td>${esc(a.category)}</td>
        <td>${esc(a.brand)}</td><td><span class="badge ${statusClass(a.status)}">${esc(a.status)}</span></td>
        <td>${esc(a.location)}</td></tr>`).join('');
}

// ── Settings ─────────────────────────────────────────
function renderSettings() {
    content.innerHTML = `
        <div class="page-title">Settings</div>
        <div class="page-sub">Change your password.</div>
        <div class="card glass-panel" style="max-width:420px;">
            <label>Current Password</label><input type="password" id="curPwd">
            <label>New Password</label><input type="password" id="newPwd" placeholder="Minimum 6 characters">
            <label>Confirm New Password</label><input type="password" id="confirmPwd">
            <div id="pwdStatus" class="error-text"></div>
            <button class="btn-warn" id="changePwdBtn" style="margin-top:12px;">Change Password</button>
        </div>`;
    document.getElementById('changePwdBtn').onclick = async () => {
        const cur = document.getElementById('curPwd').value;
        const nw = document.getElementById('newPwd').value;
        const confirm = document.getElementById('confirmPwd').value;
        const status = document.getElementById('pwdStatus');
        status.className = 'error-text';
        if (!cur || !nw) { status.textContent = 'All fields required.'; return; }
        if (nw.length < 6) { status.textContent = 'Password must be at least 6 characters.'; return; }
        if (nw !== confirm) { status.textContent = 'Passwords do not match.'; return; }
        const resp = await api.put('/api/employee/auth/password/' + profile.employeeId,
            { currentPassword: cur, newPassword: nw });
        if (resp.status === 200) {
            status.className = 'success-text';
            status.textContent = '✔ Password changed successfully.';
            document.getElementById('curPwd').value = '';
            document.getElementById('newPwd').value = '';
            document.getElementById('confirmPwd').value = '';
        } else if (resp.status === 401) {
            status.textContent = '✘ Current password incorrect.';
        } else {
            status.textContent = 'Error: ' + resp.status;
        }
    };
}

// ── Notifications ────────────────────────────────────
const bellIcon = document.getElementById('bellIcon');
const bellCount = document.getElementById('bellCount');
const notifDropdown = document.getElementById('notifDropdown');

async function pollUnread() {
    try {
        const resp = await api.get('/api/notifications/employee/' + profile.employeeId + '/unread');
        const data = await resp.json();
        if (data.count > 0) {
            bellCount.textContent = data.count > 99 ? '99+' : data.count;
            bellCount.classList.remove('hidden');
        } else {
            bellCount.classList.add('hidden');
        }
    } catch (ex) { /* ignore poll errors */ }
}
pollUnread();
setInterval(pollUnread, 60000);

bellIcon.addEventListener('click', async () => {
    if (!notifDropdown.classList.contains('hidden')) {
        notifDropdown.classList.add('hidden');
        return;
    }
    notifDropdown.innerHTML = '<div style="padding:16px;">Loading...</div>';
    notifDropdown.classList.remove('hidden');
    const resp = await api.get('/api/notifications/employee/' + profile.employeeId);
    const list = await resp.json();
    if (!list.length) {
        notifDropdown.innerHTML = '<div style="padding:20px; color:#484f58; font-size:12px;">No notifications yet.</div>';
        return;
    }
    notifDropdown.innerHTML = list.map(n => `
        <div class="notif-row ${n.isRead === 0 ? 'unread' : ''}" data-id="${n.id}">
            <div>${n.isRead === 0 ? '🔵' : '⚪'} ${esc(n.message)}</div>
            <div class="time">${esc((n.createdAt||'').substring(0,16))}</div>
        </div>`).join('');
    notifDropdown.querySelectorAll('.notif-row').forEach(row => {
        row.addEventListener('click', async () => {
            await api.put('/api/notifications/' + row.dataset.id + '/read');
            notifDropdown.classList.add('hidden');
            pollUnread();
        });
    });
});

// ── Initial render ───────────────────────────────────
renderHome();