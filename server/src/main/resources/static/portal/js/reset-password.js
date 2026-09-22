const params = new URLSearchParams(window.location.search);
const token = params.get('token');
const statusMsg = document.getElementById('statusMsg');

if (!token) {
    statusMsg.textContent = 'This reset link is missing its token. Please use the link from your email.';
    document.getElementById('resetBtn').disabled = true;
}

document.getElementById('resetBtn').addEventListener('click', async () => {
    const newPwd = document.getElementById('newPwd').value;
    const confirmPwd = document.getElementById('confirmPwd').value;

    if (!newPwd || newPwd.length < 6) {
        statusMsg.className = 'error-text';
        statusMsg.textContent = 'Password must be at least 6 characters.';
        return;
    }
    if (newPwd !== confirmPwd) {
        statusMsg.className = 'error-text';
        statusMsg.textContent = 'Passwords do not match.';
        return;
    }

    try {
        const resp = await fetch(API_BASE + '/api/reset-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ token, newPassword: newPwd })
        });
        const data = await resp.json();
       if (resp.status === 200 && data.success) {
           statusMsg.className = 'success-text';
           document.getElementById('resetBtn').disabled = true;
           document.getElementById('newPwd').disabled = true;
           document.getElementById('confirmPwd').disabled = true;
           document.getElementById('successAnim').classList.remove('hidden');

           let secondsLeft = 10;
           statusMsg.textContent = '✔ ' + data.message + ' Redirecting to login in ' + secondsLeft + 's...';
           const countdown = setInterval(() => {
               secondsLeft--;
               if (secondsLeft <= 0) {
                   clearInterval(countdown);
                   window.location.href = '/portal/login.html';
               } else {
                   statusMsg.textContent = '✔ ' + data.message + ' Redirecting to login in ' + secondsLeft + 's...';
               }
           }, 1000);
       } else {
            statusMsg.className = 'error-text';
            statusMsg.textContent = data.message || 'Something went wrong.';
        }
    } catch (ex) {
        statusMsg.className = 'error-text';
        statusMsg.textContent = 'Cannot connect to server.';
    }
});