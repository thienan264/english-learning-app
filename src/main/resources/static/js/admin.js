document.addEventListener('DOMContentLoaded', () => {
    const path = window.location.pathname;
    const links = [...document.querySelectorAll('.admin-side-link')];
    const active = links.filter(link => path === link.pathname || path.startsWith(link.pathname + '/'))
        .sort((a, b) => b.pathname.length - a.pathname.length)[0];
    if (active) {
        active.classList.add('active');
        active.setAttribute('aria-current', 'page');
    }
    const breadcrumb = document.getElementById('adminBreadcrumb');
    if (breadcrumb) {
        const title = document.querySelector('.admin-page-heading h1')?.textContent.trim() || 'Quản lý';
        const back = document.querySelector('.admin-main .admin-back-button');
        const parent = back && back.pathname !== path ? back : null;
        const section = active || (parent && links.filter(link => parent.pathname === link.pathname || parent.pathname.startsWith(link.pathname + '/')).sort((a,b) => b.pathname.length-a.pathname.length)[0]);
        const add = (label, href) => {
            const item=document.createElement('li');item.className='breadcrumb-item';
            const text=document.createElement(href ? 'a' : 'span');text.textContent=label;
            if(href) text.href=href;
            else { item.classList.add('active');item.setAttribute('aria-current','page'); }
            item.append(text);breadcrumb.append(item);
        };
        if(section && section.pathname !== path) add(section.textContent.trim(),section.href);
        if(parent && parent.pathname !== section?.pathname && parent.pathname !== '/admin/dashboard') {
            const label=parent.pathname.endsWith('/builder') ? 'Lộ trình khóa học' : parent.pathname.endsWith('/lessons') ? 'Bài học khóa học' : parent.textContent.replace(/[←↩]/g,'').replace(/Quay lại|Trở lại/gi,'').trim() || 'Danh sách';
            add(label,parent.href);
        }
        add(title);
    }
    // Existing server filters and DataTables retain their own search controls.
    document.querySelectorAll('.admin-main table').forEach((table, index) => {
        if (table.hasAttribute('data-enrollment-group-table') || ['enrollmentTable'].includes(table.id) || table.closest('#app') || !table.tHead || !table.tBodies.length) return;
        const rows = [...table.tBodies[0].rows].filter(row => !row.querySelector('td[colspan]'));
        if (!rows.length) return;
        const tools = document.createElement('div');
        tools.className = 'admin-table-tools';
        const label = document.createElement('label');
        label.className = 'admin-table-search';
        const icon = document.createElement('i');
        icon.className = 'bi bi-search';
        icon.setAttribute('aria-hidden', 'true');
        const input = document.createElement('input');
        input.type = 'search'; input.className = 'form-control';
        input.placeholder = table.hasAttribute('data-name-filter') ? 'Tìm theo họ tên hoặc tên đăng nhập…' : 'Tìm trong danh sách…';
        input.setAttribute('aria-label', table.hasAttribute('data-name-filter') ? 'Lọc theo tên học viên' : 'Tìm trong danh sách ' + (index + 1));
        label.append(icon, input);
        const count = document.createElement('span');
        count.className = 'admin-table-count';
        count.setAttribute('aria-live', 'polite');
        count.textContent = rows.length + ' mục';
        let courseFilter=null;
        let advisoryFilter=null;
        tools.append(label);
        if(table.hasAttribute('data-course-filter')) {
            courseFilter=document.createElement('select');
            courseFilter.className='form-select admin-course-filter';
            courseFilter.setAttribute('aria-label','Lọc loại khóa học');
            [['all','Tất cả khóa học'],['free','Miễn phí'],['paid','Có phí'],['sale','Đang ưu đãi']].forEach(([value,text])=>{
                const option=document.createElement('option');option.value=value;option.textContent=text;courseFilter.append(option);
            });
            tools.append(courseFilter);
        }
        if(table.hasAttribute('data-advisory-filter')) {
            advisoryFilter=document.createElement('select');
            advisoryFilter.className='form-select admin-course-filter';
            advisoryFilter.setAttribute('aria-label','Lọc trạng thái tư vấn');
            [['all','Tất cả trạng thái'],['PENDING','Chưa gọi'],['CONTACTED','Đã gọi'],['CANCELLED','Hủy']].forEach(([value,text])=>{
                const option=document.createElement('option');option.value=value;option.textContent=text;advisoryFilter.append(option);
            });
            tools.append(advisoryFilter);
        }
        tools.append(count);
        table.before(tools);
        if(table.hasAttribute('data-name-filter') && window.jQuery?.fn?.DataTable) {
            const normalizeName=value=>value.toLocaleLowerCase('vi').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d');
            window.jQuery.fn.dataTable.ext.search.push((settings, data, index)=>settings.nTable!==table || normalizeName(settings.aoData[index].nTr.dataset.studentName || '').includes(normalizeName(input.value.trim())));
            const orders=window.jQuery(table).DataTable({
                order:[[4,'desc']],
                language:{search:'Tìm trong danh sách:',lengthMenu:'Hiển thị _MENU_ mục',info:'_START_–_END_ / _TOTAL_ mục',infoEmpty:'0 mục',infoFiltered:'(từ _MAX_ mục)',zeroRecords:'Không tìm thấy kết quả phù hợp.',emptyTable:'Chưa có đơn hàng.',paginate:{first:'Đầu',last:'Cuối',next:'Sau',previous:'Trước'}}
            });
            const updateCount=()=>{count.textContent=orders.rows({search:'applied'}).count()+' / '+rows.length+' mục';};
            orders.on('draw',updateCount);
            input.addEventListener('input',()=>orders.draw());
            updateCount();
            return;
        }
        const empty = table.tBodies[0].insertRow();
        empty.hidden = true;
        const cell = empty.insertCell(); cell.colSpan = table.tHead.rows[0].cells.length;
        cell.className = 'text-center text-muted py-4'; cell.textContent = 'Không tìm thấy kết quả phù hợp.';
        const normalize = value => value.toLocaleLowerCase('vi').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').toLocaleLowerCase('vi');
        const applyFilters = () => {
            const query = normalize(input.value.trim());
            let visible = 0;
            rows.forEach(row => {
                const type=courseFilter?.value || 'all';
                const matchesType=type==='all' || (type==='free' && row.dataset.free==='true') || (type==='paid' && row.dataset.free==='false') || (type==='sale' && row.dataset.onSale==='true');
                const matchesStatus=!advisoryFilter || advisoryFilter.value==='all' || row.dataset.advisoryStatus===advisoryFilter.value;
                const searchable=table.hasAttribute('data-name-filter') ? row.dataset.studentName || '' : [...row.cells].map(cell => cell.querySelector('form') ? '' : cell.textContent).join(' ');
                row.hidden = !normalize(searchable).includes(query) || !matchesType || !matchesStatus;
                if (!row.hidden) visible++;
            });
            empty.hidden = visible !== 0;
            count.textContent = visible + ' / ' + rows.length + ' mục';
        };
        input.addEventListener('input',applyFilters);
        if(courseFilter) courseFilter.addEventListener('change',applyFilters);
        if(advisoryFilter) advisoryFilter.addEventListener('change',applyFilters);
    });
});
// Use native anchor navigation, including keyboard access and reduced-motion preferences.
if(document.querySelector('.dashboard-jump-nav')) {
    document.documentElement.style.scrollBehavior=window.matchMedia('(prefers-reduced-motion: reduce)').matches?'auto':'smooth';
    const observer=new IntersectionObserver(entries=>{
        entries.forEach(entry=>{if(entry.isIntersecting){
            document.querySelectorAll('.dashboard-jump-nav a').forEach(link=>{
                const active=link.hash==='#'+entry.target.id;link.classList.toggle('active',active);
                if(active) link.setAttribute('aria-current','location');else link.removeAttribute('aria-current');
            });
        }});
    },{rootMargin:'-140px 0px -60% 0px'});
    document.querySelectorAll('.dashboard-anchor').forEach(el=>observer.observe(el));
}
