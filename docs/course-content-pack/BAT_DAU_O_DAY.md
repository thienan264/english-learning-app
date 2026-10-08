# BỘ NỘI DUNG ĐỒ ÁN — HƯỚNG DẪN NHẬP

Bộ tài liệu gốc phục vụ demo học tập và AI tư vấn. Không phải đề IELTS chính thức, chứng nhận CEFR hay cam kết tăng band. Người, tổ chức và biểu đồ là tình huống giả định. Audio là giọng tổng hợp.

## Nhập Reading/Listening

1. Mở DANH_SACH_NHAP.csv để ghép file với ID bài. Bài 57 đã nhập: không cần nhập lại hoặc ghi đè kết quả cũ.
2. Vào Soạn đề thi của đúng bài → chọn Word → Bắt đầu phân tích → kiểm tra câu hỏi, nhãn, đáp án, giải thích → Lưu.
3. Listening: tải thêm WAV cùng tên; transcript nằm riêng để kiểm tra, không dán transcript vào nội dung hiển thị cho học viên.
4. Bài luyện chọn PRACTICE; bài cuối chặng chọn FINAL. Các mục tiêu đã có trong file giáo viên và manifest.
5. Đề cuối chặng có 6 câu mỗi kỹ năng. Ngưỡng demo 8/10 nghĩa là cần 5/6 câu đúng, vì 4/6 chỉ được 6.7/10. Kiểm tra pass_score chưa đặt trên 10.

## Nhập Writing

Ba thư mục Writing có hướng dẫn riêng. Không đưa chúng vào bộ quét Reading/Listening. Tạo Task 1, tải ảnh PNG; tạo Task 2; ghép đúng hai task vào bài. Các file instruction.txt chỉ có đề, không kèm đáp án hay dàn ý.

## Đánh giá đầu vào — khóa ID 12

Khóa Đánh giá đầu vào đã được tạo nhưng chưa có bài. Tạo một chương và hai bài MOCK_TEST: Reading và Listening, rồi quét hai Word tương ứng. Vai trò PLACEMENT. Mỗi bài có 9 câu, chia 3 câu Beginner, 3 Intermediate, 3 Advanced. Không gán tất cả câu cùng level; tag LEVEL đã có trong Word.
Level bài để Chưa phân loại vì đề trộn mức; mục tiêu là gợi ý mức bắt đầu của từng kỹ năng. Kết quả 3 câu/mức chỉ là bằng chứng demo còn ít. Có thể đề xuất Intermediate khi đạt ít nhất 2/3 Beginner, và Advanced khi đồng thời đạt ít nhất 2/3 Beginner và 2/3 Intermediate. Nếu khó đúng nhưng dễ sai, đề xuất kiểm tra bổ sung. Đây mới là quy tắc dự kiến: hệ thống chưa tự xếp level/chat.

## Phạm vi và lưu ý

Bộ gồm 24 bài luyện của 8 khóa, 6 đề cuối chặng và 2 đề đầu vào. Trong 24 bài luyện có 3 bài Writing; Reading/Listening có tổng 159 câu. Các bài Reading/Listening ngắn đánh giá mục tiêu cụ thể, không quy đổi band IELTS.
Các bài demo cũ khác trong database không nằm trong bộ này; chưa cần xóa. Khi nối AI, chỉ lấy nội dung đã rà soát/phân loại, không mặc định coi mọi bài cũ là phù hợp.
Bài 67 đang có kỹ năng Listening trong database nên nội dung được thiết kế thành nghe paraphrase, không yêu cầu sửa loại bài.
Đề cuối chặng và đầu vào dùng nội dung mới; không lấy lại câu luyện. Không cần tạo thêm khóa ngoài khóa ID12 đã có.

## Bảng ghép nhanh

