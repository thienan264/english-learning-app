# Khuôn nhập đề tự động

Giữ nguyên các khối [PASSAGE], [TITLE], [CONTENT], [GROUP], [TYPE], [INSTRUCTION], [Q], [OPT], [ANS] của mẫu gốc. Mỗi tag bắt đầu một dòng riêng. Không đổi tên tag hoặc dùng đánh số tự động của Word cho các dòng tag.

Sau mỗi [Q], thêm:

```
[LEVEL] BEGINNER
[COMPETENCY] DETAIL
```

Sau [ANS], thêm:

```
[EXPLANATION] Bằng chứng và lý do chọn đáp án.
```

EXPLANATION có thể nhiều dòng, kết thúc khi gặp tag tiếp theo. LEVEL và COMPETENCY chỉ áp dụng cho câu hiện tại; không tự truyền sang câu sau. File cũ thiếu tag vẫn nhập được và để nhãn trống.

LEVEL: BEGINNER, INTERMEDIATE, ADVANCED.

| COMPETENCY | Ý nghĩa |
|---|---|
| DETAIL | Thông tin chi tiết |
| MAIN_IDEA | Ý chính |
| PARAPHRASE | Diễn đạt tương đương |
| INFERENCE | Suy luận |
| AUTHOR_ATTITUDE | Quan điểm/thái độ |
| ARGUMENT | Theo dõi lập luận |
| CORRECTION | Thông tin được sửa lại |
| SPEAKER_MATCH | Gán ý kiến cho người nói |
| TRUE_FALSE_NOT_GIVEN | Phân biệt đúng/sai/không được đề cập |

Trắc nghiệm một lựa chọn: [ANS] B. Nhiều lựa chọn: [ANS] B, C. Mỗi [OPT] bắt đầu bằng chữ cái, ví dụ [OPT] A. Da Nang.

Tải Word mẫu Reading_Beginner_Doc_ho_so_ca_nhan.docx vào bài “Đọc hồ sơ cá nhân”, bấm phân tích, kiểm tra 1 passage/1 nhóm/5 câu, nhãn Beginner/DETAIL, đáp án B C A D B và giải thích, rồi lưu. Phân tích chưa tự lưu hoặc thay thế bài hiện tại. Đây là cùng nội dung bài mẫu cũ, không phải đề cuối khóa mới.

Bộ quét hiện hỗ trợ DOCX và PDF có văn bản; không hỗ trợ ảnh/PDF scan. Với Listening, audio vẫn cần tải riêng. Mục tiêu và vai trò bài vẫn khai báo ở trang Mục tiêu học tập. Khuôn này dành cho Reading/Listening, chưa áp dụng cho bộ tạo Writing.
