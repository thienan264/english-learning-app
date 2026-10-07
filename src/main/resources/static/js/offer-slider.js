(() => {
    const slider=document.getElementById('saleCoursesCarousel');
    if(!slider) return;
    const track=slider.querySelector('.offer-slider-track');
    const offers=[...track.querySelectorAll('.sale-carousel-item')];
    const fallback=track.querySelector('.no-sale-fallback');
    fallback.hidden=offers.length>0;
    const originals=offers.length?offers:[fallback];
    const count=originals.length;
    const prev=slider.querySelector('.offer-slider-prev'),next=slider.querySelector('.offer-slider-next');
    // Matching copies on both ends keep the neighbours visible across the seam.
    function copy(slide){
        const clone=slide.cloneNode(true);
        clone.dataset.offerCopy='true';clone.setAttribute('aria-hidden','true');
        clone.removeAttribute('id');clone.querySelectorAll('[id]').forEach(node=>node.removeAttribute('id'));
        clone.querySelectorAll('a,button,input,select,textarea').forEach(node=>node.setAttribute('tabindex','-1'));
        return clone;
    }
    if(count>1){
        const before=document.createDocumentFragment(),after=document.createDocumentFragment();
        originals.forEach(slide=>{before.append(copy(slide));after.append(copy(slide));});
        track.prepend(before);track.append(after);
    }
    const slides=[...track.querySelectorAll('.offer-slide')].filter(slide=>!slide.hidden);
    let position=count>1?count:0,moving=false,completion;
    const queue=[];
    function render(){
        slides.forEach((slide,i)=>{
            slide.classList.toggle('is-current',i===position);
            if(!slide.dataset.offerCopy){slide.setAttribute('role','group');slide.setAttribute('aria-label','Ưu đãi '+(originals.indexOf(slide)+1)+' / '+count);}
        });
        const active=slides[position];
        track.style.transform='translateX('+(slider.clientWidth/2-active.offsetLeft-active.offsetWidth/2)+'px)';
        prev.disabled=next.disabled=count<2;prev.hidden=next.hidden=count<2;
    }
    function finish(){
        clearTimeout(completion);
        if(!moving) return;
        // The copied seam has identical neighbours: reset without a visible jump.
        position=count+(position%count+count)%count;
        track.style.transition='none';render();void track.offsetWidth;track.style.transition='';
        moving=false;
        if(queue.length) move(queue.shift());
    }
    function move(step){
        if(count<2) return;
        if(moving){queue.push(step);return;}
        moving=true;position+=step;render();
        completion=setTimeout(finish,window.matchMedia('(prefers-reduced-motion: reduce)').matches?0:500);
    }
    track.addEventListener('transitionend',event=>{if(event.target===track && event.propertyName==='transform') finish();});
    prev.addEventListener('click',()=>move(-1));next.addEventListener('click',()=>move(1));
    slides.forEach((slide,i)=>{
        slide.addEventListener('click',event=>{if(!event.target.closest('a,button') && i!==position) move(i<position?-1:1);});
        if(!slide.dataset.offerCopy) slide.addEventListener('focusin',()=>{if(!moving){position=i;render();}});
    });
    let startX=null,suppressClick=false;
    slider.addEventListener('click',event=>{if(suppressClick){event.preventDefault();event.stopPropagation();suppressClick=false;}},true);
    slider.addEventListener('pointerdown',event=>{if(event.isPrimary) startX=event.clientX;});
    slider.addEventListener('pointerup',event=>{if(startX!==null && Math.abs(event.clientX-startX)>55){suppressClick=true;move(event.clientX<startX?1:-1);setTimeout(()=>{suppressClick=false;},0);}startX=null;});
    slider.addEventListener('pointercancel',()=>{startX=null;});
    window.addEventListener('resize',render);
    if(window.ResizeObserver) new ResizeObserver(render).observe(slider);
    track.style.transition='none';render();void track.offsetWidth;track.style.transition='';
    document.dispatchEvent(new Event('offers:updated'));
})();
