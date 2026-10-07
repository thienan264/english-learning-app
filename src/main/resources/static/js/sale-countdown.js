(() => {
    const clocks=[...document.querySelectorAll('[data-offer-end]')];
    if(!clocks.length) return;
    const pad=value=>String(value).padStart(2,'0');
    function update(){
        const now=Date.now();
        document.querySelectorAll('[data-offer-end]').forEach(clock=>{
            const end=Date.parse(clock.dataset.offerEnd);
            if(!Number.isFinite(end)) return;
            const remaining=Math.max(0,Math.ceil((end-now)/1000));
            const value=clock.querySelector('.offer-countdown-value');
            if(!remaining){
                clock.querySelector('.offer-countdown-label').textContent='Ưu đãi đã kết thúc';
                value.textContent='00 : 00 : 00';
                clock.closest('.offer-countdown').classList.add('offer-countdown-ended');
                return;
            }
            const days=Math.floor(remaining/86400),hours=Math.floor(remaining%86400/3600),minutes=Math.floor(remaining%3600/60),seconds=remaining%60;
            value.textContent=(days ? days+' ngày · ' : '')+pad(hours)+' : '+pad(minutes)+' : '+pad(seconds);
        });
    }
    update();setInterval(update,1000);
    document.addEventListener('offers:updated',update);
    document.addEventListener('visibilitychange',()=>{if(!document.hidden) update();});
})();
