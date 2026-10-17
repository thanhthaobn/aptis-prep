# Git workflow

## Branch

Đã có `.github/workflows/build.yml`: GitHub chạy Java 21 + Maven verify khi push/PR, không cần SQL Server hoặc API key. Việc này chưa tự tạo repository hoặc bảo vệ main. Sau khi đưa repo lên GitHub, người giữ nền bật branch protection/ruleset: yêu cầu PR, ít nhất 1 review và job verify đạt; không cho push trực tiếp. CI H2 không thay kiểm migration SQL Server.

Không push trực tiếp vào `main`. Tạo branch theo chức năng:

```text
feature/account-goal-placement
feature/learning-checkpoint
feature/question-bank-scoring
feature/exam-grading-result
fix/short-description
docs/short-description
```

## Trước khi tạo Pull Request

```powershell
git switch main
git pull origin main
git switch <feature-branch>
git merge main
.\mvnw.cmd clean test
```

## Quy tắc database

- Không sửa migration đã merge.
- Thay đổi schema phải có migration mới.
- Migration và Entity phải nằm trong cùng Pull Request.
- Cập nhật ERD và `PROJECT_DECISIONS.md` nếu thay đổi mô hình đã chốt.
- Kiểm tra migration trên database rỗng trước khi yêu cầu review.

## Commit message

```text
feat: add lesson completion flow
fix: prevent duplicate exam submission
docs: update question ERD
test: add checkpoint service tests
refactor: separate grading service
```
