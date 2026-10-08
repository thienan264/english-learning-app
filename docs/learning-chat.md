# Chat tư vấn học tập

Thẻ gợi ý và giá được giữ cùng lịch sử trong phiên, nên quay lại trang không mất nút mở khóa. Tin nhắn cũ chưa lưu thẻ sẽ không thể phục hồi thẻ. Giá trên thẻ là giá tại thời điểm tư vấn; xem giá hiện hành tại trang khóa học.

Catalog phân biệt đã đăng ký (enrolled) với quyền học (accessible). Câu hỏi ưu đãi lấy trực tiếp từ dữ liệu máy chủ và loại khóa đã đăng ký. Tư vấn thông thường được hướng dẫn không mời mua lại khóa còn quyền học. Giao diện trình bày giá gốc, giá hiện tại, phần trăm giảm và thời hạn riêng trên từng thẻ.

Mở `/chat` sau khi đăng nhập, hoặc nút “Trợ lý học tập” trên website và trang học. Khởi động lại Spring Boot để nạp controller/template mới.

Luồng: người dùng → máy chủ xác định tài khoản từ phiên đăng nhập → LearningSummaryService tính hồ sơ → LearningChatContextService lấy bài có mục tiêu và nội dung → Gemini trả JSON → máy chủ đối chiếu actionIds → giao diện hiển thị văn bản và nút.

Không cần train mô hình. Gemini nhận context mới ở mỗi lượt gồm mục tiêu, thống kê kết quả/năng lực, khóa đã đăng ký, tên/mục tiêu/nhãn bài và quyền truy cập. Không gửi email, mật khẩu, đáp án, lời giải hay câu hỏi của đề. Chỉ bài Reading/Listening có câu hỏi và Listening có media; Writing có đề được đưa vào catalog. Đây là kiểm tra có nội dung, không thay thế việc giáo viên duyệt chất lượng đề.

Catalog được xếp ưu tiên quyền truy cập, placement cho kỹ năng thiếu dữ liệu, practice đúng level và năng lực REVIEW. Prompt dùng kết quả này tư vấn, không coi điểm luyện tập là band IELTS. AI vẫn có thể trả lời không chính xác; nút chỉ được lấy từ danh sách máy chủ và không đổi quyền truy cập hoặc level. Trang hồ sơ là nơi xem căn cứ đánh giá.

Dùng GEMINI_API_KEY hiện có trong `.env`. Model mặc định `gemini-3.1-flash-lite`; có thể đổi bằng property `gemini.chat.model`. API key gửi trong header. Timeout kết nối 5 giây, đọc 30 giây. Structured response với responseMimeType + responseSchema; tham khảo tài liệu chính thức https://ai.google.dev/gemini-api/docs/structured-output .

Lịch sử tối đa 12 tin nhắn, giữ trong HTTP session và tách theo user ID. Mất phiên/restart thì lịch sử mất. “Bắt đầu cuộc trò chuyện mới” xóa lịch sử của người hiện tại. Không có lưu lịch sử lâu dài trong database ở bản đồ án này. Một phiên chỉ gửi một yêu cầu cùng lúc; đây không phải rate limit toàn hệ thống.

POST/DELETE dùng CSRF của Spring Security. API không nhận userId từ client. Output dùng textContent, không render HTML của AI; chỉ chấp nhận đường dẫn nội bộ xác minh. Lỗi nhà cung cấp/thiếu key/JSON lỗi trả fallback có ghi rõ AI chưa kết nối, không giả làm câu trả lời AI.

Kiểm thử LearningChatTests: render trang, giới hạn input, xác thực/tách lịch sử, xóa phiên, lọc catalog rỗng, không gửi đáp án, chặn ID/URL ngoài, fallback placement. LearningSummaryTests kiểm tra căn cứ đánh giá. Kiểm thử không gọi Gemini thật; cần thử một cuộc trò chuyện trên ứng dụng với API key để kiểm tra quota và kết nối thực tế.

Cải thiện tư vấn giá: context.courses có currentPrice VND, free và accessible riêng biệt. Giá khuyến mãi chỉ dùng trong khoảng thời gian đang áp dụng như catalog hiện tại; khóa thiếu giá giữ null. Khóa đánh giá đầu vào không đưa vào danh sách khóa mua. Prompt trả lời câu hỏi giá/mục tiêu trước, gợi ý đầu vào tùy chọn và ưu tiên nút khóa đã nhắc đến.
