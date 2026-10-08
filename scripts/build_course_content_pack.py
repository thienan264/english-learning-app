from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
from xml.sax.saxutils import escape
import json, csv, wave, subprocess, tempfile, hashlib
from course_content_data import EXAMS, WRITING

ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'docs/course-content-pack'
OUT.mkdir(parents=True,exist_ok=True)
COURSES={2:'02_Tieng_Anh_hang_ngay_1',4:'04_Tieng_Anh_nguoi_di_lam',5:'05_Tieng_Anh_nen_tang',7:'07_IELTS_Academic_nang_cao',8:'08_IELTS_Luyen_de_sua_diem_yeu',9:'09_Tieng_Anh_hang_ngay_2',10:'10_Ngu_phap_tu_vung_ung_dung',11:'11_IELTS_Ky_nang_cot_loi',12:'12_Danh_gia_dau_vao'}
TEMPLATE=ROOT/'docs/exams/Reading_Beginner_Doc_ho_so_ca_nhan.docx'
manifest=[]

def write_docx(path, lines):
    xml='<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>'
    for line in lines:
        xml+='<w:p><w:r><w:t xml:space="preserve">'+escape(line)+'</w:t></w:r></w:p>'
    xml+='<w:sectPr><w:pgSz w:w="11906" w:h="16838"/></w:sectPr></w:body></w:document>'
    with ZipFile(TEMPLATE) as source, ZipFile(path,'w',ZIP_DEFLATED) as output:
        for info in source.infolist():
            output.writestr(info.filename,xml.encode('utf-8') if info.filename=='word/document.xml' else source.read(info.filename))

def append_questions(lines, questions, default_level, first_number, skill, teaching=''):
    # Group contiguous questions of the same supported type; bind to the current passage.
    current_type=None
    for n,q in enumerate(questions,first_number):
        if q['type'] != current_type:
            current_type=q['type']
            instruction='Choose ONE correct answer, A, B, C or D.' if current_type=='MULTIPLE_CHOICE_SINGLE' else 'Write TRUE if the statement agrees, FALSE if it contradicts, or NOT GIVEN if there is not enough information.'
            if skill=='LISTENING': instruction='Listen to the recording. '+instruction
            if teaching: instruction=teaching+'\n'+instruction
            lines += ['[GROUP]',f'[TYPE] {current_type}','[INSTRUCTION] '+instruction]
        lines += [f'[Q] {n}. {q["text"]}',f'[LEVEL] {q.get("level") or default_level}',f'[COMPETENCY] {q["tag"]}']
        if q.get('options'):
            lines += [f'[OPT] {chr(65+i)}. {option}' for i,option in enumerate(q['options'])]
        lines += [f'[ANS] {q["answer"]}',f'[EXPLANATION] {q["explanation"]}','']

def make_audio(path, blocks, rate):
    # Render each spoken turn independently to preserve speaker voices. No Internet/API required.
    if path.exists() and path.stat().st_size>100: return
    pcm=[]; params=None
    with tempfile.TemporaryDirectory(prefix='english-course-audio-') as temporary:
        for i,block in enumerate(blocks):
            voice,text=block[:2]; speed=block[2] if len(block)>2 else rate
            part=Path(temporary)/f'{i}.wav'; txt=Path(temporary)/f'{i}.txt';txt.write_text(text)
            subprocess.run(['say','-v',voice,'-r',str(speed),'-f',str(txt),'-o',str(part),'--file-format=WAVE','--data-format=LEI16'],check=True,capture_output=True)
            with wave.open(str(part),'rb') as w:
                key=(w.getnchannels(),w.getsampwidth(),w.getframerate())
                if params is None: params=key
                if params!=key: raise ValueError('Audio sample rates differ')
                pcm.append(w.readframes(w.getnframes()))
            pcm.append(b'\x00'*(params[0]*params[1]*params[2]))
        with wave.open(str(path),'wb') as w:
            w.setnchannels(params[0]);w.setsampwidth(params[1]);w.setframerate(params[2])
            # Three seconds before the recording; one-second silence between speakers.
            w.writeframes(b'\x00'*(params[0]*params[1]*params[2]*3)+b''.join(pcm))

