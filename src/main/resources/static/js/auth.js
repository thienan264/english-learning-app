document.querySelectorAll('[data-password]').forEach(button => {
    button.addEventListener('click', () => {
        const input = document.getElementById(button.dataset.password);
        const visible = input.type === 'password';
        input.type = visible ? 'text' : 'password';
        button.textContent = visible ? 'Ẩn' : 'Hiện';
        button.setAttribute('aria-pressed', String(visible));
        button.setAttribute('aria-label', visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
    });
});
const form = document.getElementById('registerForm');
if (form) {
    const email = document.getElementById('email');
    const code = document.getElementById('verificationCode');
    const button = document.getElementById('sendCode');
    const status = document.getElementById('codeStatus');
    let availableAt = 0;
    let sending = false;
    function updateButton() {
        const remaining = Math.max(0, Math.ceil((availableAt - Date.now()) / 1000));
        button.disabled = sending || remaining > 0;
        button.textContent = sending ? 'Đang gửi…' : remaining ? `Gửi lại (${remaining}s)` : 'Gửi mã';
    }
    setInterval(updateButton, 1000);
    email.addEventListener('input', () => {
        code.value = '';
        status.textContent = 'Email đã thay đổi. Hãy gửi mã xác nhận tới địa chỉ mới.';
        status.classList.remove('error');
    });
    button.addEventListener('click', async () => {
        if (!email.reportValidity()) return;
        sending = true;
        updateButton();
        status.textContent = '';
        status.classList.remove('error');
        try {
            const params = new URLSearchParams({email: email.value});
            const csrf = form.querySelector('input[name="_csrf"]');
            if (csrf) params.set(csrf.name, csrf.value);
            const response = await fetch(form.dataset.sendCodeUrl, {
                method: 'POST', credentials: 'same-origin',
                headers: {'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json'},
                body: params
            });
            const data = await response.json();
            status.textContent = data.message || 'Không gửi được mã. Vui lòng thử lại.';
            status.classList.toggle('error', !response.ok);
            if (response.ok) {
                availableAt = Date.now() + (data.retryAfter || 60) * 1000;
                code.value = '';
                code.focus();
            }
        } catch (error) {
            status.classList.add('error');
            status.textContent = 'Không kết nối được máy chủ. Tải lại trang và thử lại.';
        } finally {
            sending = false;
            updateButton();
        }
    });
    const password = document.getElementById('password');
    const confirmation = document.getElementById('confirmPassword');
    function checkPasswords() {
        confirmation.setCustomValidity(confirmation.value && confirmation.value !== password.value ? 'Mật khẩu xác nhận không khớp.' : '');
    }
    password.addEventListener('input', checkPasswords);
    confirmation.addEventListener('input', checkPasswords);
}
