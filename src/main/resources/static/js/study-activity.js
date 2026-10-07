(() => {
    const path=window.location.pathname;
    if (!path.startsWith('/learn/') && !path.startsWith('/lessons/') && !/^\/courses\/[^/]+\/(study-flashcards|vocabulary-quiz)$/.test(path)) return;
    let lastInteraction=Date.now();
    ['pointerdown','keydown','scroll','mousemove'].forEach(event=>document.addEventListener(event,()=>{lastInteraction=Date.now();},{passive:true}));
    async function heartbeat(){
        if(document.hidden || !document.hasFocus() || Date.now()-lastInteraction>120000) return;
        const token=document.querySelector('input[name="_csrf"]')?.value || document.querySelector('meta[name="_csrf"]')?.content || document.querySelector('meta[name="study-csrf"]')?.content;
        if(!token) return;
        try {await fetch('/api/activity/study',{method:'POST',headers:{'X-CSRF-TOKEN':token},credentials:'same-origin'});} catch (_) { /* Tracking must never interrupt learning. */ }
    }
    heartbeat();setInterval(heartbeat,30000);
})();
