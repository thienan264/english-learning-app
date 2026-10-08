document.addEventListener('DOMContentLoaded', () => {
    const search = document.getElementById('notificationSearch');
    if (!search) return;
    const type = document.getElementById('notificationType');
    const read = document.getElementById('notificationRead');
    const items = [...document.querySelectorAll('.admin-notification-item')];
    const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D').toLowerCase().trim();
    const entries = items.map(item => ({item, text:normalize(item.querySelector('h6').textContent+' '+item.querySelector('p').textContent)}));
    [...new Set(items.map(item=>item.dataset.title))].sort((a,b)=>a.localeCompare(b,'vi')).forEach(title=>{
        const option=document.createElement('option');option.value=title;option.textContent=title;type.append(option);
    });
    const apply = () => {
        const query=normalize(search.value);
        let count=0;
        entries.forEach(({item,text})=>{
            item.hidden=!(text.includes(query) && (!type.value || item.dataset.title===type.value) && (!read.value || item.dataset.read===read.value));
            if(!item.hidden) count++;
        });
        document.getElementById('notificationFilterCount').textContent='Hiển thị '+count+' / '+items.length+' thông báo';
        document.getElementById('notificationFilterEmpty').hidden=count!==0 || items.length===0;
    };
    search.addEventListener('input',apply);type.addEventListener('change',apply);read.addEventListener('change',apply);
    document.getElementById('notificationReset').addEventListener('click',()=>{search.value='';type.value='';read.value='';apply();});
    apply();
});