| Khóa ID | Bài ID | Bài | File Word |
|---|---|---|---|
| 5 | 57 | Đọc hồ sơ cá nhân | [01_doc_ho_so_ca_nhan.docx](05_Tieng_Anh_nen_tang/01_doc_ho_so_ca_nhan.docx) |
| 5 | 58 | Nghe lịch sinh hoạt | [02_nghe_lich_sinh_hoat.docx](05_Tieng_Anh_nen_tang/02_nghe_lich_sinh_hoat.docx) |
| 5 | 59 | Đọc lịch và thông báo | [03_doc_lich_thong_bao.docx](05_Tieng_Anh_nen_tang/03_doc_lich_thong_bao.docx) |
| 2 | 60 | Đọc thực đơn và bảng giá | [01_doc_thuc_don.docx](02_Tieng_Anh_hang_ngay_1/01_doc_thuc_don.docx) |
| 2 | 61 | Đọc tin nhắn hẹn gặp | [02_doc_tin_nhan.docx](02_Tieng_Anh_hang_ngay_1/02_doc_tin_nhan.docx) |
| 2 | 62 | Nghe gọi món | [03_nghe_goi_mon.docx](02_Tieng_Anh_hang_ngay_1/03_nghe_goi_mon.docx) |
| 9 | 63 | Đọc email về chuyến đi | [01_doc_email_chuyen_di.docx](09_Tieng_Anh_hang_ngay_2/01_doc_email_chuyen_di.docx) |
| 9 | 64 | Nghe kế hoạch cuối tuần | [02_nghe_ke_hoach_cuoi_tuan.docx](09_Tieng_Anh_hang_ngay_2/02_nghe_ke_hoach_cuoi_tuan.docx) |
| 9 | 65 | Đọc quy định nơi công cộng | [03_doc_quy_dinh.docx](09_Tieng_Anh_hang_ngay_2/03_doc_quy_dinh.docx) |
| 10 | 66 | Hiểu quan hệ nguyên nhân và đối lập | [01_nguyen_nhan_doi_lap.docx](10_Ngu_phap_tu_vung_ung_dung/01_nguyen_nhan_doi_lap.docx) |
| 10 | 67 | Nhận diện diễn đạt tương đương | [02_nghe_paraphrase.docx](10_Ngu_phap_tu_vung_ung_dung/02_nghe_paraphrase.docx) |
| 10 | 68 | Đoán nghĩa từ trong ngữ cảnh | [03_doan_nghia_tu.docx](10_Ngu_phap_tu_vung_ung_dung/03_doan_nghia_tu.docx) |
| 4 | 69 | Đọc email công việc | [01_doc_email_cong_viec.docx](04_Tieng_Anh_nguoi_di_lam/01_doc_email_cong_viec.docx) |
| 4 | 70 | Nghe sắp xếp cuộc họp | [02_nghe_sap_xep_cuoc_hop.docx](04_Tieng_Anh_nguoi_di_lam/02_nghe_sap_xep_cuoc_hop.docx) |
| 4 | 71 | Đọc đề xuất cải thiện công việc | [03_doc_de_xuat_cai_thien.docx](04_Tieng_Anh_nguoi_di_lam/03_doc_de_xuat_cai_thien.docx) |
| 11 | 72 | Reading: Paraphrase và thông tin chi tiết | [01_reading_paraphrase.docx](11_IELTS_Ky_nang_cot_loi/01_reading_paraphrase.docx) |
| 11 | 73 | Listening: Nhận diện thông tin được sửa lại | [02_listening_sua_thong_tin.docx](11_IELTS_Ky_nang_cot_loi/02_listening_sua_thong_tin.docx) |
| 7 | 75 | Reading: Quan điểm và suy luận | [01_reading_quan_diem_suy_luan.docx](07_IELTS_Academic_nang_cao/01_reading_quan_diem_suy_luan.docx) |
| 7 | 76 | Listening: Theo dõi lập luận | [02_listening_theo_doi_lap_luan.docx](07_IELTS_Academic_nang_cao/02_listening_theo_doi_lap_luan.docx) |
| 8 | 78 | Reading: Phân biệt thông tin đúng, sai, không có | [01_reading_true_false_not_given.docx](08_IELTS_Luyen_de_sua_diem_yeu/01_reading_true_false_not_given.docx) |
| 8 | 79 | Listening: Ghép ý với người nói | [02_listening_ghep_nguoi_noi.docx](08_IELTS_Luyen_de_sua_diem_yeu/02_listening_ghep_nguoi_noi.docx) |
| 5 | 89 | Reading — Kiểm tra cuối chặng Beginner | [04_cuoi_chang_reading.docx](05_Tieng_Anh_nen_tang/04_cuoi_chang_reading.docx) |
| 5 | 90 | Listening — Kiểm tra cuối chặng Beginner | [05_cuoi_chang_listening.docx](05_Tieng_Anh_nen_tang/05_cuoi_chang_listening.docx) |
| 11 | 91 | Reading — Kiểm tra cuối chặng Intermediate | [04_cuoi_chang_reading.docx](11_IELTS_Ky_nang_cot_loi/04_cuoi_chang_reading.docx) |
| 11 | 92 | Listening — Kiểm tra cuối chặng Intermediate | [05_cuoi_chang_listening.docx](11_IELTS_Ky_nang_cot_loi/05_cuoi_chang_listening.docx) |
| 7 | 87 | Reading — Kiểm tra cuối chặng Advanced | [04_cuoi_chang_reading.docx](07_IELTS_Academic_nang_cao/04_cuoi_chang_reading.docx) |
| 7 | 88 | Listening — Kiểm tra cuối chặng Advanced | [05_cuoi_chang_listening.docx](07_IELTS_Academic_nang_cao/05_cuoi_chang_listening.docx) |
| 12 | Tạo mới | Đánh giá đầu vào — Reading | [01_danh_gia_dau_vao_reading.docx](12_Danh_gia_dau_vao/01_danh_gia_dau_vao_reading.docx) |
| 12 | Tạo mới | Đánh giá đầu vào — Listening | [02_danh_gia_dau_vao_listening.docx](12_Danh_gia_dau_vao/02_danh_gia_dau_vao_listening.docx) |
| 11 | 74 | Writing Task 2: Trình bày quan điểm | [03_writing_quan_diem.docx](11_IELTS_Ky_nang_cot_loi/03_writing_quan_diem.docx) |
| 7 | 77 | Writing Task 1: So sánh dữ liệu | [03_writing_so_sanh_du_lieu.docx](07_IELTS_Academic_nang_cao/03_writing_so_sanh_du_lieu.docx) |
| 8 | 80 | Writing Task 2: Đánh giá lợi ích và bất lợi | [03_writing_loi_ich_bat_loi.docx](08_IELTS_Luyen_de_sua_diem_yeu/03_writing_loi_ich_bat_loi.docx) |