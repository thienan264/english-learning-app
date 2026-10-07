const avatarInput = document.getElementById('avatar');
if (avatarInput) {
    const preview = document.getElementById('avatarPreview');
    const fallback = document.getElementById('avatarFallback');
    const filename = document.getElementById('avatarFilename');
    let previewUrl;
    avatarInput.addEventListener('change', () => {
        const file = avatarInput.files[0];
        if (!file) return;
        if (!['image/png', 'image/jpeg'].includes(file.type) || file.size > 5 * 1024 * 1024) {
            filename.textContent = 'Chọn ảnh JPG hoặc PNG, tối đa 5 MB.';
            avatarInput.value = '';
            return;
        }
        if (previewUrl) URL.revokeObjectURL(previewUrl);
        previewUrl = URL.createObjectURL(file);
        preview.src = previewUrl;
        preview.hidden = false;
        fallback.hidden = true;
        filename.textContent = file.name + ' · Nhấn Lưu thay đổi để cập nhật.';
        const removal = document.getElementById('removeAvatar');
        if (removal) removal.checked = false;
    });
    window.addEventListener('pagehide', () => { if (previewUrl) URL.revokeObjectURL(previewUrl); });
}
const filters = document.querySelectorAll('[data-notification-filter]');
filters.forEach(button => button.addEventListener('click', () => {
    const unreadOnly = button.dataset.notificationFilter === 'unread';
    filters.forEach(filter => {
        const active = filter === button;
        filter.classList.toggle('active', active);
        filter.setAttribute('aria-pressed', String(active));
    });
    const items = document.querySelectorAll('.notification-item');
    let visible = 0;
    items.forEach(item => {
        item.hidden = unreadOnly && item.dataset.unread !== 'true';
        if (!item.hidden) visible++;
    });
    const empty = document.getElementById('noUnreadNotifications');
    if (empty) empty.hidden = !(unreadOnly && visible === 0);
    const originalEmpty = document.querySelector('.notification-empty:not(#noUnreadNotifications)');
    if (originalEmpty) originalEmpty.hidden = unreadOnly;
}));
