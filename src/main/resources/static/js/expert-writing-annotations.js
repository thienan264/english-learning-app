(() => {
    'use strict';
    // Offsets refer to the immutable submitted text, including repeated sentences.
    const normalizeText = text => text.replace(/\r\n?/g, '\n');
    function segments(text, notes) {
        let cursor = 0;
        const result = [];
        for (const note of [...notes].sort((a, b) => a.start - b.start)) {
            if (!Number.isInteger(note.start) || !Number.isInteger(note.end) || note.start < cursor || note.end <= note.start || note.end > text.length || text.slice(note.start, note.end) !== note.original) throw new Error('Đoạn đánh dấu không khớp bài gốc.');
            result.push({text: text.slice(cursor, note.start)}, {text: text.slice(note.start, note.end), note});
            cursor = note.end;
        }
        result.push({text: text.slice(cursor)});
        return result;
    }
    function popupPosition(anchor, viewport, size) {
        const margin = 12, gap = 8;
        const left = Math.max(margin, Math.min(anchor.left, viewport.width - size.width - margin));
        const below = anchor.bottom + gap;
        const above = anchor.top - size.height - gap;
        const preferredTop = below + size.height <= viewport.height - margin ? below : (above >= margin ? above : margin);
        const top = Math.max(margin, Math.min(preferredTop, viewport.height - size.height - margin));
        return {left, top};
    }
    if (typeof module !== 'undefined'  && module.exports) module.exports = {segments, normalizeText, popupPosition};
    if (typeof document === 'undefined') return;
    const tasks = [...document.querySelectorAll('.expert-task')];
    if (!tasks.length) return;
    const input = document.getElementById('annotationsJson');
    let annotations = [];
    try {
        const stored = document.getElementById('expertReviewData')?.value;
        annotations = stored ? JSON.parse(stored).annotations : JSON.parse(input?.value || '[]');
        if (!Array.isArray(annotations)) throw new Error('Dữ liệu đánh dấu không hợp lệ.');
    } catch (error) {
        const warning = document.createElement('p');warning.className = 'alert alert-warning';warning.textContent = 'Không đọc được đánh dấu: ' + error.message;
        tasks[0].before(warning);
        annotations = [];
    }
    const sync = () => { if (input) input.value = JSON.stringify(annotations); };
    const el = (tag, className, text) => { const node = document.createElement(tag);node.className = className; if (text !== undefined) node.textContent = text;return node; };
    const states = tasks.map(root => ({root, task: Number(root.dataset.task), essay: root.querySelector('.expert-essay'), text: normalizeText(root.querySelector('.expert-essay').textContent), selected: null, editable: root.dataset.editable === 'true'}));
    function render(state) {
        const {root, essay, text, task, editable} = state;
        const notes = annotations.filter(a => a.task === task);
        const list = root.querySelector('.annotation-list');list.replaceChildren();essay.replaceChildren();
        for (const part of segments(text, notes)) {
            if (!part.note) { essay.append(document.createTextNode(part.text));continue; }
            const index = annotations.indexOf(part.note);
            const mark = el('button', 'expert-error-mark', part.text);mark.type = 'button';
            mark.setAttribute('aria-label', 'Lỗi ' + (index + 1) + ': ' + part.text + '. Xem câu sửa');
            mark.addEventListener('click', () => { if (editable) { openEditor(state, part.note, mark, true);return; } const item = document.getElementById('expert-error-' + index);item?.scrollIntoView({block: 'nearest', behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'});item?.focus({preventScroll:true}); });
            essay.append(mark);
        }
        for (const note of notes) {
            const index = annotations.indexOf(note);
            const card = el('section', 'expert-error-note');card.id = 'expert-error-' + index;card.tabIndex = -1;
            card.append(el('h4', 'h6 fw-bold', 'Lỗi ' + (index + 1)), el('div', 'expert-text text-danger mb-2', note.original));
            if (editable) {
                const correctionLabel = el('label', 'form-label fw-bold', 'Câu sửa đúng');
                const correction = el('textarea', 'form-control mb-2');correction.id = 'error-correction-' + index;correctionLabel.htmlFor = correction.id;correction.value = note.correction;correction.maxLength = 5000;correction.rows = 2;
                const explanationLabel = el('label', 'form-label', 'Giải thích lỗi');
                const explanation = el('textarea', 'form-control mb-2');explanation.id = 'error-explanation-' + index;explanationLabel.htmlFor = explanation.id;explanation.value = note.explanation || '';explanation.maxLength = 5000;explanation.rows = 2;
                correction.addEventListener('input', () => { note.correction = correction.value;sync(); });
                explanation.addEventListener('input', () => { note.explanation = explanation.value;sync(); });
                const remove = el('button', 'btn btn-sm btn-outline-danger', 'Xóa đánh dấu');remove.type = 'button';
                remove.addEventListener('click', () => { annotations = annotations.filter(a => a !== note);sync();states.forEach(render); });
                card.append(correctionLabel, correction, explanationLabel, explanation, remove);
            } else {
                card.append(el('div', 'small fw-bold text-success', 'Câu sửa đúng'), el('div', 'expert-text text-success', note.correction));
                if (note.explanation) card.append(el('div', 'small fw-bold mt-2', 'Giải thích'), el('div', 'expert-text', note.explanation));
            }
            list.append(card);
        }
    }
    let activeEditor = null;
    function positionEditor(state) {
        const rect = state.anchor?.getBoundingClientRect();
        if (!rect || state.draft.hidden) return;
        const size = state.draft.getBoundingClientRect();
        const position = popupPosition(rect, {width:window.innerWidth, height:window.innerHeight}, size);
        state.draft.style.left = position.left + 'px';state.draft.style.top = position.top + 'px';
    }
    function closeEditor(state, restoreFocus = true) {
        state.draft.hidden = true;state.selected = null;state.editing = null;
        state.correction.value = '';state.explanation.value = '';state.message.textContent = '';
        if (activeEditor === state) activeEditor = null;
        if (restoreFocus) state.essay.focus({preventScroll:true});
    }
    function openEditor(state, note, anchor, editing = false) {
        if (!state.draft) return;
        if (activeEditor === state && editing && state.editing === note) { positionEditor(state);return; }
        if (activeEditor && !activeEditor.draft.hidden) {
            if (activeEditor.correction.value || activeEditor.explanation.value) {
                activeEditor.message.textContent = 'Lưu hoặc hủy phần đang sửa trước khi chọn đoạn khác.';
                activeEditor.correction.focus({preventScroll:true});return;
            }
            closeEditor(activeEditor, false);
        }
        state.selected = {...note};state.editing = editing ? note : null;state.anchor = anchor;
        state.draft.querySelector('.annotation-original').textContent = note.original;
        state.correction.value = editing ? note.correction : '';
        state.explanation.value = editing ? note.explanation || '' : '';
        state.draft.querySelector('.annotation-add').textContent = editing ? 'Lưu chỉnh sửa' : 'Lưu đánh dấu';
        state.remove.hidden = !editing;state.draft.hidden = false;state.message.textContent = '';
        activeEditor = state;positionEditor(state);state.correction.focus({preventScroll:true});
    }
    for (const state of states) {
        try { render(state); } catch (error) { state.essay.textContent = state.text;state.root.querySelector('.annotation-list').textContent = error.message; }
        if (!state.editable || !state.root.querySelector('.annotation-editor')) continue;
        const root = state.root;
        state.draft = root.querySelector('.annotation-draft');
        state.message = root.querySelector('.annotation-message');state.draft.append(state.message);
        state.correction = state.draft.querySelector('.annotation-correction');
        state.explanation = state.draft.querySelector('.annotation-explanation');
        state.draft.classList.add('annotation-popup');state.draft.setAttribute('role','dialog');
        state.draft.setAttribute('aria-label','Sửa lỗi Task ' + state.task);
        state.essay.tabIndex = -1;
        state.remove = el('button','btn btn-outline-danger ms-2','Xóa đánh dấu');state.remove.type = 'button';state.remove.hidden = true;state.draft.append(state.remove);
        document.body.append(state.draft);
        const capture = event => {
            if (event.type === 'keyup' && event.shiftKey) return;
            const selection = window.getSelection();
            if (!selection || !selection.rangeCount || selection.isCollapsed) return;
            const range = selection.getRangeAt(0);
            if (!state.essay.contains(range.startContainer) || !state.essay.contains(range.endContainer)) return;
            const prefix = range.cloneRange();prefix.selectNodeContents(state.essay);prefix.setEnd(range.startContainer, range.startOffset);
            const start = prefix.toString().length;
            const note = {task:state.task, start, end:start + range.toString().length, original:range.toString()};
            if (!note.original.trim()) return;
            const overlapping = annotations.find(b => b.task === note.task && note.start < b.end && note.end > b.start);
            if (overlapping) {
                const ordered = annotations.filter(a => a.task === state.task).sort((a,b) => a.start - b.start);
                const mark = state.essay.querySelectorAll('.expert-error-mark')[ordered.indexOf(overlapping)];
                openEditor(state, overlapping, mark || range.cloneRange(), true);return;
            }
            openEditor(state, note, range.cloneRange());
        };
        state.essay.addEventListener('pointerup', capture);state.essay.addEventListener('keyup', capture);
        state.draft.querySelector('.annotation-cancel').addEventListener('click', () => closeEditor(state));
        state.draft.addEventListener('keydown', event => { if (event.key === 'Escape') { event.preventDefault();event.stopPropagation();closeEditor(state); } });
        state.remove.addEventListener('click', () => { annotations = annotations.filter(a => a !== state.editing);closeEditor(state);sync();states.forEach(render); });
        state.draft.querySelector('.annotation-add').addEventListener('click', () => {
            if (!state.selected || !state.correction.value.trim()) { state.message.textContent = 'Nhập câu sửa đúng trước khi lưu.';return; }
            if (!state.editing && annotations.length >= 100) { state.message.textContent = 'Tối đa 100 lỗi được đánh dấu.';return; }
            const note = {...state.selected, correction:state.correction.value.trim(), explanation:state.explanation.value.trim()};
            const other = annotations.filter(a => a !== state.editing);
            try { segments(state.text, [...other.filter(a => a.task === state.task), note]); } catch(error) { state.message.textContent = error.message;return; }
            annotations = [...other, note];annotations.sort((a,b) => a.task - b.task || a.start - b.start);sync();
            closeEditor(state);states.forEach(render);
        });
    }
    window.addEventListener('resize', () => { if (activeEditor) positionEditor(activeEditor); });
    document.addEventListener('scroll', () => { if (activeEditor) positionEditor(activeEditor); }, true);
    document.getElementById('expertReviewForm')?.addEventListener('submit', event => {
        const unfinished = states.find(s => s.draft && !s.draft.hidden);
        if (unfinished) { event.preventDefault();unfinished.message.textContent = 'Thêm hoặc hủy đoạn đánh dấu đang nhập trước khi gửi phản hồi.';unfinished.correction.focus({preventScroll:true});return; }
        if (annotations.some(a => !a.correction?.trim())) { event.preventDefault();alert('Nhập câu sửa đúng cho tất cả lỗi đã đánh dấu.');return; }
        sync();
    });
})();
