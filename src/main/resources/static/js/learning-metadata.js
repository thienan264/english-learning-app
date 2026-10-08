(() => {
'use strict';
document.querySelectorAll('.learning-metadata-form').forEach(form => {
    let saving = false;
    const button = form.querySelector('button[type="submit"]');
    const status = form.querySelector('.metadata-save-status');
    form.addEventListener('input', () => {
        if (!saving) { status.textContent = 'Có thay đổi chưa lưu'; status.className = 'metadata-save-status small text-muted'; }
    });
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (saving || !form.reportValidity()) return;
        const data = new FormData(form);
        const controls = [...form.querySelectorAll('button,select,textarea')];
        saving = true;
        controls.forEach(control => control.disabled = true);
        button.textContent = 'Đang lưu…';
        status.textContent = 'Đang lưu mục tiêu';
        status.className = 'metadata-save-status small text-muted';
        try {
            const response = await fetch(form.action + '/inline', {method:'POST', body:data, headers:{Accept:'application/json'}});
            if (response.redirected || response.status === 401 || response.status === 403) throw new Error('Phiên đăng nhập có thể đã hết. Nội dung vẫn được giữ; hãy đăng nhập lại để lưu.');
            if (!(response.headers.get('Content-Type') || '').includes('application/json')) throw new Error('Chưa lưu được. Nội dung vẫn được giữ để bạn thử lại.');
            const result = await response.json();
            if (!response.ok) throw new Error(result.message || 'Chưa lưu được. Hãy thử lại.');
            status.textContent = '✓ Đã lưu mục tiêu';
            status.className = 'metadata-save-status small text-success';
        } catch (error) {
            status.textContent = error.message || 'Chưa lưu được. Hãy thử lại.';
            status.className = 'metadata-save-status small text-danger';
        } finally {
            controls.forEach(control => control.disabled = false);
            button.textContent = 'Lưu mục tiêu';
            saving = false;
        }
    });
});
})();
