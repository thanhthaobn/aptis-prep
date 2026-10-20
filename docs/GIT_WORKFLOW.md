# Git workflow — AptisPrep

## 1. Nhánh chính thức và quyền phối hợp

Repository: https://github.com/thanhthaobn/aptis-prep

```text
feature/thong ─┐
feature/thao  ─┼── Pull Request → develop ── Pull Request → main
feature/tram  ─┘                   tích hợp                  ổn định/demo
```

| Nhánh | Người dùng | Mục đích |
|---|---|---|
| main | Chủ repo duyệt/merge | Bản ổn định, không chứa phần đang làm dở |
| develop | Tích hợp cả nhóm qua PR | Nguồn cập nhật thường xuyên cho nhánh thành viên |
| feature/thong | Thông | Tài khoản/Goal, scoring/AI và grading/kết quả dữ liệu |
| feature/thao | Thảo | Lesson/Flashcard, lộ trình/checkpoint và giao diện kết quả |
| feature/tram | Trâm | Câu hỏi/media/đề, Placement và engine attempt |

Không push trực tiếp main/develop, không force push hoặc xóa các nhánh đang dùng chung. Nhánh cá nhân đã tạo sẵn, không tạo lại từ main. Một PR chỉ bao gồm phần nhỏ có thể kiểm tra; không chờ xong toàn bộ luồng.

Chủ repo phụ trách duyệt/merge develop → main; PR thành viên → develop cần người khác review, người merge theo thống nhất của nhóm. Tên nhánh không tự giới hạn quyền người push/merge. Nếu gói GitHub hỗ trợ, bảo vệ cả main/develop: yêu cầu PR, ít nhất một approval, check verify đạt, giải quyết conversation, không force push/xóa. Quy ước nhóm không thay thế việc bảo vệ kỹ thuật; repo private GitHub Free có giới hạn tính năng này. Đặt default branch develop nếu phù hợp và luôn kiểm tra base khi tạo PR.

## 2. Nhận nhánh đã tạo sẵn — một lần

Thành viên nhận lời mời Collaborator rồi clone repository vào thư mục mới, không clone đè project đang có:

```powershell
git clone https://github.com/thanhthaobn/aptis-prep.git
cd aptis-prep
git fetch origin
```

Mỗi người chỉ chạy lệnh của mình:

```powershell
# Thông
git switch --track origin/feature/thong
# Thảo
git switch --track origin/feature/thao
# Trâm
git switch --track origin/feature/tram
```

Nếu nhánh local đã tồn tại, dùng git switch feature/TEN thay vì --track. Người đang giữ project gốc không cần clone lại. Cấu hình Java 21/SQL Server theo [SETUP_LOCAL](SETUP_LOCAL.md); password/key riêng, không commit.

```powershell
git config user.name "Tên của bạn"
git config user.email "Email GitHub hoặc email noreply của bạn"
git branch --show-current
```

## 3. Trước khi code và trước khi mở PR

Ví dụ Thảo, các bạn khác thay bằng nhánh của mình. Kiểm tra git status trước: nếu có thay đổi chưa commit thì xử lý chúng trước khi chuyển/merge nhánh; không reset hoặc ghi đè để làm sạch.

```powershell
git status
git switch feature/thao
git fetch origin
git merge origin/develop
```

Không merge main thay develop trong công việc hằng ngày. Nếu có conflict, đọc cả hai phía và xử lý bằng IntelliJ; không chọn Accept Yours/Theirs toàn bộ một cách máy móc. Hoàn tất merge và commit nếu Git yêu cầu, sau đó kiểm thử.

## 4. Test → commit → push nhánh cá nhân

```powershell
.\mvnw.cmd -B -ntp clean verify
git status
git diff
```

Chọn đúng file trong IntelliJ Commit hoặc git add theo đường dẫn cụ thể. Kiểm tra nội dung staged trước khi commit:

```powershell
git diff --cached --stat
git diff --cached
git commit -m "feat: save lesson completion progress"
git push
```

Nếu nhánh chưa có upstream, ví dụ Thảo:

```powershell
git push -u origin feature/thao
```

Không commit application-dev.properties, .env, API key/token/password thật, target, IDE settings, DB backup hoặc upload runtime. Không dùng git add -f để ép file bị ignore vào commit. .gitignore không tự loại bí mật đã commit; nếu lộ credential thì thu hồi/đổi ngay và báo chủ repo, không chỉ xóa ở commit kế tiếp.

Commit message: feat/fix/docs/test/refactor, mô tả ngắn đúng nội dung. Không push --force khi bị từ chối; đọc lỗi và phối hợp trước.

