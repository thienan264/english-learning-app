# Lộ trình trợ lý học tập — phạm vi đồ án

Mục tiêu: dữ liệu bài học → kết quả học → gợi ý có căn cứ. Không chứng nhận CEFR hoặc band IELTS.

## Các bước

1. Phân loại bài học: level, mục tiêu, vai trò. Đã triển khai trang admin Mục tiêu học tập.
2. Đã thêm nhãn năng lực, level và giải thích trong bộ soạn đề Reading/Listening. Kết quả mới chụp lại nhãn/level khi nộp. Nhập file cũ vẫn để chưa phân loại, cần rà soát thủ công.
3. Tạo bộ nội dung mẫu nhỏ, đáp án và giải thích; tách câu luyện khỏi câu đánh giá.
4. Tổng hợp hồ sơ: tỷ lệ đúng theo kỹ năng/level/nhãn, số câu làm, thời điểm và tiến độ. Không so sánh trực tiếp score band với score thang 10. Writing dùng tiêu chí riêng.
5. Kết nối chat với hồ sơ của người đang đăng nhập và danh mục khóa thực tế; kiểm tra ID/link và quyền truy cập.
6. Kiểm thử người mới, dữ liệu thiếu, khóa đã mua, kết quả thay đổi và lỗi API.

## Bước 1: cách sử dụng

Vào quản trị khóa học → Xây dựng lộ trình → Mục tiêu học tập. Trang hiển thị cả bài có và chưa có module trong khóa.

- Beginner: thông tin trực tiếp, câu đơn, chủ đề quen thuộc.
- Intermediate: hiểu ý chính, paraphrase và liên kết các ý.
- Advanced: suy luận và phân tích lập luận.
- Vai trò: PRACTICE (học/luyện), PLACEMENT (đầu vào), FINAL (cuối khóa).
- Mục tiêu: “Tìm thông tin thời gian trong thông báo ngắn”, thay vì “Nâng cao Reading”.

Thông tin chưa được rà soát để trống. Không tự coi dữ liệu cũ là Beginner hay bài đánh giá. Level bài và level khóa có thể khác; metadata bài là thông tin được khai báo rõ cho bước tư vấn sau này.

Các cột learning_level, learning_objective, assessment_role được thêm vào lessons bằng cấu hình Hibernate ddl-auto=update hiện có khi khởi động ứng dụng. Không cập nhật điểm hoặc nội dung đề cũ.

## Phạm vi còn lại

Metadata chưa được dùng để quyết định lên level. Chưa có hồ sơ tổng hợp hoặc chat; bộ Quiz cũ chưa có ô chọn nhãn. Bài đầu vào trộn nhiều level cần metadata từng câu ở bước tiếp theo; không suy ra mọi câu cùng độ khó từ level bài.

## Bước 2: nhập bài mẫu

Xem bai-mau-doc-ho-so-ca-nhan.md. Sau khi khởi động lại, bộ soạn đề có các ô năng lực, level và giải thích. Metadata mới của Question thêm cột competency_tag và learning_level theo ddl-auto=update. Câu chưa chọn level dùng level bài khi lưu kết quả; nếu cả hai chưa có thì kết quả vẫn không có level. Không suy ra level từ tên đề.

## Nhập đề tự động

Bộ quét Strict Template đã hỗ trợ [LEVEL], [COMPETENCY] và [EXPLANATION] sau [Q]. Xem exams/strict-template-guide.md và Word mẫu exams/Reading_Beginner_Doc_ho_so_ca_nhan.docx. File cũ không có nhãn vẫn đọc được. Không áp dụng khuôn này cho Writing; Listening cần audio riêng.

## Bộ nội dung đầy đủ đã tạo

Xem course-content-pack/BAT_DAU_O_DAY.md và archive Bo_noi_dung_8_khoa_va_danh_gia.zip. 24 bài luyện (bao gồm bài57 đã nhập), 6 đề cuối chặng, 2 đề đầu vào; 29 Word Reading/Listening với159 câu, 12 audio, 3 bộ Writing/3 biểu đồ. Khóa đánh giá đầu vào ID12 hiện chưa có bài; cần tạo hai bài rồi nhập. Chưa ghi vào database hoặc triển khai Chat Box.

## Bước 3 — hồ sơ học tập đã triển khai

Trang /profile/learning-summary và API /profile/learning-summary/data. Xem learning-summary.md để biết quy tắc, dữ liệu còn thiếu và phạm vi. Chưa nối Chat Box hoặc gửi dữ liệu tới API AI. Không dùng bài luyện để tự gán level.
