document.addEventListener('DOMContentLoaded', () => {
    const navbar = document.querySelector('.student-navbar');
    if (!navbar) return;
    const links = [...navbar.querySelectorAll('.navbar-nav .nav-link')];
    const targets = links.map(link => ({link, url: new URL(link.href, location.href)}));
    const activate = link => {
        links.forEach(item => {
            const active = item === link;
            item.classList.toggle('is-current', active);
            if (active) item.setAttribute('aria-current', 'location');
            else item.removeAttribute('aria-current');
        });
    };
    const home = targets.find(t => t.url.pathname === '/' && !t.url.hash)?.link;
    const sections = targets.filter(t => t.url.hash && t.url.pathname === location.pathname)
        .map(t => ({...t, section: document.getElementById(t.url.hash.slice(1))})).filter(t => t.section);
    const update = () => {
        if (location.pathname === '/') {
            const threshold = navbar.getBoundingClientRect().height + 100;
            let selected = home;
            sections.forEach(t => { if (t.section.getBoundingClientRect().top <= threshold) selected = t.link; });
            activate(selected);
        } else {
            const exact = targets.find(t => !t.url.hash && t.url.pathname === location.pathname);
            const coursePage = /^\/(courses?|learn\/course|lessons)(\/|$)/.test(location.pathname);
            activate(exact?.link || (coursePage ? targets.find(t => t.url.hash === '#courses')?.link : null));
        }
    };
    targets.forEach(({link}) => link.addEventListener('click', () => activate(link)));
    let scheduled = false;
    window.addEventListener('scroll', () => {
        if (scheduled) return;
        scheduled = true;
        requestAnimationFrame(() => { scheduled = false;update(); });
    }, {passive:true});
    window.addEventListener('hashchange', update);
    window.addEventListener('resize', update);
    update();
    const destination = targets.find(t => t.url.pathname === location.pathname && t.url.hash === location.hash);
    if (location.hash && destination) activate(destination.link);
});
