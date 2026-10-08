document.addEventListener('DOMContentLoaded', () => {
    const groups = [...document.querySelectorAll('.enrollment-group')];
    const search = document.getElementById('enrollmentSearch');
    if (!search) return;
    const course = document.getElementById('enrollmentCourse');
    const status = document.getElementById('enrollmentStatus');
    const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D').toLowerCase().trim();
    const courses = new Map();
    const initialOpen = new Map();
    groups.forEach(group => {
        initialOpen.set(group,group.open);
        group.querySelectorAll('.enrollment-row').forEach(row => courses.set(row.dataset.courseId,row.dataset.courseTitle));
    });
    [...courses].sort((a,b)=>a[1].localeCompare(b[1],'vi')).forEach(([id,title])=>{
        const option=document.createElement('option');option.value=id;option.textContent=title;course.append(option);
    });
    let wasFiltering = false;
    const apply = () => {
        const query = normalize(search.value);
        const filtering = Boolean(query || course.value || status.value);
        if (filtering && !wasFiltering) groups.forEach(g=>initialOpen.set(g,g.open));
        let students = 0, enrollments = 0;
        groups.forEach(group=>{
            const matchesName=normalize(group.dataset.student).includes(query);
            let visible=0;
            group.querySelectorAll('.enrollment-row').forEach(row=>{
                const expired=row.dataset.expired==='true';
                const matchesStatus = !status.value || (status.value==='expired' ? expired && row.dataset.status!=='REVOKED' : status.value==='learning' ? ['ACTIVE','IN_PROGRESS'].includes(row.dataset.status) && !expired : row.dataset.status===status.value);
                row.hidden=!(matchesName && (!course.value || course.value===row.dataset.courseId) && matchesStatus);
                if(!row.hidden)visible++;
            });
            group.hidden=visible===0;
            group.querySelector('.enrollment-group-count').textContent=visible+' khóa học';
            if(filtering && visible)group.open=true;
            else if(!filtering && wasFiltering)group.open=initialOpen.get(group);
            if(visible){students++;enrollments+=visible;}
        });
        document.getElementById('enrollmentCount').textContent=students+' học viên · '+enrollments+' đăng ký phù hợp';
        document.getElementById('enrollmentEmpty').hidden=students!==0;
        wasFiltering=filtering;
    };
    search.addEventListener('input',apply);course.addEventListener('change',apply);status.addEventListener('change',apply);
    document.getElementById('enrollmentReset').addEventListener('click',()=>{search.value='';course.value='';status.value='';apply();});
    // Keep Bootstrap dialogs outside collapsed groups and clipping containers.
    document.querySelectorAll('#enrollmentGroups .modal').forEach(modal=>document.body.append(modal));
    apply();
});
