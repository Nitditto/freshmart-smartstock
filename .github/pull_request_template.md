## Mô tả
<!-- Làm gì và vì sao? Liên kết user story / endpoint liên quan. -->

## Loại thay đổi
- [ ] feat
- [ ] fix
- [ ] docs
- [ ] refactor
- [ ] test

## Checklist
- [ ] Branch dạng `feature/{ten}`, PR vào `develop` (không vào `main`)
- [ ] Commit theo quy ước `feat: ...`, `fix: ...`, `docs: ...`, `refactor: ...`, `test: ...`
- [ ] `mvn -B verify` chạy xanh ở local
- [ ] Endpoint mới/sửa đã có `@Operation` trên Swagger và khớp `docs/api-contract.md`
- [ ] Có unit test cho logic nghiệp vụ mới
- [ ] KHÔNG commit secrets (`.env`, JWT secret, API key, connection string thật)
- [ ] Nếu thêm migration: đã dùng đúng số version được cấp phát trong `docs/git-flow.md`

## Cách kiểm thử
<!-- Các bước reviewer có thể làm theo (Swagger / test). -->

## Reviewer
<!-- Ít nhất 1 thành viên khác trong nhóm review. -->
