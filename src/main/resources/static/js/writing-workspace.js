document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-essay-target]').forEach(button => {
        const essay = document.getElementById(button.dataset.essayTarget);
        if (!essay) return;
        let originalHeight;
        button.addEventListener('click', () => {
            const expand = !essay.classList.contains('is-expanded');
            if (expand) {
                originalHeight = essay.style.height;
                essay.style.height = '';
            } else {
                essay.style.height = originalHeight || '';
            }
            essay.classList.toggle('is-expanded', expand);
            button.setAttribute('aria-expanded', String(expand));
            button.textContent = expand ? '⤡ Thu gọn ô viết' : '⤢ Mở rộng ô viết';
            essay.focus({preventScroll: true});
        });
    });
});
