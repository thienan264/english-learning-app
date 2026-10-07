(() => {
    if (window.pageNavigationFeedbackInstalled) return;
    window.pageNavigationFeedbackInstalled = true;
    let timer, fallback, navigationTimer;
    let navigating = false;
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    const scenes = [...document.body.children].filter(element =>
        ['MAIN','SECTION','DIV','FOOTER'].includes(element.tagName) &&
        !element.matches('.modal, .toast-container') && !element.querySelector('nav'));
    scenes.forEach(element => element.classList.add('page-content-arriving'));
    // Fixed dialogs must live outside animated/scrolling cards and tables.
    document.addEventListener('show.bs.modal', event => {
        const modal = event.target;
        if (!modal.classList.contains('modal')) return;
        scenes.forEach(element => element.classList.remove('page-content-arriving', 'page-content-leaving'));
        if (modal.parentElement !== document.body) document.body.append(modal);
    });
    const bar = document.createElement('div');
    bar.className = 'page-navigation-progress';
    bar.setAttribute('aria-hidden', 'true');
    document.body.append(bar);
    const reset = () => {
        clearTimeout(timer); clearTimeout(fallback);
        clearTimeout(navigationTimer); navigating = false;
        scenes.forEach(element => element.classList.remove('page-content-leaving'));
        bar.classList.remove('is-loading');
    };
    const start = () => {
        reset();
        // Fast navigation needs no visual interruption.
        timer = setTimeout(() => bar.classList.add('is-loading'), 150);
        fallback = setTimeout(reset, 10000);
    };
    document.addEventListener('click', event => {
        const link = event.target.closest('a[href]');
        if (!link || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || link.hasAttribute('download') || link.hasAttribute('data-bs-toggle') || (link.target && link.target !== '_self')) return;
        const url = new URL(link.href, window.location.href);
        if (!['http:', 'https:'].includes(url.protocol) || url.origin !== window.location.origin || (url.pathname === location.pathname && url.search === location.search)) return;
        if (event.defaultPrevented) return;
        if (reducedMotion.matches) { start(); return; }
        event.preventDefault();
        if (navigating) return;
        start(); navigating = true;
        scenes.forEach(element => element.classList.add('page-content-leaving'));
        navigationTimer = setTimeout(() => window.location.assign(url.href), 220);
    });
    document.addEventListener('submit', event => {
        const form = event.target;
        const target = event.submitter?.formTarget || form.target;
        if ((target && target !== '_self') || form.method === 'dialog') return;
        queueMicrotask(() => {
            if (event.defaultPrevented) return;
            start();
            if (!reducedMotion.matches) scenes.forEach(element => element.classList.add('page-content-leaving'));
        });
    });
    window.addEventListener('pageshow', reset);
    window.addEventListener('pagehide', reset);
})();