for exam in EXAMS:
    folder=OUT/COURSES[exam['course']];folder.mkdir(parents=True,exist_ok=True)
    base=folder/exam['slug']; lines=[]
    sections=exam.get('sections') or [(exam['level'],exam['title'],exam['content'],exam['questions'])]
    n=1
    for level,title,content,questions in sections:
        lines += ['[PASSAGE]',f'[TITLE] {title}','[CONTENT]']
        if exam['skill']=='READING' and exam['teaching'] and exam['role']=='PRACTICE':
            lines += ['Hướng dẫn: '+exam['teaching'],'']
        lines += content.splitlines()+['']
        append_questions(lines,questions,level,n,exam['skill'],exam['teaching'] if exam['skill']=='LISTENING' and exam['role']=='PRACTICE' else '')
        n+=len(questions)
    word=base.with_suffix('.docx');write_docx(word,lines)
    base.with_suffix('.txt').write_text('\n'.join(lines))
    audio=None; duration=None
    if exam['script']:
        script=base.parent/(base.name+'_transcript.txt')
        script.write_text('\n\n'.join(f'{block[0]}: {block[1]}' for block in exam['script']))
        audio=base.with_suffix('.wav')
        make_audio(audio,exam['script'],exam['rate'] or {'BEGINNER':120,'INTERMEDIATE':145,'ADVANCED':160,'MIXED':140}[exam['level']])
        with wave.open(str(audio),'rb') as w: duration=round(w.getnframes()/w.getframerate(),1)
    # Keep the teacher answer key separate from learner-facing passage/audio.
    guide=[f'# {exam["title"]}',f'Khóa ID: {exam["course"]}; Bài ID: {exam["lesson"] or "chưa tạo"}.',f'Level: {exam["level"]}; vai trò: {exam["role"]}.',f'Mục tiêu: {exam["objective"]}',f'Số câu: {len(exam["questions"])}.']
    if audio: guide += [f'Audio: {audio.name}; thời lượng {duration} giây. Giọng tổng hợp, không phải bản ghi người thật.','Transcript là tài liệu quản trị; không dán vào phần câu hỏi.']
    guide += ['','## Đáp án và căn cứ','']
    for i,q in enumerate(exam['questions'],1): guide += [f'{i}. **{q["answer"]}** · {q["tag"]} · {q.get("level") or exam["level"]}: {q["explanation"]}']
    base.parent.joinpath(base.name+'_giao_vien.md').write_text('\n\n'.join(guide))
    manifest.append(dict(course_id=exam['course'],lesson_id=exam['lesson'],title=exam['title'],skill=exam['skill'],level=exam['level'],role=exam['role'],objective=exam['objective'],question_count=len(exam['questions']),word=str(word.relative_to(OUT)),audio=str(audio.relative_to(OUT)) if audio else None,audio_seconds=duration,answer_key=' '.join(q['answer'] for q in exam['questions']),status='ALREADY_ENTERED' if exam['lesson']==57 else 'READY_TO_IMPORT'))
    print(f'Prepared {exam["lesson"] or "placement"}: {exam["title"]}',flush=True)

