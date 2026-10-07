# Xác thực email đăng ký EngMaster AI

Ứng dụng gửi mã gồm 6 chữ số qua SMTP Gmail. Tài khoản mới chỉ được lưu sau khi xác nhận mã đúng trong cùng phiên trình duyệt. Không thay đổi khả năng đăng nhập của các tài khoản đã tồn tại.

## Cấu hình Gmail

Địa chỉ gửi đã được cấu hình trong `.env`: `phamanmap2004@gmail.com`.

1. Bật xác minh 2 bước cho tài khoản Google gửi mail.
2. Mở https://myaccount.google.com/apppasswords và tạo mật khẩu ứng dụng cho EngMaster AI.
3. Điền mật khẩu ứng dụng vào `MAIL_PASSWORD` trong `.env` (16 ký tự, bỏ khoảng trắng). Không dùng mật khẩu đăng nhập Gmail thông thường, không gửi mật khẩu vào chat và không commit `.env`.
4. Khởi động lại ứng dụng.

```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=phamanmap2004@gmail.com
MAIL_FROM=phamanmap2004@gmail.com
MAIL_PASSWORD=
```

Hướng dẫn chính thức: https://support.google.com/accounts/answer/185833

## Kiểm tra gửi email thật

Mở `/register`, điền email nhận và nhấn **Gửi mã**. Kiểm tra hộp thư đến/thư rác, nhập mã và hoàn tất đăng ký. Mã không xuất hiện trong phản hồi API hoặc log ứng dụng. Nếu chưa cấu hình hoặc SMTP không gửi được, giao diện báo lỗi và không tạo tài khoản.

## Quy tắc

- Mã có hiệu lực 10 phút, lưu dưới dạng BCrypt, chỉ dùng một lần.
- Mã gắn với email và phiên trình duyệt; đổi email phải gửi mã mới.
- Gửi lại cách nhau ít nhất 60 giây; tối đa 5 lần/email/giờ và 10 lần/phiên/giờ.
- Nhập sai tối đa 5 lần cho mỗi mã; gửi lại làm mã cũ mất hiệu lực.
- Xác nhận mã và lưu người dùng trong cùng giao dịch database.
- Bảng `email_verifications` được tạo bởi cấu hình Hibernate `ddl-auto=update` hiện tại.
- Đăng ký và gửi mã vẫn yêu cầu CSRF.

## Kiểm tra tự động

Các test dùng mail sender giả lập, không gửi email thật. Kiểm tra mã đúng/sai, hết hạn, dùng lại, sai phiên/email, giới hạn gửi lại, SMTP lỗi và endpoint không có CSRF.
