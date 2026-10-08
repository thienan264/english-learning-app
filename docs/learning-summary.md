# Hồ sơ học tập — bước chuẩn bị Chat Box

## Mở chức năng

Menu tài khoản → Hồ sơ học tập; URL /profile/learning-summary. API nội bộ /profile/learning-summary/data lấy phiên đăng nhập, không nhận userId để chuyển tài khoản. DTO không chứa email, mật khẩu, bản ghi User hoặc bài viết cá nhân.

## Quy tắc bản đồ án

- Reading và Listening riêng biệt. Writing vẫn dùng chức năng chấm riêng; chưa suy ra level tổng thể.
- Lượt gần nhất mỗi bài trong 90 ngày, không chọn điểm cao nhất hoặc cộng tất cả lần làm lại.
- Đếm từng câu có snapshot level, nhãn năng lực và isCorrect hợp lệ. Câu thiếu nhãn để unknown, không suy diễn từ đề vừa sửa.
- Tách thống kê theo cả năng lực và level; không dùng tỷ lệ Beginner để che phần yếu Advanced.
- Mỗi nhóm từ5 câu, tỷ lệ đúng dưới70% thì Nên ôn lại. Ít hơn5 câu: Cần thêm dữ liệu. Đây là ngưỡng nội bộ, không chuẩn trình độ.
- Bài PLACEMENT cần tối thiểu3 câu mỗi level. Beginner chưa đạt2/3 → đề xuất Beginner; Beginner đạt nhưng Intermediate chưa đạt2/3 → Intermediate; hai mức đều đạt → Advanced. Nếu mức cao đạt trong khi mức thấp không đạt, không xếp và đề nghị kiểm tra thêm.
- Bài FINAL cần tối thiểu6 câu, đủ nhãn và cùng level. Ngưỡng pass_score, mặc định8/10. Đạt Beginner → Intermediate, đạt Intermediate → Advanced; Advanced đạt vẫn Advanced. Không đạt → ôn chặng vừa làm.
- Dùng bài đánh giá hoàn chỉnh gần nhất làm căn cứ, có link xem lại. Nếu chỉ có bài luyện thì chưa xếp mức.
- Bài hoàn thành không tự chứng nhận năng lực. Khóa hết hạn/thu hồi vẫn có trạng thái riêng.

## Kiểm tra dữ liệu lúc triển khai

Ngày08/10/2026: bài58 (Nghe lịch sinh hoạt),59 (Đọc lịch và thông báo),89 (Reading cuối Beginner),90 (Listening cuối Beginner) còn0 câu. Bài58 và90 chưa có audio. File tương ứng đã có trong course-content-pack/05_Tieng_Anh_nen_tang; cần quét Word, tải WAV và bấm Lưu. Bài đầu vào95/96 có9 câu/bài và level từng câu đầy đủ; level bài đang Beginner không ảnh hưởng vì hồ sơ dùng snapshot từng câu.

## Kiểm thử

Người mới, đánh giá đầu vào trộn level, lặp đề, snapshot thiếu, JSON lỗi, dữ liệu cũ, user khác, kết quả mâu thuẫn, đề cuối chặng, khóa hết hạn, trang Thymeleaf và API. Chưa chạy thực nghiệm phân loại trên người học thật hoặc UI trình duyệt với database đang chạy.

## Bước tiếp theo

Dùng DTO này làm ngữ cảnh riêng của user trong Chat Box, kết hợp danh mục bài/khóa đủ nội dung và quyền truy cập. AI giải thích gợi ý; backend giữ trách nhiệm tính kết quả, quyền truy cập và kiểm tra ID/link. Không tự train mô hình.
