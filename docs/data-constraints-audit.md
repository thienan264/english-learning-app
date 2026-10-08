# Rà soát ràng buộc dữ liệu

Kiểm tra mã nguồn và đọc trực tiếp pg_constraint trong PostgreSQL english_db ngày 08/10/2026. Không sửa dữ liệu trong lần rà soát này. Đây là kiểm tra tĩnh và truy vấn tổng hợp; chưa phải kiểm thử toàn bộ luồng bằng cách gửi dữ liệu sai.

## Đã có

- Khóa chính, khóa ngoại cho các quan hệ chính trong database.
- UNIQUE username, email, order_code; UNIQUE(course_id,user_id) ở đánh giá khóa học.
- Kiểm tra đăng ký: định dạng email, email trùng không phân biệt hoa thường ở ứng dụng, độ dài và độ mạnh mật khẩu, xác nhận mật khẩu.
- Kiểm tra mục tiêu học tập: level/vai trò hợp lệ, giới hạn 500 ký tự, bài thuộc chương của khóa.
- Callback thanh toán kiểm tra số tiền; PaymentService dùng transaction.

## Cần bổ sung

1. user_course_enrollments thiếu UNIQUE(user_id,course_id); user_lesson_progress thiếu UNIQUE(user_id,lesson_id); user_flashcard_progress thiếu UNIQUE(user_id,flashcard_id). Hiện chưa phát hiện trùng nhưng database chưa ngăn tạo trùng khi có yêu cầu đồng thời.
2. writing_exams thiếu UNIQUE(lesson_id), trong khi luồng soạn tìm một đề theo lessonId.
3. Giá và ngày ưu đãi thiếu validation phía máy chủ và CHECK database. CourseServiceImpl.saveCourse lưu trực tiếp. Cần giá không âm, giá ưu đãi nhỏ hơn giá gốc, ngày kết thúc không trước ngày bắt đầu, thời hạn truy cập hợp lệ.
4. Tên khóa/chương/bài cần kiểm tra chuỗi trống sau trim, không chỉ NOT NULL. Builder chưa kiểm tra đầy đủ loại bài/kỹ năng và quyền sở hữu module/lesson theo courseId của URL trước khi thêm/xóa.
5. Thiếu CHECK cho miền số: số câu đúng trong 0..tổng câu, tiến độ trong 0..100, điểm theo đúng thang, thời gian làm bài dương. Không áp một thang điểm chung cho Writing và Reading/Listening.
6. Luồng xóa khóa phải xử lý rõ đơn hàng, đăng ký, modules và các quan hệ phụ thuộc. CourseServiceImpl.deleteCourse chưa thể hiện đầy đủ các quan hệ này. FK có thể chặn xóa thay vì tự dọn; không nên xóa lịch sử thanh toán để giải quyết lỗi FK.
7. Cần kiểm thử transaction cho việc thay toàn bộ câu hỏi: lỗi giữa quá trình không được làm mất cấu trúc đề cũ. Không thể coi catch exception là rollback.

## Kết quả kiểm tra dữ liệu hiện tại

- Không phát hiện trùng cặp đăng ký khóa, tiến độ bài, tiến độ từ vựng, hoặc nhiều đề Writing cùng lesson.
- Không phát hiện số câu đúng ngoài miền, phần trăm hoàn thành ngoài 0..100, bài/chương lệch khóa, câu hỏi không có parent, hoặc tên khóa rỗng trong các truy vấn đã chạy.
- Phát hiện courses.id=11 (IELTS — Kỹ năng cốt lõi): giá gốc 5.000.000đ, sale_price 29.933.000đ. Giá ưu đãi cao hơn giá gốc; kỳ ưu đãi đã hết nhưng dữ liệu cấu hình vẫn không hợp lệ. Cần người quản trị xác định giá đúng, không tự đoán giá để sửa.

Ưu tiên gia cố validation backend, UNIQUE và CHECK sau khi rà soát dữ liệu; sau đó kiểm thử xóa và tính toàn vẹn transaction. Các ràng buộc đúng trong entity không đủ để khẳng định schema thực tế đã có, vì ứng dụng dùng ddl-auto=update.


## Hoàn tất ba phần ưu tiên — 09/10/2026

- Validation máy chủ và CHECK PostgreSQL cho giá không âm, khóa trả phí có giá dương, giá ưu đãi nhỏ hơn giá gốc, ngày ưu đãi đúng thứ tự và thời hạn truy cập không âm. Giá và ưu đãi hợp lệ vẫn sửa bình thường.
- Đã áp dụng UNIQUE cho đăng ký khóa, tiến độ bài và tiến độ từ vựng. Khóa theo người dùng trong transaction khi tạo tiến độ/đăng ký; callback thanh toán khóa đơn hàng để tránh xử lý đồng thời. Không giới hạn số lần làm bài hay lịch sử đơn hàng.
- Xóa khóa/chương/bài qua service transactional: kiểm tra quan hệ và lịch sử trước khi xóa. Khóa có đăng ký/đơn hàng/đánh giá/lịch sử từ vựng bị chặn xóa; bài có lịch sử làm bài hoặc học thực tế bị chặn. Nội dung chưa sử dụng vẫn có thể xóa. Thông báo lỗi hiển thị trên trang quản trị, không xóa lịch sử thanh toán để giải quyết khóa ngoại.
- Ưu đãi sai của khóa 11 đã hết hạn: gỡ cấu hình ưu đãi, giữ giá gốc. Giá trị cũ được lưu trong bảng course_pricing_repair_log; không đoán giá ưu đãi thay thế.
- Migration: scripts/migrations/20261009_basic_data_constraints.sql đã chạy trên english_db. Khi dùng database khác cần chạy migration này. Migration có transaction, kiểm tra tên constraint và có thể chạy lại.
- Đã kiểm thử trực tiếp PostgreSQL: từ chối giá âm, giá giảm cao hơn giá gốc, ngày đảo ngược, đăng ký trùng và tiến độ bài trùng. Dữ liệu thử được ROLLBACK.

Các mục rà soát khác ở trên (miền điểm, transaction khi thay đề, v.v.) vẫn là đề xuất riêng; lần này không tuyên bố đã kiểm thử mọi ràng buộc của toàn hệ thống.