charts=[]
for task in WRITING:
    folder=OUT/COURSES[task['course']];folder.mkdir(parents=True,exist_ok=True)
    base=folder/task['slug']; chart=task['chart']; image=folder/(task['slug']+'_task1_chart.png')
    charts.append(dict(**chart,output=str(image)))
    label1=f'DEMO C{task["course"]} L{task["lesson"]} — Task 1'
    label2=f'DEMO C{task["course"]} L{task["lesson"]} — Task 2'
    text=f'''# {task['title']}

Khóa ID {task['course']}; Bài ID {task['lesson']}; level {task['level']}; vai trò PRACTICE. Task trọng tâm: {task['focus']}.

Bộ ghép Writing hiện yêu cầu cả hai task: tạo hai đề dưới đây ở Đề bài Writing rồi ghép chúng vào bài ID {task['lesson']}. Tổng thời gian: 60 phút. Đây là bộ bài luyện, không đảm bảo band cụ thể.

## Task 1 — nhập vào hệ thống

Tên: {label1}
Loại: TASK_1
Min words: 150
Time limit: 20 phút
Ảnh: {image.name}

Instruction:
{task['task1']}

## Task 2 — nhập vào hệ thống

Tên: {label2}
Loại: TASK_2
Min words: 250
Time limit: 40 phút

Instruction:
{task['task2']}

## Hướng dẫn học — tham khảo trước khi viết

Task 1: xác định đơn vị, năm và nhóm; chọn xu hướng chính cho overview; dùng số liệu minh họa. Không bịa nguyên nhân.
Task 2: phân tích yêu cầu, chọn quan điểm, lập hai ý chính với lý do/ví dụ; dành thời gian kiểm tra câu và liên kết.

## Gợi ý phản hồi — không dán vào đề nếu muốn đánh giá độc lập

Task 1: {task['notes1']}
Task 2: {task['notes2']}

Tiêu chí nhận xét Task 1: hoàn thành yêu cầu, tổ chức/liên kết, từ vựng, ngữ pháp.
Tiêu chí nhận xét Task 2: trả lời yêu cầu/lập luận, tổ chức/liên kết, từ vựng, ngữ pháp.
Điểm AI là tham khảo. Không dùng bài này một mình để suy ra level Reading/Listening.

## Dữ liệu biểu đồ để kiểm tra ảnh

{json.dumps(chart,ensure_ascii=False,indent=2)}

Dữ liệu giả định phục vụ đồ án, không phải thống kê thực tế.
'''
    base.with_suffix('.md').write_text(text)
    write_docx(base.with_suffix('.docx'),text.splitlines())
    (folder/(task['slug']+'_task1_instruction.txt')).write_text(task['task1'])
    (folder/(task['slug']+'_task2_instruction.txt')).write_text(task['task2'])
    manifest.append(dict(course_id=task['course'],lesson_id=task['lesson'],title=task['title'],skill='WRITING',level=task['level'],role='PRACTICE',word=str(base.with_suffix('.docx').relative_to(OUT)),guide=str(base.with_suffix('.md').relative_to(OUT)),chart=str(image.relative_to(OUT)),task1_title=label1,task2_title=label2,status='MANUAL_WRITING_BUILDER'))

(OUT/'chart-data.json').write_text(json.dumps(charts,ensure_ascii=False,indent=2))
subprocess.run(['swift','-module-cache-path','/private/tmp/english-course-swift-cache',str(ROOT/'scripts/render_course_charts.swift'),str(OUT/'chart-data.json')],check=True,capture_output=True)
# Keep shared chart data portable after rendering with absolute output paths.
(OUT/'chart-data.json').write_text(json.dumps([
    {**chart, 'output': str(Path(chart['output']).relative_to(OUT))} for chart in charts
],ensure_ascii=False,indent=2))
(OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2))
with (OUT/'DANH_SACH_NHAP.csv').open('w',encoding='utf-8-sig',newline='') as file:
    writer=csv.writer(file);writer.writerow(['course_id','lesson_id','title','skill','level','role','word','audio_or_chart','questions'])
    for row in manifest: writer.writerow([row['course_id'],row['lesson_id'] or '',row['title'],row['skill'],row['level'],row['role'],row['word'],row.get('audio') or row.get('chart') or '',row.get('question_count','')])
