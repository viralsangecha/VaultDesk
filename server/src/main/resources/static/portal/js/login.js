if (getToken()) window.location.href = '/portal/index.html';

const statusMsg = document.getElementById('statusMsg');
const loginBtn = document.getElementById('loginBtn');

async function doLogin() {
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    if (!username || !password) {
        statusMsg.textContent = 'Please enter username and password.';
        return;
    }
    try {
        const resp = await fetch(API_BASE + '/api/employee/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await resp.json();
        if (resp.status === 200 && data.success) {
            saveSession(data.token, {
                employeeId: data.employeeId,
                name: data.name,
                empCode: data.empCode,
                designation: data.designation,
                departmentId: data.departmentId,
                email: data.email
            });
            window.location.href = '/portal/index.html';
        } else {
            statusMsg.textContent = 'Invalid username or password.';
        }
    } catch (ex) {
        statusMsg.textContent = 'Cannot connect to server.';
    }
}
document.getElementById('forgotLink').addEventListener('click', async (e) => {
    e.preventDefault();
    const username = prompt('Enter your username to receive a reset link by email:');
    if (!username) return;
    try {
        const resp = await fetch(API_BASE + '/api/forgot-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, userType: 'EMPLOYEE' })
        });
        const data = await resp.json();
        alert(data.message || 'Something went wrong.');
    } catch (ex) {
        alert('Cannot connect to server.');
    }
});

loginBtn.addEventListener('click', doLogin);
document.getElementById('password').addEventListener('keydown', e => { if (e.key === 'Enter') doLogin(); });
document.getElementById('username').addEventListener('keydown', e => { if (e.key === 'Enter') doLogin(); });