## Chức năng

Mô tả ngắn thay đổi và lý do.

## URL kiểm tra

Ghi URL, role và dữ liệu cần dùng để reviewer kiểm tra.

## Database

- [ ] Không thay đổi schema
- [ ] Có migration mới
- [ ] Entity khớp migration
- [ ] ERD/tài liệu đã cập nhật

## Kiểm tra

- [ ] Chạy bằng Java 21
- [ ] `.\mvnw.cmd clean test` thành công
- [ ] Controller không gọi Repository trực tiếp
- [ ] Không dùng `@RestController` cho giao diện
- [ ] Đã kiểm tra phân quyền
- [ ] Đã kiểm tra validation
- [ ] Có ảnh giao diện nếu thay đổi Thymeleaf/CSS
- [ ] Dùng layout/fragment và component trong `docs/UI_GUIDE.md`
- [ ] Đã kiểm tra trạng thái không có dữ liệu và có dữ liệu
- [ ] Đã kiểm tra giao diện mobile nếu thay đổi Thymeleaf/CSS