lines=['# BỘ NỘI DUNG ĐỒ ÁN — HƯỚNG DẪN NHẬP','',
'Bộ tài liệu gốc phục vụ demo học tập và AI tư vấn. Không phải đề IELTS chính thức, chứng nhận CEFR hay cam kết tăng band. Người, tổ chức và biểu đồ là tình huống giả định. Audio là giọng tổng hợp.',
'', '## Nhập Reading/Listening', '',
'1. Mở DANH_SACH_NHAP.csv để ghép file với ID bài. Bài 57 đã nhập: không cần nhập lại hoặc ghi đè kết quả cũ.',
'2. Vào Soạn đề thi của đúng bài → chọn Word → Bắt đầu phân tích → kiểm tra câu hỏi, nhãn, đáp án, giải thích → Lưu.',
'3. Listening: tải thêm WAV cùng tên; transcript nằm riêng để kiểm tra, không dán transcript vào nội dung hiển thị cho học viên.',
'4. Bài luyện chọn PRACTICE; bài cuối chặng chọn FINAL. Các mục tiêu đã có trong file giáo viên và manifest.',
'5. Đề cuối chặng có 6 câu mỗi kỹ năng. Ngưỡng demo 8/10 nghĩa là cần 5/6 câu đúng, vì 4/6 chỉ được 6.7/10. Kiểm tra pass_score chưa đặt trên 10.',
'', '## Nhập Writing', '',
'Ba thư mục Writing có hướng dẫn riêng. Không đưa chúng vào bộ quét Reading/Listening. Tạo Task 1, tải ảnh PNG; tạo Task 2; ghép đúng hai task vào bài. Các file instruction.txt chỉ có đề, không kèm đáp án hay dàn ý.',
'', '## Đánh giá đầu vào — khóa ID 12', '',
'Khóa Đánh giá đầu vào đã được tạo nhưng chưa có bài. Tạo một chương và hai bài MOCK_TEST: Reading và Listening, rồi quét hai Word tương ứng. Vai trò PLACEMENT. Mỗi bài có 9 câu, chia 3 câu Beginner, 3 Intermediate, 3 Advanced. Không gán tất cả câu cùng level; tag LEVEL đã có trong Word.',
'Level bài để Chưa phân loại vì đề trộn mức; mục tiêu là gợi ý mức bắt đầu của từng kỹ năng. Kết quả 3 câu/mức chỉ là bằng chứng demo còn ít. Có thể đề xuất Intermediate khi đạt ít nhất 2/3 Beginner, và Advanced khi đồng thời đạt ít nhất 2/3 Beginner và 2/3 Intermediate. Nếu khó đúng nhưng dễ sai, đề xuất kiểm tra bổ sung. Đây mới là quy tắc dự kiến: hệ thống chưa tự xếp level/chat.',
'', '## Phạm vi và lưu ý', '',
'Bộ gồm 24 bài luyện của 8 khóa, 6 đề cuối chặng và 2 đề đầu vào. Trong 24 bài luyện có 3 bài Writing; Reading/Listening có tổng 159 câu. Các bài Reading/Listening ngắn đánh giá mục tiêu cụ thể, không quy đổi band IELTS.',
'Các bài demo cũ khác trong database không nằm trong bộ này; chưa cần xóa. Khi nối AI, chỉ lấy nội dung đã rà soát/phân loại, không mặc định coi mọi bài cũ là phù hợp.',
'Bài 67 đang có kỹ năng Listening trong database nên nội dung được thiết kế thành nghe paraphrase, không yêu cầu sửa loại bài.',
'Đề cuối chặng và đầu vào dùng nội dung mới; không lấy lại câu luyện. Không cần tạo thêm khóa ngoài khóa ID12 đã có.',
'', '## Bảng ghép nhanh', '', '| Khóa ID | Bài ID | Bài | File Word |', '|---|---|---|---|']
for row in manifest:
    lines.append(f'| {row["course_id"]} | {row["lesson_id"] or "Tạo mới"} | {row["title"]} | [{Path(row["word"]).name}]({row["word"]}) |')
(OUT/'BAT_DAU_O_DAY.md').write_text('\n'.join(lines))
print(f'Generated {len(manifest)} packages; {sum(x.get("question_count",0) for x in manifest)} questions; {sum(bool(x.get("audio")) for x in manifest)} audio tracks.',flush=True)