## 5. PR nhánh thành viên → develop

Trên GitHub chọn Pull requests → New pull request:

```text
base: develop
compare: feature/thao  (hoặc nhánh đúng của người làm)
```

- Kiểm Files changed, điền mẫu PR: đã làm, cách chạy/test, migration, thay đổi contract và phần cần phối hợp.
- Mời người sở hữu dữ liệu/service liên quan review; tác giả không tự approve thay người khác.
- Workflow Java 21 build/job verify phải xanh; xử lý nhận xét/conflict, cập nhật develop và test lại trước merge.
- Khi cần sửa, commit/push tiếp cùng nhánh, PR tự cập nhật. Không tạo PR mới cho từng lần sửa review.
- Do nhóm tái sử dụng nhánh cá nhân dài hạn, dùng **Create a merge commit** cho các PR này để giữ quan hệ lịch sử; không xóa nhánh cá nhân sau merge. Nếu repo chưa cho phép, chủ repo bật Allow merge commits trong Settings → General → Pull Requests. Nếu chuyển sang nhánh ngắn hạn theo task trong tương lai thì chốt lại chiến lược trước.

Sau khi PR được merge, mỗi người đồng bộ develop vào nhánh của mình khi working tree sạch:

```powershell
git fetch origin
git switch feature/thao
git merge origin/develop
git push
```

Trước PR tiếp theo kiểm diff so với develop để không mang thay đổi ngoài dự kiến. Không sửa trực tiếp develop để xử lý lỗi tích hợp; sửa trên nhánh thành viên, mở PR nhỏ.

## 6. PR develop → main — chủ repo duyệt và merge

Chỉ khi mốc tích hợp đã chạy ổn, các PR dự kiến đã vào develop, Actions xanh và đã kiểm thử local/SQL Server cần thiết:

1. Chủ repo mở PR với **base main**, **compare develop**.
2. Ghi mốc chức năng đã hoàn thành, test, migration, giới hạn còn lại; xem lại Files changed. Nếu develop chứa phần dở thì chưa merge toàn bộ lên main.
3. Người khác review theo rule; chủ repo không tự approve PR do mình tạo. Sửa lỗi trên nhánh chức năng → develop, không code trực tiếp main.
4. Chủ repo dùng **Create a merge commit**, không Squash/Rebase cho develop → main, giữ lịch sử hai nhánh dài hạn. Không xóa develop.
5. Đồng bộ main trở lại develop nếu có khác biệt (đặc biệt merge commit hoặc sửa nóng): PR **base develop**, **compare main**, review/checks rồi merge commit. Nếu GitHub báo không có thay đổi thì bỏ qua. Thành viên lấy develop mới về theo mục 5.

Nếu buộc phải hotfix từ main, tạo nhánh riêng và PR vào main do chủ repo xử lý; sau đó phải đưa sửa lỗi trở lại develop trước khi các nhánh tiếp tục tích hợp. Không tự tạo quy trình hotfix bỏ qua review.

## 7. CI, database và Definition of Done

- .github/workflows/build.yml chạy Java 21 + Maven verify cho push/PR, gồm cả develop và nhánh cá nhân; không cần SQL Server/OpenAI key trên CI. Không thêm secret thật chỉ để test chạy.
- CI dùng H2/mock không thay kiểm migration/constraint/concurrency trên SQL Server thật. Actions xanh không tự chứng minh nghiệp vụ hoặc giao diện đúng.
- Không sửa migration đã merge/đã chạy, đặc biệt V001–V005. Migration mới lấy số theo thứ tự merge, không chia số theo người; PR đi cùng Entity/test và thông báo người sở hữu bảng.
- Kiểm migration trên DB thử nghiệm rỗng/đúng quy trình, không chạy test phá dữ liệu trên DB dùng chung. Cập nhật ERD/PROJECT_DECISIONS nếu đổi mô hình đã chốt.
- Mỗi bảng một Entity/Repository; gọi service theo [SERVICE_CONTRACTS](SERVICE_CONTRACTS.md). Người giữ nền review config/security/CSS/fragment/migration.
- Có test quyền/validation/NULL/lỗi và ca đồng thời liên quan, hướng dẫn kiểm tra, ảnh nếu sửa UI; không báo hoàn tất khi mới có fixture/schema.
- Phạm vi từng người theo [TEAM_ASSIGNMENT_V2](TEAM_ASSIGNMENT_V2.md), thứ tự theo [TEAM_WORK_CHECKLIST](TEAM_WORK_CHECKLIST.md). Tên branch trong file này thay tên branch gợi ý cũ ở các tài liệu đó.
