# KẾ HOẠCH REFACTOR TOÀN DIỆN HỆ THỐNG APTISPREP
> **Kiến trúc đích**: Feature-Driven Package Architecture (`com.aptisprep.mvc.<feature>...`)
> **Baseline Tests**: **345 run, 0 failures, 0 errors, 6 skipped** (`BUILD SUCCESS`)
> **Ngày lập kế hoạch**: 10/11/2026 — Trạng thái: **Bước 0: Phân tích & Đề xuất (Chờ phê duyệt)**

---

## MỤC LỤC
1. [PHÂN LOẠI TOÀN BỘ CLASS TRONG service/ (BƯỚC 0 BỔ SUNG)](#1-phân-loại-toàn-bộ-class-trong-service-bước-0-bổ-sung)
   - [Bảng phân loại chi tiết 89 class/interface/record](#bảng-phân-loại-chi-tiết-89-classinterfacerecord)
   - [Phân tích 5 class không chắc chắn & đề xuất nhóm](#phân-tích-5-class-không-chắc-chắn--đề-xuất-nhóm)
2. [BẢNG ÁNH XẠ TOÀN DIỆN 249 FILE JAVA HIỆN TẠI -> PACKAGE ĐÍCH](#2-bảng-ánh-xạ-toàn-diện-249-file-java-hiện-tại---package-đích)
3. [DANH SÁCH VI PHẠM QUY TẮC THEO TỪNG CONTROLLER & SERVICE](#3-danh-sách-vi-phạm-quy-tắc-theo-từng-controller--service)
   - [Vi phạm ở Controller](#vi-phạm-ở-controller)
   - [Vi phạm ở Service](#vi-phạm-ở-service)
4. [ĐỒ THỊ PHỤ THUỘC GIỮA CÁC FEATURE & CÁC VÒNG CẦN PHÁ](#4-đồ-thị-phụ-thuộc-giữa-các-feature--các-vòng-cần-phá)
5. [DANH SÁCH NGOẠI LỆ AJAX / STREAM BẮT BUỘC PHẢI GIỮ](#5-danh-sách-ngoại-lệ-ajax--stream-bắt-buộc-phải-giữ)
6. [KẾ HOẠCH THỰC HIỆN CÁC BƯỚC TIẾP THEO (BƯỚC 1 -> BƯỚC 6)](#6-kế-hoạch-thực-hiện-các-bước-tiếp-theo-bước-1---bước-6)

---

## 1. PHÂN LOẠI TOÀN BỘ CLASS TRONG service/ (BƯỚC 0 BỔ SUNG)

### Tiêu chí phân loại:
*   **Nhóm 1 (SERVICE THẬT)**: Có use-case được Controller hoặc feature khác gọi $\rightarrow$ Cấu trúc gồm `interface XxxService` + `impl/XxxServiceImpl` (annotation `@Service` CHỈ đặt ở Impl). Các interface `*Contract` đổi thành `*Service`.
*   **Nhóm 2 (LISTENER)**: Lắng nghe Spring ApplicationEvent $\rightarrow$ Đổi tên `*Listener`, chuyển vào `<feature>/service/impl`, giữ nguyên `@EventListener` và `@Transactional`.
*   **Nhóm 3 (HELPER)**: Chỉ được Impl cùng feature gọi (validator, writer, factory, policy, formatter, calculator, hasher, coverage, reader nội bộ) $ightarrow$ Package-private trong `<feature>/service/impl`, KHÔNG gắn `@Service` (dùng `@Component` nếu cần inject), Controller không được tham chiếu.
*   **Nhóm 4 (MODEL / VALUE / DTO)**: Record hoặc value object $ightarrow$ Chuyển thành DTO đặt tại `<feature>/dto`, đặt tên hậu tố `DTO`.
*   **Nhóm 5 (VIEW HELPER)**: Bean giao diện cho Thymeleaf (`ResultLabels` - bean `@resultLabels`) $ightarrow$ Đặt vào `common/util`, giữ nguyên tên bean.
*   **Nhóm 6 (HẠ TẦNG / INTEGRATION)**: Tích hợp thư viện ngoài hoặc API ngoài (`AudioDurationService`, `GeminiGradingService`).

### Bảng phân loại chi tiết 89 class/interface/record

| Tên class hiện tại | Nhóm | Caller (Controller / Feature khác / Nội bộ / Test) | Tên mới | Package đích |
|---|:---:|---|---|---|
| `AccountAccessContract` | **1** (SERVICE THẬT (Interface)) | Other: service.account.AccountAccessService, service.learning.LearningPathService, service.learning.LessonRecommendationServiceImpl, service.placement.PlacementServiceImpl<br>Test: 2 tests | `AccountAccessService` | `com.aptisprep.mvc.common.service` |
| `AccountAccessService` | **1** (SERVICE THẬT) | Ctrl: StudentAccountController, StudentExamCatalogController, StudentExamController<br>Other: service.placement.PlacementOrchestrationService<br>Same: AccountAccessServiceImpl, AccountProfileServiceImpl, LearningActivityServiceImpl<br>Test: 5 tests | `AccountAccessService` | `com.aptisprep.mvc.account.service` |
| `AccountAccessServiceImpl` | **1** (SERVICE THẬT (Impl)) | Test: 1 tests | `AccountAccessServiceImpl` | `com.aptisprep.mvc.account.service.impl` |
| `AccountProfileService` | **1** (SERVICE THẬT) | Ctrl: StudentAccountController, StudentDashboardController<br>Same: AccountProfileServiceImpl<br>Test: 2 tests | `AccountProfileService` | `com.aptisprep.mvc.account.service` |
| `AccountProfileServiceImpl` | **1** (SERVICE THẬT (Impl)) | Test: 1 tests | `AccountProfileServiceImpl` | `com.aptisprep.mvc.account.service.impl` |
| `AdminDashboardService` | **1** (SERVICE THẬT) | Ctrl: AdminDashboardController<br>Same: AdminDashboardServiceImpl | `AdminDashboardService` | `com.aptisprep.mvc.admin.service` |
| `AdminDashboardServiceImpl` | **1** (SERVICE THẬT (Impl)) | Chưa có caller trực tiếp | `AdminDashboardServiceImpl` | `com.aptisprep.mvc.admin.service.impl` |
| `AdminExamServiceImpl` | **1** (SERVICE THẬT (Impl)) | Chưa có caller trực tiếp | `AdminExamServiceImpl` | `com.aptisprep.mvc.admin.service.impl` |
| `AiAssessmentContract` | **1** (SERVICE THẬT (Interface)) | Ctrl: AiAssessmentController<br>Other: service.exam.AttemptGradingOrchestrationService, service.grading.AiAssessmentService, service.grading.AttemptFinalizationService, service.learning.LessonTeachingService<br>Test: 4 tests | `AiAssessmentService` | `com.aptisprep.mvc.common.service` |
| `AttemptFinalizationService` | **1** (SERVICE THẬT) | Other: service.exam.AttemptGradingOrchestrationService, service.exam.ExamAttemptService<br>Test: 4 tests | `AttemptFinalizationService` | `com.aptisprep.mvc.grading.service` |
| `AttemptOutboxService` | **1** (SERVICE THẬT) | Other: service.grading.AiAssessmentService, service.grading.AttemptFinalizationService, service.placement.PlacementOrchestrationService<br>Same: AttemptGradingOrchestrationService, ExamAttemptService<br>Test: 9 tests | `AttemptOutboxService` | `com.aptisprep.mvc.exam.service` |
| `AudioUploadService` | **1** (SERVICE THẬT) | Other: service.exam.ExamAnswerWriter<br>Same: AudioUploadServiceImpl<br>Test: 1 tests | `AudioUploadService` | `com.aptisprep.mvc.storage.service` |
| `AudioUploadServiceImpl` | **1** (SERVICE THẬT (Impl)) | Test: 1 tests | `AudioUploadServiceImpl` | `com.aptisprep.mvc.storage.service.impl` |
| `CatalogService` | **1** (SERVICE THẬT) | Ctrl: AdminCatalogController<br>Same: CatalogServiceImpl<br>Test: 2 tests | `CatalogService` | `com.aptisprep.mvc.admin.service` |
| `CatalogServiceImpl` | **1** (SERVICE THẬT (Impl)) | Chưa có caller trực tiếp | `CatalogServiceImpl` | `com.aptisprep.mvc.admin.service.impl` |
| `CertificateSchemeContract` | **1** (SERVICE THẬT (Interface)) | Other: service.admin.ExamDefinitionValidator, service.exam.ExamStartValidator, service.scoring.CertificateSchemeService<br>Test: 1 tests | `CertificateSchemeService` | `com.aptisprep.mvc.common.service` |
| `ExamAttemptContract` | **1** (SERVICE THẬT (Interface)) | Ctrl: AdminResultController, StudentAccountController, StudentDashboardController, StudentExamCatalogController<br>Other: service.account.AccountAccessServiceImpl, service.admin.AdminDashboardServiceImpl, service.admin.StudentManagementServiceImpl, service.exam.ExamAttemptService, service.learning.CheckpointStatusReader, service.learning.LearningPathService, service.placement.PlacementServiceImpl<br>Test: 3 tests | `ExamAttemptService` | `com.aptisprep.mvc.common.service` |
| `ExamAttemptService` | **1** (SERVICE THẬT) | Ctrl: AdminResultController, StudentAccountController, StudentExamCatalogController, StudentExamController<br>Test: 5 tests | `ExamAttemptService` | `com.aptisprep.mvc.exam.service` |
| `ExamCatalogContract` | **1** (SERVICE THẬT (Interface)) | Other: service.catalog.PublishedExamCatalogService, service.learning.CheckpointStatusReader, service.learning.LearningPathService, service.placement.PlacementServiceImpl<br>Test: 1 tests | `ExamCatalogService` | `com.aptisprep.mvc.common.service` |
| `FlashcardSetService` | **1** (SERVICE THẬT) | Ctrl: AdminFlashcardController, AdminLessonController, StudentFlashcardController, StudentLearningPathController, StudentLessonController<br>Same: FlashcardSetServiceImpl<br>Test: 3 tests | `FlashcardSetService` | `com.aptisprep.mvc.learning.service` |
| `FlashcardSetServiceImpl` | **1** (SERVICE THẬT (Impl)) | Same: FlashcardSetService | `FlashcardSetServiceImpl` | `com.aptisprep.mvc.learning.service.impl` |
| `LearningActivityService` | **1** (SERVICE THẬT) | Ctrl: StudentAccountController, StudentDashboardController<br>Same: LearningActivityServiceImpl<br>Test: 1 tests | `LearningActivityService` | `com.aptisprep.mvc.account.service` |
| `LearningActivityServiceImpl` | **1** (SERVICE THẬT (Impl)) | Chưa có caller trực tiếp | `LearningActivityServiceImpl` | `com.aptisprep.mvc.account.service.impl` |
| `LearningPathContract` | **1** (SERVICE THẬT (Interface)) | Other: service.account.GoalPathListener, service.admin.StudentManagementServiceImpl, service.learning.LearningPathService, service.placement.PlacementOrchestrationService<br>Test: 1 tests | `LearningPathService` | `com.aptisprep.mvc.common.service` |
| `LearningPathService` | **1** (SERVICE THẬT) | Ctrl: StudentDashboardController, StudentLearningPathController<br>Same: LessonService<br>Test: 6 tests | `LearningPathService` | `com.aptisprep.mvc.learning.service` |
| `LessonListeningService` | **1** (SERVICE THẬT) | Ctrl: StudentLessonController<br>Same: LessonListeningServiceImpl, LessonTeachingService<br>Test: 2 tests | `LessonListeningService` | `com.aptisprep.mvc.learning.service` |
| `LessonListeningServiceImpl` | **1** (SERVICE THẬT (Impl)) | Test: 2 tests | `LessonListeningServiceImpl` | `com.aptisprep.mvc.learning.service.impl` |
| `LessonRecommendationService` | **1** (SERVICE THẬT) | Ctrl: StudentDashboardController<br>Same: LessonRecommendationServiceImpl<br>Test: 1 tests | `LessonRecommendationService` | `com.aptisprep.mvc.learning.service` |
| `LessonRecommendationServiceImpl` | **1** (SERVICE THẬT (Impl)) | Test: 1 tests | `LessonRecommendationServiceImpl` | `com.aptisprep.mvc.learning.service.impl` |
| `LessonService` | **1** (SERVICE THẬT) | Ctrl: AdminFlashcardController, AdminLessonController, LessonAudioController, StudentAccountController, StudentExamCatalogController, StudentFlashcardController, StudentLearningPathController, StudentLessonController<br>Same: FlashcardSetServiceImpl, LearningPathService<br>Test: 4 tests | `LessonService` | `com.aptisprep.mvc.learning.service` |
| `LessonTeachingService` | **1** (SERVICE THẬT) | Ctrl: LessonAudioController, StudentLessonController<br>Same: LessonCheckService, LessonService<br>Test: 4 tests | `LessonTeachingService` | `com.aptisprep.mvc.learning.service` |
| `LocalStorageServiceImpl` | **1** (SERVICE THẬT (Impl)) | Test: 1 tests | `LocalStorageServiceImpl` | `com.aptisprep.mvc.storage.service.impl` |
| `PasswordService` | **1** (SERVICE THẬT) | Ctrl: PasswordController<br>Same: PasswordServiceImpl | `PasswordService` | `com.aptisprep.mvc.auth.service` |
| `PasswordServiceImpl` | **1** (SERVICE THẬT (Impl)) | Chưa có caller trực tiếp | `PasswordServiceImpl` | `com.aptisprep.mvc.auth.service.impl` |
| `PlacementService` | **1** (SERVICE THẬT) | Ctrl: StudentAccountController<br>Same: PlacementServiceImpl<br>Test: 1 tests | `PlacementService` | `com.aptisprep.mvc.placement.service` |
| `PlacementServiceImpl` | **1** (SERVICE THẬT (Impl)) | Test: 1 tests | `PlacementServiceImpl` | `com.aptisprep.mvc.placement.service.impl` |
| `ScoringAdminService` | **1** (SERVICE THẬT) | Ctrl: AdminScoringController<br>Same: ScoringAdminServiceImpl<br>Test: 2 tests | `ScoringAdminService` | `com.aptisprep.mvc.scoring.service` |
| `ScoringAdminServiceImpl` | **1** (SERVICE THẬT (Impl)) | Test: 1 tests | `ScoringAdminServiceImpl` | `com.aptisprep.mvc.scoring.service.impl` |
| `ScoringGuideService` | **1** (SERVICE THẬT) | Ctrl: StudentAccountController, StudentExamCatalogController<br>Same: ScoringGuideServiceImpl<br>Test: 1 tests | `ScoringGuideService` | `com.aptisprep.mvc.scoring.service` |
| `ScoringGuideServiceImpl` | **1** (SERVICE THẬT (Impl)) | Test: 1 tests | `ScoringGuideServiceImpl` | `com.aptisprep.mvc.scoring.service.impl` |
| `ScoringPolicyContract` | **1** (SERVICE THẬT (Interface)) | Other: service.exam.ExamAttemptService, service.grading.AttemptFinalizationService, service.grading.ObjectiveGradingService, service.scoring.ScoringPolicyService, service.scoring.ScoringPolicyServiceImpl<br>Test: 3 tests | `ScoringPolicyService` | `com.aptisprep.mvc.common.service` |
| `ScoringPolicyServiceImpl` | **1** (SERVICE THẬT (Impl)) | Test: 2 tests | `ScoringPolicyServiceImpl` | `com.aptisprep.mvc.scoring.service.impl` |
| `ScoringSchemeUsageContract` | **1** (SERVICE THẬT (Interface)) | Other: service.catalog.ScoringSchemeUsageService, service.scoring.ScoringAdminServiceImpl<br>Test: 2 tests | `ScoringSchemeUsageService` | `com.aptisprep.mvc.common.service` |
| `StorageService` | **1** (SERVICE THẬT) | Ctrl: LessonAudioController<br>Other: service.admin.CatalogServiceImpl, service.admin.QuestionMediaWriter, service.exam.ExamAttemptService, service.exam.ExamStartValidator, service.grading.AiAssessmentService, service.learning.LessonService, service.learning.LessonTeachingService<br>Same: AudioDurationService, AudioUploadServiceImpl, LocalStorageService<br>Test: 11 tests | `StorageService` | `com.aptisprep.mvc.storage.service` |
| `StudentExamCatalogService` | **1** (SERVICE THẬT) | Ctrl: StudentExamCatalogController<br>Same: StudentExamCatalogServiceImpl<br>Test: 5 tests | `StudentExamCatalogService` | `com.aptisprep.mvc.student.service` |
| `StudentExamCatalogServiceImpl` | **1** (SERVICE THẬT (Impl)) | Test: 2 tests | `StudentExamCatalogServiceImpl` | `com.aptisprep.mvc.student.service.impl` |
| `StudentManagementService` | **1** (SERVICE THẬT) | Ctrl: AdminStudentController<br>Same: AdminDashboardServiceImpl, StudentManagementServiceImpl | `StudentManagementService` | `com.aptisprep.mvc.admin.service` |
| `StudentManagementServiceImpl` | **1** (SERVICE THẬT (Impl)) | Chưa có caller trực tiếp | `StudentManagementServiceImpl` | `com.aptisprep.mvc.admin.service.impl` |
| `UserService` | **1** (SERVICE THẬT) | Ctrl: AiAssessmentController, AuthController<br>Same: UserServiceImpl<br>Test: 1 tests | `UserService` | `com.aptisprep.mvc.auth.service` |
| `UserServiceImpl` | **1** (SERVICE THẬT (Impl)) | Chưa có caller trực tiếp | `UserServiceImpl` | `com.aptisprep.mvc.auth.service.impl` |
| `AttemptGradingOrchestrationService` | **2** (LISTENER) | Test: 2 tests | `AttemptGradingOrchestrationListener` | `com.aptisprep.mvc.exam.service.impl` |
| `GoalPathListener` | **2** (LISTENER) | Chưa có caller trực tiếp | `GoalPathListener` | `com.aptisprep.mvc.account.service.impl` |
| `PlacementOrchestrationService` | **2** (LISTENER) | Test: 1 tests | `PlacementOrchestrationListener` | `com.aptisprep.mvc.placement.service.impl` |
| `AdminExamService` | **3** (HELPER) | Same: AdminExamServiceImpl, CatalogServiceImpl<br>Test: 1 tests | `AdminExamService` | `com.aptisprep.mvc.admin.service.impl` |
| `AiAssessmentService` | **3** (HELPER) | Test: 2 tests | `AiAssessmentService` | `com.aptisprep.mvc.grading.service.impl` |
| `AiProcessingPolicy` | **3** (HELPER) | Same: AiAssessmentService | `AiProcessingPolicy` | `com.aptisprep.mvc.grading.service.impl` |
| `CertificateSchemeService` | **3** (HELPER) | Test: 2 tests | `CertificateSchemeService` | `com.aptisprep.mvc.scoring.service.impl` |
| `CheckpointScorePolicy` | **3** (HELPER) | Other: service.exam.AttemptResultReader, service.grading.AttemptFinalizationService, service.grading.ObjectiveGradingService<br>Test: 1 tests | `CheckpointScorePolicy` | `com.aptisprep.mvc.scoring.service.impl` |
| `CheckpointStartValidator` | **3** (HELPER) | Same: ExamAttemptService | `CheckpointStartValidator` | `com.aptisprep.mvc.exam.service.impl` |
| `ExamAnswerWriter` | **3** (HELPER) | Same: ExamAttemptService | `ExamAnswerWriter` | `com.aptisprep.mvc.exam.service.impl` |
| `ExamDefinitionValidator` | **3** (HELPER) | Same: AdminExamServiceImpl, CatalogServiceImpl | `ExamDefinitionValidator` | `com.aptisprep.mvc.admin.service.impl` |
| `ExamGradingPayloadFactory` | **3** (HELPER) | Same: AttemptOutboxService, ExamAttemptService<br>Test: 1 tests | `ExamGradingPayloadFactory` | `com.aptisprep.mvc.exam.service.impl` |
| `ExamSnapshotService` | **3** (HELPER) | Other: service.admin.AdminExamServiceImpl<br>Same: AttemptResultReader, ExamAnswerWriter, ExamAttemptService, ExamGradingPayloadFactory | `ExamSnapshotFactory` | `com.aptisprep.mvc.exam.service.impl` |
| `ExamStartValidator` | **3** (HELPER) | Other: service.admin.AdminExamServiceImpl, service.placement.PlacementExamCatalogService, service.student.StudentExamCatalogServiceImpl<br>Same: CheckpointStartValidator, ExamAttemptService<br>Test: 3 tests | `ExamStartValidator` | `com.aptisprep.mvc.exam.service.impl` |
| `GradingContextFormatter` | **3** (HELPER) | Other: service.exam.AttemptGradingOrchestrationService, service.exam.ExamAttemptService, service.exam.ExamGradingPayloadFactory, service.grading.AttemptFinalizationService, service.grading.ObjectiveGradingService<br>Test: 1 tests | `GradingContextFormatter` | `com.aptisprep.mvc.contract.service.impl` |
| `LearningProgressCalculator` | **3** (HELPER) | Same: LearningPathService<br>Test: 1 tests | `LearningProgressCalculator` | `com.aptisprep.mvc.learning.service.impl` |
| `LocalStorageService` | **3** (HELPER) | Same: LocalStorageServiceImpl<br>Test: 1 tests | `LocalStorageService` | `com.aptisprep.mvc.storage.service.impl` |
| `PasswordHasher` | **3** (HELPER) | Same: PasswordServiceImpl, UserServiceImpl<br>Test: 3 tests | `PasswordHasher` | `com.aptisprep.mvc.auth.service.impl` |
| `PublishedExamCatalogService` | **3** (HELPER) | Test: 1 tests | `PublishedExamCatalogService` | `com.aptisprep.mvc.catalog.service.impl` |
| `QuestionDefinitionValidator` | **3** (HELPER) | Same: CatalogServiceImpl | `QuestionDefinitionValidator` | `com.aptisprep.mvc.admin.service.impl` |
| `QuestionMediaWriter` | **3** (HELPER) | Same: CatalogServiceImpl<br>Test: 1 tests | `QuestionMediaWriter` | `com.aptisprep.mvc.admin.service.impl` |
| `ScoreBandCoverage` | **3** (HELPER) | Same: CertificateSchemeService, ScoringAdminServiceImpl, ScoringPolicyServiceImpl | `ScoreBandCoverage` | `com.aptisprep.mvc.scoring.service.impl` |
| `ScoringPolicyService` | **3** (HELPER) | Same: ScoringPolicyServiceImpl<br>Test: 1 tests | `ScoringPolicyService` | `com.aptisprep.mvc.scoring.service.impl` |
| `ScoringSchemeUsageService` | **3** (HELPER) | Chưa có caller trực tiếp | `ScoringSchemeUsageService` | `com.aptisprep.mvc.catalog.service.impl` |
| `SpeakingGradingService` | **3** (HELPER) | Same: AiAssessmentService, GeminiGradingService<br>Test: 1 tests | `SpeakingGradingService` | `com.aptisprep.mvc.grading.service.impl` |
| `WritingGradingService` | **3** (HELPER) | Same: AiAssessmentService, GeminiGradingService<br>Test: 1 tests | `WritingGradingService` | `com.aptisprep.mvc.grading.service.impl` |
| `GradingResult` | **4** (MODEL / VALUE (DTO)) | Same: AiAssessmentService, GeminiGradingService, SpeakingGradingService, WritingGradingService<br>Test: 3 tests | `GradingResultDTO` | `com.aptisprep.mvc.grading.dto` |
| `GradingTextLimits` | **4** (MODEL / VALUE (DTO)) | Other: service.exam.ExamAnswerWriter, service.exam.ExamGradingPayloadFactory, service.grading.AiAssessmentService, service.grading.GeminiGradingService<br>Test: 1 tests | `GradingTextLimitsDTO` | `com.aptisprep.mvc.common.dto` |
| `IntegrationModels` | **4** (MODEL / VALUE (DTO)) | Ctrl: AdminResultController, StudentAccountController, StudentDashboardController, StudentExamCatalogController<br>Other: dto.AdminDashboardView, event.AttemptSubmittedEvent, service.account.AccountAccessServiceImpl, service.admin.AdminDashboardServiceImpl, service.admin.StudentManagementServiceImpl, service.catalog.PublishedExamCatalogService, service.exam.AttemptGradingOrchestrationService, service.exam.AttemptResultReader, service.exam.ExamAttemptService, service.exam.ExamGradingPayloadFactory, service.grading.AttemptFinalizationService, service.grading.ObjectiveGradingService, service.learning.CheckpointStatusReader, service.learning.LearningPathService, service.learning.LessonRecommendationService, service.learning.LessonRecommendationServiceImpl, service.placement.PlacementService, service.placement.PlacementServiceImpl, service.scoring.ScoringGuideService, service.scoring.ScoringGuideServiceImpl<br>Same: AccountAccessContract, ExamAttemptContract, ExamCatalogContract, GradingContextFormatter, LearningPathContract<br>Test: 9 tests | `IntegrationModelsDTO` | `com.aptisprep.mvc.common.dto` |
| `LessonAuthorProfile` | **4** (MODEL / VALUE (DTO)) | Ctrl: AdminLessonController<br>Test: 1 tests | `LessonAuthorProfileDTO` | `com.aptisprep.mvc.learning.dto` |
| `ScoringViews` | **4** (MODEL / VALUE (DTO)) | Ctrl: AdminScoringController<br>Same: ScoringAdminService, ScoringAdminServiceImpl | `ScoringViewsDTO` | `com.aptisprep.mvc.scoring.dto` |
| `ResultLabels` | **5** (VIEW HELPER) | Chưa có caller trực tiếp | `ResultLabels` | `com.aptisprep.mvc.common.util` |
| `AudioDurationService` | **6** (HẠ TẦNG / INTEGRATION) | Other: service.learning.LessonListeningServiceImpl<br>Test: 4 tests | `AudioDurationHelper` | `com.aptisprep.mvc.storage.service.impl` |
| `GeminiGradingService` | **6** (HẠ TẦNG / INTEGRATION) | Test: 5 tests | `GeminiGradingService` | `com.aptisprep.mvc.grading.service.impl` |
| `AttemptResultReader` | **BORDERLINE** (CẦN PHÊ DUYỆT) | Same: ExamAttemptService | `AttemptResultReader` | `com.aptisprep.mvc.exam.service.impl` |
| `CheckpointStatusReader` | **BORDERLINE** (CẦN PHÊ DUYỆT) | Same: LearningPathService | `CheckpointStatusReader` | `com.aptisprep.mvc.learning.service.impl` |
| `LessonCheckService` | **BORDERLINE** (CẦN PHÊ DUYỆT) | Same: LessonTeachingService<br>Test: 1 tests | `LessonCheckService` | `com.aptisprep.mvc.learning.service.impl` |
| `ObjectiveGradingService` | **BORDERLINE** (CẦN PHÊ DUYỆT) | Other: service.exam.ExamAttemptService | `ObjectiveGradingService` | `com.aptisprep.mvc.grading.service.impl` |
| `PlacementExamCatalogService` | **BORDERLINE** (CẦN PHÊ DUYỆT) | Same: PlacementService, PlacementServiceImpl<br>Test: 4 tests | `PlacementExamCatalogService` | `com.aptisprep.mvc.placement.service.impl` |

### Phân tích 5 class không chắc chắn & đề xuất nhóm

Dưới đây là bằng chứng tra cứu caller thực tế của 5 class borderline và đề xuất xếp nhóm:

#### 1. `ObjectiveGradingService` (`service/grading/ObjectiveGradingService.java`)
*   **Caller thực tế**:
    *   Controller: Không có.
    *   Feature khác: Có 1 feature gọi: `service.exam.ExamAttemptService` (Feature `exam` gọi để chấm bài trắc nghiệm tự động khi nộp bài).
    *   Nội bộ feature `grading`: Không có class nào khác gọi.
    *   Test: `AiAssessmentServiceTests`, `AttemptFinalizationServiceTests`.
*   **Bằng chứng**: Trong `ExamAttemptService.java`:
    ```java
    private final ObjectiveGradingService objectiveGrading;
    // Gọi: objectiveGrading.grade(attempt);
    ```
*   **Đề xuất**: **NHÓM 1 (SERVICE THẬT)**. Vì được một feature khác (`exam`) gọi trực tiếp cho use-case chấm điểm khách quan. Cần chuẩn hóa thành:
    *   Interface: `com.aptisprep.mvc.grading.service.ObjectiveGradingService`
    *   Impl: `com.aptisprep.mvc.grading.service.impl.ObjectiveGradingServiceImpl` (chứa `@Service`).
    *(Nếu muốn đưa về Nhóm 3 - Helper, phải chuyển lời gọi này thông qua `AttemptFinalizationService` hoặc qua `ApplicationEvent`).*

#### 2. `LessonCheckService` (`service/learning/LessonCheckService.java`)
*   **Caller thực tế**:
    *   Controller: Không có.
    *   Feature khác: Không có.
    *   Nội bộ feature `learning`: Duy nhất `LessonTeachingService` gọi (dùng để lưu/chấm các câu hỏi kiểm tra nhanh trong bài học).
    *   Test: `V2SqlServerPreservationTests`.
*   **Bằng chứng**: Chỉ được inject bên trong `LessonTeachingService.java`.
*   **Đề xuất**: **NHÓM 3 (HELPER)**. Đổi tên thành `LessonCheckHelper` (hoặc giữ tên `LessonCheckService`), phạm vi package-private, bỏ `@Service`, gắn `@Component` nếu cần inject, đặt trong `com.aptisprep.mvc.learning.service.impl`.

#### 3. `PlacementExamCatalogService` (`service/placement/PlacementExamCatalogService.java`)
*   **Caller thực tế**:
    *   Controller: Không có.
    *   Feature khác: Không có.
    *   Nội bộ feature `placement`: Duy nhất `PlacementServiceImpl` gọi để tìm danh sách đề Placement có sẵn và hợp lệ cho học viên.
    *   Test: `PlacementExamCatalogServiceTests`, `PlacementServiceTests`, `TramBusinessCompletionTests`, `V2BusinessRegressionTests`.
*   **Bằng chứng**: Chỉ được gọi từ `PlacementServiceImpl.java`:
    ```java
    private final PlacementExamCatalogService catalog;
    ```
*   **Đề xuất**: **NHÓM 3 (HELPER)**. Đổi tên thành `PlacementExamCatalogHelper`, phạm vi package-private, bỏ `@Service`, đặt trong `com.aptisprep.mvc.placement.service.impl`.

#### 4. `CheckpointStatusReader` (`service/learning/CheckpointStatusReader.java`)
*   **Caller thực tế**:
    *   Controller: Không có.
    *   Feature khác: Không có.
    *   Nội bộ feature `learning`: Duy nhất `LearningPathService` gọi để tra cứu trạng thái hoàn thành bài học và trạng thái checkpoint.
    *   Test: Không có test trực tiếp.
*   **Bằng chứng**: Hiện tại class đã là package-private (`final class CheckpointStatusReader`) và có `@Component`.
*   **Đề xuất**: **NHÓM 3 (HELPER)**. Giữ nguyên là package-private `@Component` đặt trong `com.aptisprep.mvc.learning.service.impl`.

#### 5. `AttemptResultReader` (`service/exam/AttemptResultReader.java`)
*   **Caller thực tế**:
    *   Controller: Không có.
    *   Feature khác: Không có.
    *   Nội bộ feature `exam`: Duy nhất `ExamAttemptService` gọi để đọc kết quả bài thi, snapshot và format kết quả.
    *   Test: Không có test trực tiếp.
*   **Bằng chứng**: Hiện tại class có `@Service` nhưng chỉ phục vụ nội bộ cho `ExamAttemptService`.
*   **Đề xuất**: **NHÓM 3 (HELPER)**. Bỏ `@Service`, đổi phạm vi thành package-private `@Component`, đặt trong `com.aptisprep.mvc.exam.service.impl`.


---

## 2. BẢNG ÁNH XẠ TOÀN DIỆN 249 FILE JAVA HIỆN TẠI -> PACKAGE ĐÍCH

| STT | File hiện tại | Tên class mới | Package đích | Ghi chú / Trách nhiệm |
|:---:|---|---|---|---|
| 1 | `com/aptisprep/AptisPrepApplication.java` | `AptisPrepApplication` | `com.aptisprep.mvc` | Main Application Class |
| 2 | `com/aptisprep/config/AiClientConfig.java` | `AiClientConfig` | `com.aptisprep.mvc.common.config` | Cấu hình Spring Boot dùng chung |
| 3 | `com/aptisprep/config/AiProperties.java` | `AiProperties` | `com.aptisprep.mvc.common.config` | Cấu hình Spring Boot dùng chung |
| 4 | `com/aptisprep/config/StorageProperties.java` | `StorageProperties` | `com.aptisprep.mvc.common.config` | Cấu hình Spring Boot dùng chung |
| 5 | `com/aptisprep/config/WebMvcConfig.java` | `WebMvcConfig` | `com.aptisprep.mvc.common.config` | Cấu hình Spring Boot dùng chung |
| 6 | `com/aptisprep/controller/AiAssessmentController.java` | `AiAssessmentController` | `com.aptisprep.mvc.grading.controller` | Controller tác vụ chấm AI |
| 7 | `com/aptisprep/controller/HomeController.java` | `HomeController` | `com.aptisprep.mvc.common.controller` | Controller trang chủ / điều hướng |
| 8 | `com/aptisprep/controller/LessonAudioController.java` | `LessonAudioController` | `com.aptisprep.mvc.learning.controller` | Controller bài học & lộ trình |
| 9 | `com/aptisprep/controller/ListPagination.java` | `ListPagination` | `com.aptisprep.mvc.common.util` | Helper giao diện dùng chung |
| 10 | `com/aptisprep/dto/CertificateEntityDto.java` | `CertificateEntityDTO` | `com.aptisprep.mvc.scoring.dto` | DTO chứng chỉ & thang điểm |
| 11 | `com/aptisprep/dto/FlashcardEntityDto.java` | `FlashcardEntityDTO` | `com.aptisprep.mvc.flashcard.dto` | DTO flashcard |
| 12 | `com/aptisprep/dto/FlashcardSetEntityDto.java` | `FlashcardSetEntityDTO` | `com.aptisprep.mvc.flashcard.dto` | DTO flashcard |
| 13 | `com/aptisprep/dto/LessonEntityDto.java` | `LessonEntityDTO` | `com.aptisprep.mvc.learning.dto` | DTO bài học |
| 14 | `com/aptisprep/dto/MockExamEntityDto.java` | `MockExamEntityDTO` | `com.aptisprep.mvc.catalog.dto` | DTO ngân hàng đề & câu hỏi |
| 15 | `com/aptisprep/dto/QuestionEntityDto.java` | `QuestionEntityDTO` | `com.aptisprep.mvc.catalog.dto` | DTO ngân hàng đề & câu hỏi |
| 16 | `com/aptisprep/dto/ScoreBandEntityDto.java` | `ScoreBandEntityDTO` | `com.aptisprep.mvc.scoring.dto` | DTO chứng chỉ & thang điểm |
| 17 | `com/aptisprep/dto/ScoringScaleRuleEntityDto.java` | `ScoringScaleRuleEntityDTO` | `com.aptisprep.mvc.scoring.dto` | DTO chứng chỉ & thang điểm |
| 18 | `com/aptisprep/dto/ScoringSchemeEntityDto.java` | `ScoringSchemeEntityDTO` | `com.aptisprep.mvc.scoring.dto` | DTO chứng chỉ & thang điểm |
| 19 | `com/aptisprep/dto/UserEntityDto.java` | `UserEntityDTO` | `com.aptisprep.mvc.auth.dto` | DTO xác thực & tài khoản |
| 20 | `com/aptisprep/dto/UserGoalEntityDto.java` | `UserGoalEntityDTO` | `com.aptisprep.mvc.auth.dto` | DTO xác thực & tài khoản |
| 21 | `com/aptisprep/entity/AiAssessment.java` | `AiAssessment` | `com.aptisprep.mvc.grading.entity` | Entity kết quả kỹ năng & chấm AI |
| 22 | `com/aptisprep/entity/AiAssessmentJob.java` | `AiAssessmentJob` | `com.aptisprep.mvc.grading.entity` | Entity kết quả kỹ năng & chấm AI |
| 23 | `com/aptisprep/entity/AttemptOutboxEntity.java` | `AttemptOutboxEntity` | `com.aptisprep.mvc.exam.entity` | Entity lượt thi & snapshot đề thi |
| 24 | `com/aptisprep/entity/Certificate.java` | `Certificate` | `com.aptisprep.mvc.scoring.entity` | Entity chứng chỉ & thang điểm |
| 25 | `com/aptisprep/entity/ExamAttemptEntity.java` | `ExamAttemptEntity` | `com.aptisprep.mvc.exam.entity` | Entity lượt thi & snapshot đề thi |
| 26 | `com/aptisprep/entity/ExamAttemptSnapshotEntity.java` | `ExamAttemptSnapshotEntity` | `com.aptisprep.mvc.exam.entity` | Entity lượt thi & snapshot đề thi |
| 27 | `com/aptisprep/entity/ExamItemSnapshotEntity.java` | `ExamItemSnapshotEntity` | `com.aptisprep.mvc.exam.entity` | Entity lượt thi & snapshot đề thi |
| 28 | `com/aptisprep/entity/ExamMediaSnapshotEntity.java` | `ExamMediaSnapshotEntity` | `com.aptisprep.mvc.exam.entity` | Entity lượt thi & snapshot đề thi |
| 29 | `com/aptisprep/entity/ExamOptionSnapshotEntity.java` | `ExamOptionSnapshotEntity` | `com.aptisprep.mvc.exam.entity` | Entity lượt thi & snapshot đề thi |
| 30 | `com/aptisprep/entity/ExamQuestionSnapshotEntity.java` | `ExamQuestionSnapshotEntity` | `com.aptisprep.mvc.exam.entity` | Entity lượt thi & snapshot đề thi |
| 31 | `com/aptisprep/entity/ExamSkillResultEntity.java` | `ExamSkillResultEntity` | `com.aptisprep.mvc.grading.entity` | Entity kết quả kỹ năng & chấm AI |
| 32 | `com/aptisprep/entity/Flashcard.java` | `Flashcard` | `com.aptisprep.mvc.flashcard.entity` | Entity thẻ từ vựng flashcard |
| 33 | `com/aptisprep/entity/FlashcardReview.java` | `FlashcardReview` | `com.aptisprep.mvc.flashcard.entity` | Entity thẻ từ vựng flashcard |
| 34 | `com/aptisprep/entity/FlashcardSet.java` | `FlashcardSet` | `com.aptisprep.mvc.flashcard.entity` | Entity thẻ từ vựng flashcard |
| 35 | `com/aptisprep/entity/LearningPath.java` | `LearningPath` | `com.aptisprep.mvc.learning.entity` | Entity bài học & lộ trình học |
| 36 | `com/aptisprep/entity/LearningPathItem.java` | `LearningPathItem` | `com.aptisprep.mvc.learning.entity` | Entity bài học & lộ trình học |
| 37 | `com/aptisprep/entity/Lesson.java` | `Lesson` | `com.aptisprep.mvc.learning.entity` | Entity bài học & lộ trình học |
| 38 | `com/aptisprep/entity/LessonCheck.java` | `LessonCheck` | `com.aptisprep.mvc.learning.entity` | Entity bài học & lộ trình học |
| 39 | `com/aptisprep/entity/LessonCheckAnswerEntity.java` | `LessonCheckAnswerEntity` | `com.aptisprep.mvc.learning.entity` | Entity bài học & lộ trình học |
| 40 | `com/aptisprep/entity/LessonCheckAttempt.java` | `LessonCheckAttempt` | `com.aptisprep.mvc.learning.entity` | Entity bài học & lộ trình học |
| 41 | `com/aptisprep/entity/LessonCheckFeedbackEntity.java` | `LessonCheckFeedbackEntity` | `com.aptisprep.mvc.learning.entity` | Entity bài học & lộ trình học |
| 42 | `com/aptisprep/entity/LessonContextSnapshot.java` | `LessonContextSnapshot` | `com.aptisprep.mvc.learning.entity` | Entity bài học & lộ trình học |
| 43 | `com/aptisprep/entity/LessonContextSnapshotEntity.java` | `LessonContextSnapshotEntity` | `com.aptisprep.mvc.learning.entity` | Entity bài học & lộ trình học |
| 44 | `com/aptisprep/entity/LessonEngagement.java` | `LessonEngagement` | `com.aptisprep.mvc.learning.entity` | Entity bài học & lộ trình học |
| 45 | `com/aptisprep/entity/LessonMaterial.java` | `LessonMaterial` | `com.aptisprep.mvc.learning.entity` | Entity bài học & lộ trình học |
| 46 | `com/aptisprep/entity/LessonPlaybackRangeEntity.java` | `LessonPlaybackRangeEntity` | `com.aptisprep.mvc.learning.entity` | Entity bài học & lộ trình học |
| 47 | `com/aptisprep/entity/LessonProgress.java` | `LessonProgress` | `com.aptisprep.mvc.learning.entity` | Entity bài học & lộ trình học |
| 48 | `com/aptisprep/entity/LessonResponse.java` | `LessonResponse` | `com.aptisprep.mvc.learning.entity` | Entity bài học & lộ trình học |
| 49 | `com/aptisprep/entity/LessonWorkSubmission.java` | `LessonWorkSubmission` | `com.aptisprep.mvc.learning.entity` | Entity bài học & lộ trình học |
| 50 | `com/aptisprep/entity/MockExamEntity.java` | `MockExamEntity` | `com.aptisprep.mvc.catalog.entity` | Entity ngân hàng câu hỏi & đề thi |
| 51 | `com/aptisprep/entity/MockExamQuestionEntity.java` | `MockExamQuestionEntity` | `com.aptisprep.mvc.catalog.entity` | Entity ngân hàng câu hỏi & đề thi |
| 52 | `com/aptisprep/entity/MockExamSectionEntity.java` | `MockExamSectionEntity` | `com.aptisprep.mvc.catalog.entity` | Entity ngân hàng câu hỏi & đề thi |
| 53 | `com/aptisprep/entity/ProfileApplication.java` | `ProfileApplication` | `com.aptisprep.mvc.account.entity` | Entity hồ sơ & mục tiêu học viên |
| 54 | `com/aptisprep/entity/QuestionEntity.java` | `QuestionEntity` | `com.aptisprep.mvc.catalog.entity` | Entity ngân hàng câu hỏi & đề thi |
| 55 | `com/aptisprep/entity/QuestionItemEntity.java` | `QuestionItemEntity` | `com.aptisprep.mvc.catalog.entity` | Entity ngân hàng câu hỏi & đề thi |
| 56 | `com/aptisprep/entity/QuestionMediaEntity.java` | `QuestionMediaEntity` | `com.aptisprep.mvc.catalog.entity` | Entity ngân hàng câu hỏi & đề thi |
| 57 | `com/aptisprep/entity/QuestionOptionEntity.java` | `QuestionOptionEntity` | `com.aptisprep.mvc.catalog.entity` | Entity ngân hàng câu hỏi & đề thi |
| 58 | `com/aptisprep/entity/ScoreBand.java` | `ScoreBand` | `com.aptisprep.mvc.scoring.entity` | Entity chứng chỉ & thang điểm |
| 59 | `com/aptisprep/entity/ScoringScaleRule.java` | `ScoringScaleRule` | `com.aptisprep.mvc.scoring.entity` | Entity chứng chỉ & thang điểm |
| 60 | `com/aptisprep/entity/ScoringScheme.java` | `ScoringScheme` | `com.aptisprep.mvc.scoring.entity` | Entity chứng chỉ & thang điểm |
| 61 | `com/aptisprep/entity/User.java` | `User` | `com.aptisprep.mvc.auth.entity` | Entity người dùng / xác thực |
| 62 | `com/aptisprep/entity/UserAnswerDetailEntity.java` | `UserAnswerDetailEntity` | `com.aptisprep.mvc.exam.entity` | Entity lượt thi & snapshot đề thi |
| 63 | `com/aptisprep/entity/UserAnswerEntity.java` | `UserAnswerEntity` | `com.aptisprep.mvc.exam.entity` | Entity lượt thi & snapshot đề thi |
| 64 | `com/aptisprep/entity/UserFlashcardProgress.java` | `UserFlashcardProgress` | `com.aptisprep.mvc.flashcard.entity` | Entity thẻ từ vựng flashcard |
| 65 | `com/aptisprep/entity/UserGoal.java` | `UserGoal` | `com.aptisprep.mvc.account.entity` | Entity hồ sơ & mục tiêu học viên |
| 66 | `com/aptisprep/entity/UserGoalSkill.java` | `UserGoalSkill` | `com.aptisprep.mvc.account.entity` | Entity hồ sơ & mục tiêu học viên |
| 67 | `com/aptisprep/entity/UserMediaActivityEntity.java` | `UserMediaActivityEntity` | `com.aptisprep.mvc.exam.entity` | Entity lượt thi & snapshot đề thi |
| 68 | `com/aptisprep/entity/UserSkillProfile.java` | `UserSkillProfile` | `com.aptisprep.mvc.account.entity` | Entity hồ sơ & mục tiêu học viên |
| 69 | `com/aptisprep/enums/AiAssessmentState.java` | `AiAssessmentState` | `com.aptisprep.mvc.common.enums` | Enum dùng chung toàn hệ thống |
| 70 | `com/aptisprep/enums/AiJobState.java` | `AiJobState` | `com.aptisprep.mvc.common.enums` | Enum dùng chung toàn hệ thống |
| 71 | `com/aptisprep/enums/CefrLevel.java` | `CefrLevel` | `com.aptisprep.mvc.common.enums` | Enum dùng chung toàn hệ thống |
| 72 | `com/aptisprep/enums/GoalStatus.java` | `GoalStatus` | `com.aptisprep.mvc.common.enums` | Enum dùng chung toàn hệ thống |
| 73 | `com/aptisprep/enums/QuestionType.java` | `QuestionType` | `com.aptisprep.mvc.common.enums` | Enum dùng chung toàn hệ thống |
| 74 | `com/aptisprep/enums/RecordStatus.java` | `RecordStatus` | `com.aptisprep.mvc.common.enums` | Enum dùng chung toàn hệ thống |
| 75 | `com/aptisprep/enums/Role.java` | `Role` | `com.aptisprep.mvc.common.enums` | Enum dùng chung toàn hệ thống |
| 76 | `com/aptisprep/enums/Skill.java` | `Skill` | `com.aptisprep.mvc.common.enums` | Enum dùng chung toàn hệ thống |
| 77 | `com/aptisprep/enums/UserStatus.java` | `UserStatus` | `com.aptisprep.mvc.common.enums` | Enum dùng chung toàn hệ thống |
| 78 | `com/aptisprep/event/AiAssessmentCompletedEvent.java` | `AiAssessmentCompletedEvent` | `com.aptisprep.mvc.common.event` | Domain / Application Event dùng chung |
| 79 | `com/aptisprep/event/AttemptCompletedEvent.java` | `AttemptCompletedEvent` | `com.aptisprep.mvc.common.event` | Domain / Application Event dùng chung |
| 80 | `com/aptisprep/event/AttemptSubmittedEvent.java` | `AttemptSubmittedEvent` | `com.aptisprep.mvc.common.event` | Domain / Application Event dùng chung |
| 81 | `com/aptisprep/event/GoalChangedEvent.java` | `GoalChangedEvent` | `com.aptisprep.mvc.common.event` | Domain / Application Event dùng chung |
| 82 | `com/aptisprep/exception/AccessDeniedException.java` | `AccessDeniedException` | `com.aptisprep.mvc.common.exception` | Exception dùng chung |
| 83 | `com/aptisprep/exception/AiGradingException.java` | `AiGradingException` | `com.aptisprep.mvc.common.exception` | Exception dùng chung |
| 84 | `com/aptisprep/exception/BusinessRuleException.java` | `BusinessRuleException` | `com.aptisprep.mvc.common.exception` | Exception dùng chung |
| 85 | `com/aptisprep/exception/DuplicateEmailException.java` | `DuplicateEmailException` | `com.aptisprep.mvc.common.exception` | Exception dùng chung |
| 86 | `com/aptisprep/exception/GlobalExceptionHandler.java` | `GlobalExceptionHandler` | `com.aptisprep.mvc.common.exception` | Exception dùng chung |
| 87 | `com/aptisprep/exception/ResourceNotFoundException.java` | `ResourceNotFoundException` | `com.aptisprep.mvc.common.exception` | Exception dùng chung |
| 88 | `com/aptisprep/repository/AiAssessmentJobRepository.java` | `AiAssessmentJobRepository` | `com.aptisprep.mvc.grading.repository` | Repository kết quả & AI assessment |
| 89 | `com/aptisprep/repository/AiAssessmentRepository.java` | `AiAssessmentRepository` | `com.aptisprep.mvc.grading.repository` | Repository kết quả & AI assessment |
| 90 | `com/aptisprep/repository/AttemptOutboxRepository.java` | `AttemptOutboxRepository` | `com.aptisprep.mvc.exam.repository` | Repository lượt thi & snapshot |
| 91 | `com/aptisprep/repository/CertificateRepository.java` | `CertificateRepository` | `com.aptisprep.mvc.scoring.repository` | Repository chứng chỉ & thang điểm |
| 92 | `com/aptisprep/repository/ExamAttemptRepository.java` | `ExamAttemptRepository` | `com.aptisprep.mvc.exam.repository` | Repository lượt thi & snapshot |
| 93 | `com/aptisprep/repository/ExamAttemptSnapshotRepository.java` | `ExamAttemptSnapshotRepository` | `com.aptisprep.mvc.exam.repository` | Repository lượt thi & snapshot |
| 94 | `com/aptisprep/repository/ExamSkillResultRepository.java` | `ExamSkillResultRepository` | `com.aptisprep.mvc.grading.repository` | Repository kết quả & AI assessment |
| 95 | `com/aptisprep/repository/FlashcardRepository.java` | `FlashcardRepository` | `com.aptisprep.mvc.flashcard.repository` | Repository thẻ từ vựng |
| 96 | `com/aptisprep/repository/FlashcardSetRepository.java` | `FlashcardSetRepository` | `com.aptisprep.mvc.flashcard.repository` | Repository thẻ từ vựng |
| 97 | `com/aptisprep/repository/LearningPathItemRepository.java` | `LearningPathItemRepository` | `com.aptisprep.mvc.learning.repository` | Repository bài học & lộ trình |
| 98 | `com/aptisprep/repository/LearningPathRepository.java` | `LearningPathRepository` | `com.aptisprep.mvc.learning.repository` | Repository bài học & lộ trình |
| 99 | `com/aptisprep/repository/LessonCheckAttemptRepository.java` | `LessonCheckAttemptRepository` | `com.aptisprep.mvc.learning.repository` | Repository bài học & lộ trình |
| 100 | `com/aptisprep/repository/LessonCheckRepository.java` | `LessonCheckRepository` | `com.aptisprep.mvc.learning.repository` | Repository bài học & lộ trình |
| 101 | `com/aptisprep/repository/LessonEngagementRepository.java` | `LessonEngagementRepository` | `com.aptisprep.mvc.learning.repository` | Repository bài học & lộ trình |
| 102 | `com/aptisprep/repository/LessonProgressRepository.java` | `LessonProgressRepository` | `com.aptisprep.mvc.learning.repository` | Repository bài học & lộ trình |
| 103 | `com/aptisprep/repository/LessonRepository.java` | `LessonRepository` | `com.aptisprep.mvc.learning.repository` | Repository bài học & lộ trình |
| 104 | `com/aptisprep/repository/LessonResponseRepository.java` | `LessonResponseRepository` | `com.aptisprep.mvc.learning.repository` | Repository bài học & lộ trình |
| 105 | `com/aptisprep/repository/MockExamQuestionRepository.java` | `MockExamQuestionRepository` | `com.aptisprep.mvc.catalog.repository` | Repository ngân hàng câu hỏi & đề thi |
| 106 | `com/aptisprep/repository/MockExamRepository.java` | `MockExamRepository` | `com.aptisprep.mvc.catalog.repository` | Repository ngân hàng câu hỏi & đề thi |
| 107 | `com/aptisprep/repository/MockExamSectionRepository.java` | `MockExamSectionRepository` | `com.aptisprep.mvc.catalog.repository` | Repository ngân hàng câu hỏi & đề thi |
| 108 | `com/aptisprep/repository/ProfileApplicationRepository.java` | `ProfileApplicationRepository` | `com.aptisprep.mvc.account.repository` | Repository hồ sơ & mục tiêu |
| 109 | `com/aptisprep/repository/QuestionItemRepository.java` | `QuestionItemRepository` | `com.aptisprep.mvc.catalog.repository` | Repository ngân hàng câu hỏi & đề thi |
| 110 | `com/aptisprep/repository/QuestionMediaRepository.java` | `QuestionMediaRepository` | `com.aptisprep.mvc.catalog.repository` | Repository ngân hàng câu hỏi & đề thi |
| 111 | `com/aptisprep/repository/QuestionOptionRepository.java` | `QuestionOptionRepository` | `com.aptisprep.mvc.catalog.repository` | Repository ngân hàng câu hỏi & đề thi |
| 112 | `com/aptisprep/repository/QuestionRepository.java` | `QuestionRepository` | `com.aptisprep.mvc.catalog.repository` | Repository ngân hàng câu hỏi & đề thi |
| 113 | `com/aptisprep/repository/ScoreBandRepository.java` | `ScoreBandRepository` | `com.aptisprep.mvc.scoring.repository` | Repository chứng chỉ & thang điểm |
| 114 | `com/aptisprep/repository/ScoringScaleRuleRepository.java` | `ScoringScaleRuleRepository` | `com.aptisprep.mvc.scoring.repository` | Repository chứng chỉ & thang điểm |
| 115 | `com/aptisprep/repository/ScoringSchemeRepository.java` | `ScoringSchemeRepository` | `com.aptisprep.mvc.scoring.repository` | Repository chứng chỉ & thang điểm |
| 116 | `com/aptisprep/repository/UserAnswerDetailRepository.java` | `UserAnswerDetailRepository` | `com.aptisprep.mvc.exam.repository` | Repository lượt thi & snapshot |
| 117 | `com/aptisprep/repository/UserAnswerRepository.java` | `UserAnswerRepository` | `com.aptisprep.mvc.exam.repository` | Repository lượt thi & snapshot |
| 118 | `com/aptisprep/repository/UserFlashcardProgressRepository.java` | `UserFlashcardProgressRepository` | `com.aptisprep.mvc.flashcard.repository` | Repository thẻ từ vựng |
| 119 | `com/aptisprep/repository/UserGoalRepository.java` | `UserGoalRepository` | `com.aptisprep.mvc.account.repository` | Repository hồ sơ & mục tiêu |
| 120 | `com/aptisprep/repository/UserGoalSkillRepository.java` | `UserGoalSkillRepository` | `com.aptisprep.mvc.account.repository` | Repository hồ sơ & mục tiêu |
| 121 | `com/aptisprep/repository/UserMediaActivityRepository.java` | `UserMediaActivityRepository` | `com.aptisprep.mvc.exam.repository` | Repository lượt thi & snapshot |
| 122 | `com/aptisprep/repository/UserRepository.java` | `UserRepository` | `com.aptisprep.mvc.auth.repository` | Repository người dùng |
| 123 | `com/aptisprep/repository/UserSkillProfileRepository.java` | `UserSkillProfileRepository` | `com.aptisprep.mvc.account.repository` | Repository hồ sơ & mục tiêu |
| 124 | `com/aptisprep/security/CsrfProtection.java` | `CsrfProtection` | `com.aptisprep.mvc.common.security` | Cấu hình bảo mật phiên / CSRF |
| 125 | `com/aptisprep/security/LoginSession.java` | `LoginSession` | `com.aptisprep.mvc.common.security` | Cấu hình bảo mật phiên / CSRF |
| 126 | `com/aptisprep/security/SessionAuthenticationFilter.java` | `SessionAuthenticationFilter` | `com.aptisprep.mvc.common.security` | Cấu hình bảo mật phiên / CSRF |
| 127 | `com/aptisprep/service/account/AccountAccessService.java` | `AccountAccessService` | `com.aptisprep.mvc.account.service` | Nhóm 1: SERVICE THẬT |
| 128 | `com/aptisprep/service/account/AccountProfileService.java` | `AccountProfileService` | `com.aptisprep.mvc.account.service` | Nhóm 1: SERVICE THẬT |
| 129 | `com/aptisprep/service/account/GoalPathListener.java` | `GoalPathListener` | `com.aptisprep.mvc.account.service.impl` | Nhóm 2: LISTENER |
| 130 | `com/aptisprep/service/account/LearningActivityService.java` | `LearningActivityService` | `com.aptisprep.mvc.account.service` | Nhóm 1: SERVICE THẬT |
| 131 | `com/aptisprep/service/admin/AdminDashboardService.java` | `AdminDashboardService` | `com.aptisprep.mvc.admin.service` | Nhóm 1: SERVICE THẬT |
| 132 | `com/aptisprep/service/admin/AdminExamService.java` | `AdminExamService` | `com.aptisprep.mvc.admin.service.impl` | Nhóm 3: HELPER |
| 133 | `com/aptisprep/service/admin/CatalogService.java` | `CatalogService` | `com.aptisprep.mvc.admin.service` | Nhóm 1: SERVICE THẬT |
| 134 | `com/aptisprep/service/admin/ExamDefinitionValidator.java` | `ExamDefinitionValidator` | `com.aptisprep.mvc.admin.service.impl` | Nhóm 3: HELPER |
| 135 | `com/aptisprep/service/admin/QuestionDefinitionValidator.java` | `QuestionDefinitionValidator` | `com.aptisprep.mvc.admin.service.impl` | Nhóm 3: HELPER |
| 136 | `com/aptisprep/service/admin/QuestionMediaWriter.java` | `QuestionMediaWriter` | `com.aptisprep.mvc.admin.service.impl` | Nhóm 3: HELPER |
| 137 | `com/aptisprep/service/admin/StudentManagementService.java` | `StudentManagementService` | `com.aptisprep.mvc.admin.service` | Nhóm 1: SERVICE THẬT |
| 138 | `com/aptisprep/service/auth/PasswordHasher.java` | `PasswordHasher` | `com.aptisprep.mvc.auth.service.impl` | Nhóm 3: HELPER |
| 139 | `com/aptisprep/service/auth/PasswordService.java` | `PasswordService` | `com.aptisprep.mvc.auth.service` | Nhóm 1: SERVICE THẬT |
| 140 | `com/aptisprep/service/auth/UserService.java` | `UserService` | `com.aptisprep.mvc.auth.service` | Nhóm 1: SERVICE THẬT |
| 141 | `com/aptisprep/service/catalog/PublishedExamCatalogService.java` | `PublishedExamCatalogService` | `com.aptisprep.mvc.catalog.service.impl` | Nhóm 3: HELPER |
| 142 | `com/aptisprep/service/catalog/ScoringSchemeUsageService.java` | `ScoringSchemeUsageService` | `com.aptisprep.mvc.catalog.service.impl` | Nhóm 3: HELPER |
| 143 | `com/aptisprep/service/contract/AccountAccessContract.java` | `AccountAccessService` | `com.aptisprep.mvc.common.service` | Nhóm 1: SERVICE THẬT (Interface) |
| 144 | `com/aptisprep/service/contract/AiAssessmentContract.java` | `AiAssessmentService` | `com.aptisprep.mvc.common.service` | Nhóm 1: SERVICE THẬT (Interface) |
| 145 | `com/aptisprep/service/contract/CertificateSchemeContract.java` | `CertificateSchemeService` | `com.aptisprep.mvc.common.service` | Nhóm 1: SERVICE THẬT (Interface) |
| 146 | `com/aptisprep/service/contract/ExamAttemptContract.java` | `ExamAttemptService` | `com.aptisprep.mvc.common.service` | Nhóm 1: SERVICE THẬT (Interface) |
| 147 | `com/aptisprep/service/contract/ExamCatalogContract.java` | `ExamCatalogService` | `com.aptisprep.mvc.common.service` | Nhóm 1: SERVICE THẬT (Interface) |
| 148 | `com/aptisprep/service/contract/GradingContextFormatter.java` | `GradingContextFormatter` | `com.aptisprep.mvc.contract.service.impl` | Nhóm 3: HELPER |
| 149 | `com/aptisprep/service/contract/GradingTextLimits.java` | `GradingTextLimitsDTO` | `com.aptisprep.mvc.common.dto` | Nhóm 4: MODEL / VALUE (DTO) |
| 150 | `com/aptisprep/service/contract/IntegrationModels.java` | `IntegrationModelsDTO` | `com.aptisprep.mvc.common.dto` | Nhóm 4: MODEL / VALUE (DTO) |
| 151 | `com/aptisprep/service/contract/LearningPathContract.java` | `LearningPathService` | `com.aptisprep.mvc.common.service` | Nhóm 1: SERVICE THẬT (Interface) |
| 152 | `com/aptisprep/service/contract/ScoringPolicyContract.java` | `ScoringPolicyService` | `com.aptisprep.mvc.common.service` | Nhóm 1: SERVICE THẬT (Interface) |
| 153 | `com/aptisprep/service/contract/ScoringSchemeUsageContract.java` | `ScoringSchemeUsageService` | `com.aptisprep.mvc.common.service` | Nhóm 1: SERVICE THẬT (Interface) |
| 154 | `com/aptisprep/service/exam/AttemptGradingOrchestrationService.java` | `AttemptGradingOrchestrationListener` | `com.aptisprep.mvc.exam.service.impl` | Nhóm 2: LISTENER |
| 155 | `com/aptisprep/service/exam/AttemptOutboxService.java` | `AttemptOutboxService` | `com.aptisprep.mvc.exam.service` | Nhóm 1: SERVICE THẬT |
| 156 | `com/aptisprep/service/exam/AttemptResultReader.java` | `AttemptResultReader` | `com.aptisprep.mvc.exam.service.impl` | Nhóm BORDERLINE: CẦN PHÊ DUYỆT |
| 157 | `com/aptisprep/service/exam/CheckpointStartValidator.java` | `CheckpointStartValidator` | `com.aptisprep.mvc.exam.service.impl` | Nhóm 3: HELPER |
| 158 | `com/aptisprep/service/exam/ExamAnswerWriter.java` | `ExamAnswerWriter` | `com.aptisprep.mvc.exam.service.impl` | Nhóm 3: HELPER |
| 159 | `com/aptisprep/service/exam/ExamAttemptService.java` | `ExamAttemptService` | `com.aptisprep.mvc.exam.service` | Nhóm 1: SERVICE THẬT |
| 160 | `com/aptisprep/service/exam/ExamGradingPayloadFactory.java` | `ExamGradingPayloadFactory` | `com.aptisprep.mvc.exam.service.impl` | Nhóm 3: HELPER |
| 161 | `com/aptisprep/service/exam/ExamSnapshotService.java` | `ExamSnapshotFactory` | `com.aptisprep.mvc.exam.service.impl` | Nhóm 3: HELPER |
| 162 | `com/aptisprep/service/exam/ExamStartValidator.java` | `ExamStartValidator` | `com.aptisprep.mvc.exam.service.impl` | Nhóm 3: HELPER |
| 163 | `com/aptisprep/service/grading/AiAssessmentService.java` | `AiAssessmentService` | `com.aptisprep.mvc.grading.service.impl` | Nhóm 3: HELPER |
| 164 | `com/aptisprep/service/grading/AiProcessingPolicy.java` | `AiProcessingPolicy` | `com.aptisprep.mvc.grading.service.impl` | Nhóm 3: HELPER |
| 165 | `com/aptisprep/service/grading/AttemptFinalizationService.java` | `AttemptFinalizationService` | `com.aptisprep.mvc.grading.service` | Nhóm 1: SERVICE THẬT |
| 166 | `com/aptisprep/service/grading/GeminiGradingService.java` | `GeminiGradingService` | `com.aptisprep.mvc.grading.service.impl` | Nhóm 6: HẠ TẦNG / INTEGRATION |
| 167 | `com/aptisprep/service/grading/GradingResult.java` | `GradingResultDTO` | `com.aptisprep.mvc.grading.dto` | Nhóm 4: MODEL / VALUE (DTO) |
| 168 | `com/aptisprep/service/grading/ObjectiveGradingService.java` | `ObjectiveGradingService` | `com.aptisprep.mvc.grading.service.impl` | Nhóm BORDERLINE: CẦN PHÊ DUYỆT |
| 169 | `com/aptisprep/service/grading/SpeakingGradingService.java` | `SpeakingGradingService` | `com.aptisprep.mvc.grading.service.impl` | Nhóm 3: HELPER |
| 170 | `com/aptisprep/service/grading/WritingGradingService.java` | `WritingGradingService` | `com.aptisprep.mvc.grading.service.impl` | Nhóm 3: HELPER |
| 171 | `com/aptisprep/service/learning/CheckpointStatusReader.java` | `CheckpointStatusReader` | `com.aptisprep.mvc.learning.service.impl` | Nhóm BORDERLINE: CẦN PHÊ DUYỆT |
| 172 | `com/aptisprep/service/learning/FlashcardSetService.java` | `FlashcardSetService` | `com.aptisprep.mvc.learning.service` | Nhóm 1: SERVICE THẬT |
| 173 | `com/aptisprep/service/learning/LearningPathService.java` | `LearningPathService` | `com.aptisprep.mvc.learning.service` | Nhóm 1: SERVICE THẬT |
| 174 | `com/aptisprep/service/learning/LearningProgressCalculator.java` | `LearningProgressCalculator` | `com.aptisprep.mvc.learning.service.impl` | Nhóm 3: HELPER |
| 175 | `com/aptisprep/service/learning/LessonAuthorProfile.java` | `LessonAuthorProfileDTO` | `com.aptisprep.mvc.learning.dto` | Nhóm 4: MODEL / VALUE (DTO) |
| 176 | `com/aptisprep/service/learning/LessonCheckService.java` | `LessonCheckService` | `com.aptisprep.mvc.learning.service.impl` | Nhóm BORDERLINE: CẦN PHÊ DUYỆT |
| 177 | `com/aptisprep/service/learning/LessonListeningService.java` | `LessonListeningService` | `com.aptisprep.mvc.learning.service` | Nhóm 1: SERVICE THẬT |
| 178 | `com/aptisprep/service/learning/LessonRecommendationService.java` | `LessonRecommendationService` | `com.aptisprep.mvc.learning.service` | Nhóm 1: SERVICE THẬT |
| 179 | `com/aptisprep/service/learning/LessonService.java` | `LessonService` | `com.aptisprep.mvc.learning.service` | Nhóm 1: SERVICE THẬT |
| 180 | `com/aptisprep/service/learning/LessonTeachingService.java` | `LessonTeachingService` | `com.aptisprep.mvc.learning.service` | Nhóm 1: SERVICE THẬT |
| 181 | `com/aptisprep/service/placement/PlacementExamCatalogService.java` | `PlacementExamCatalogService` | `com.aptisprep.mvc.placement.service.impl` | Nhóm BORDERLINE: CẦN PHÊ DUYỆT |
| 182 | `com/aptisprep/service/placement/PlacementOrchestrationService.java` | `PlacementOrchestrationListener` | `com.aptisprep.mvc.placement.service.impl` | Nhóm 2: LISTENER |
| 183 | `com/aptisprep/service/placement/PlacementService.java` | `PlacementService` | `com.aptisprep.mvc.placement.service` | Nhóm 1: SERVICE THẬT |
| 184 | `com/aptisprep/service/scoring/CertificateSchemeService.java` | `CertificateSchemeService` | `com.aptisprep.mvc.scoring.service.impl` | Nhóm 3: HELPER |
| 185 | `com/aptisprep/service/scoring/CheckpointScorePolicy.java` | `CheckpointScorePolicy` | `com.aptisprep.mvc.scoring.service.impl` | Nhóm 3: HELPER |
| 186 | `com/aptisprep/service/scoring/ScoreBandCoverage.java` | `ScoreBandCoverage` | `com.aptisprep.mvc.scoring.service.impl` | Nhóm 3: HELPER |
| 187 | `com/aptisprep/service/scoring/ScoringAdminService.java` | `ScoringAdminService` | `com.aptisprep.mvc.scoring.service` | Nhóm 1: SERVICE THẬT |
| 188 | `com/aptisprep/service/scoring/ScoringGuideService.java` | `ScoringGuideService` | `com.aptisprep.mvc.scoring.service` | Nhóm 1: SERVICE THẬT |
| 189 | `com/aptisprep/service/scoring/ScoringPolicyService.java` | `ScoringPolicyService` | `com.aptisprep.mvc.scoring.service.impl` | Nhóm 3: HELPER |
| 190 | `com/aptisprep/service/scoring/ScoringViews.java` | `ScoringViewsDTO` | `com.aptisprep.mvc.scoring.dto` | Nhóm 4: MODEL / VALUE (DTO) |
| 191 | `com/aptisprep/service/storage/AudioDurationService.java` | `AudioDurationHelper` | `com.aptisprep.mvc.storage.service.impl` | Nhóm 6: HẠ TẦNG / INTEGRATION |
| 192 | `com/aptisprep/service/storage/AudioUploadService.java` | `AudioUploadService` | `com.aptisprep.mvc.storage.service` | Nhóm 1: SERVICE THẬT |
| 193 | `com/aptisprep/service/storage/LocalStorageService.java` | `LocalStorageService` | `com.aptisprep.mvc.storage.service.impl` | Nhóm 3: HELPER |
| 194 | `com/aptisprep/service/storage/StorageService.java` | `StorageService` | `com.aptisprep.mvc.storage.service` | Nhóm 1: SERVICE THẬT |
| 195 | `com/aptisprep/service/student/ResultLabels.java` | `ResultLabels` | `com.aptisprep.mvc.common.util` | Thymeleaf Bean @resultLabels (View Helper) |
| 196 | `com/aptisprep/service/student/StudentExamCatalogService.java` | `StudentExamCatalogService` | `com.aptisprep.mvc.student.service` | Nhóm 1: SERVICE THẬT |
| 197 | `com/aptisprep/service/student/impl/StudentExamCatalogServiceImpl.java` | `StudentExamCatalogServiceImpl` | `com.aptisprep.mvc.student.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 198 | `com/aptisprep/service/storage/impl/AudioUploadServiceImpl.java` | `AudioUploadServiceImpl` | `com.aptisprep.mvc.storage.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 199 | `com/aptisprep/service/storage/impl/LocalStorageServiceImpl.java` | `LocalStorageServiceImpl` | `com.aptisprep.mvc.storage.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 200 | `com/aptisprep/service/scoring/impl/ScoringAdminServiceImpl.java` | `ScoringAdminServiceImpl` | `com.aptisprep.mvc.scoring.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 201 | `com/aptisprep/service/scoring/impl/ScoringGuideServiceImpl.java` | `ScoringGuideServiceImpl` | `com.aptisprep.mvc.scoring.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 202 | `com/aptisprep/service/scoring/impl/ScoringPolicyServiceImpl.java` | `ScoringPolicyServiceImpl` | `com.aptisprep.mvc.scoring.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 203 | `com/aptisprep/service/placement/impl/PlacementServiceImpl.java` | `PlacementServiceImpl` | `com.aptisprep.mvc.placement.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 204 | `com/aptisprep/service/learning/impl/FlashcardSetServiceImpl.java` | `FlashcardSetServiceImpl` | `com.aptisprep.mvc.learning.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 205 | `com/aptisprep/service/learning/impl/LessonListeningServiceImpl.java` | `LessonListeningServiceImpl` | `com.aptisprep.mvc.learning.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 206 | `com/aptisprep/service/learning/impl/LessonRecommendationServiceImpl.java` | `LessonRecommendationServiceImpl` | `com.aptisprep.mvc.learning.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 207 | `com/aptisprep/service/auth/impl/PasswordServiceImpl.java` | `PasswordServiceImpl` | `com.aptisprep.mvc.auth.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 208 | `com/aptisprep/service/auth/impl/UserServiceImpl.java` | `UserServiceImpl` | `com.aptisprep.mvc.auth.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 209 | `com/aptisprep/service/admin/impl/AdminDashboardServiceImpl.java` | `AdminDashboardServiceImpl` | `com.aptisprep.mvc.admin.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 210 | `com/aptisprep/service/admin/impl/AdminExamServiceImpl.java` | `AdminExamServiceImpl` | `com.aptisprep.mvc.admin.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 211 | `com/aptisprep/service/admin/impl/CatalogServiceImpl.java` | `CatalogServiceImpl` | `com.aptisprep.mvc.admin.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 212 | `com/aptisprep/service/admin/impl/StudentManagementServiceImpl.java` | `StudentManagementServiceImpl` | `com.aptisprep.mvc.admin.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 213 | `com/aptisprep/service/account/impl/AccountAccessServiceImpl.java` | `AccountAccessServiceImpl` | `com.aptisprep.mvc.account.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 214 | `com/aptisprep/service/account/impl/AccountProfileServiceImpl.java` | `AccountProfileServiceImpl` | `com.aptisprep.mvc.account.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 215 | `com/aptisprep/service/account/impl/LearningActivityServiceImpl.java` | `LearningActivityServiceImpl` | `com.aptisprep.mvc.account.service.impl` | Nhóm 1: SERVICE THẬT (Impl) |
| 216 | `com/aptisprep/dto/form/CertificateForm.java` | `CertificateFormDTO` | `com.aptisprep.mvc.scoring.dto` | DTO chứng chỉ & thang điểm |
| 217 | `com/aptisprep/dto/form/ExamForm.java` | `ExamFormDTO` | `com.aptisprep.mvc.catalog.dto` | DTO ngân hàng đề & câu hỏi |
| 218 | `com/aptisprep/dto/form/FlashcardSetForm.java` | `FlashcardSetFormDTO` | `com.aptisprep.mvc.flashcard.dto` | DTO flashcard |
| 219 | `com/aptisprep/dto/form/GoalForm.java` | `GoalFormDTO` | `com.aptisprep.mvc.account.dto` | DTO hồ sơ & mục tiêu |
| 220 | `com/aptisprep/dto/form/LessonForm.java` | `LessonFormDTO` | `com.aptisprep.mvc.learning.dto` | DTO bài học |
| 221 | `com/aptisprep/dto/form/PasswordForm.java` | `PasswordFormDTO` | `com.aptisprep.mvc.auth.dto` | DTO xác thực & tài khoản |
| 222 | `com/aptisprep/dto/form/ProfileForm.java` | `ProfileFormDTO` | `com.aptisprep.mvc.account.dto` | DTO hồ sơ & mục tiêu |
| 223 | `com/aptisprep/dto/form/QuestionForm.java` | `QuestionFormDTO` | `com.aptisprep.mvc.catalog.dto` | DTO ngân hàng đề & câu hỏi |
| 224 | `com/aptisprep/dto/form/RegisterForm.java` | `RegisterFormDTO` | `com.aptisprep.mvc.auth.dto` | DTO xác thực & tài khoản |
| 225 | `com/aptisprep/dto/form/ScoreBandForm.java` | `ScoreBandFormDTO` | `com.aptisprep.mvc.scoring.dto` | DTO chứng chỉ & thang điểm |
| 226 | `com/aptisprep/dto/form/ScoringScaleRuleForm.java` | `ScoringScaleRuleFormDTO` | `com.aptisprep.mvc.scoring.dto` | DTO chứng chỉ & thang điểm |
| 227 | `com/aptisprep/dto/form/ScoringSchemeForm.java` | `ScoringSchemeFormDTO` | `com.aptisprep.mvc.scoring.dto` | DTO chứng chỉ & thang điểm |
| 228 | `com/aptisprep/dto/view/AccountSnapshot.java` | `AccountSnapshotDTO` | `com.aptisprep.mvc.account.dto` | DTO hồ sơ & mục tiêu |
| 229 | `com/aptisprep/dto/view/AdminCatalogView.java` | `AdminCatalogViewDTO` | `com.aptisprep.mvc.catalog.dto` | DTO ngân hàng đề & câu hỏi |
| 230 | `com/aptisprep/dto/view/AdminDashboardView.java` | `AdminDashboardViewDTO` | `com.aptisprep.mvc.student.dto` | DTO học viên & dashboard |
| 231 | `com/aptisprep/dto/view/AdminStudentView.java` | `AdminStudentViewDTO` | `com.aptisprep.mvc.student.dto` | DTO học viên & dashboard |
| 232 | `com/aptisprep/dto/view/FlashcardViews.java` | `FlashcardViewsDTO` | `com.aptisprep.mvc.flashcard.dto` | DTO flashcard |
| 233 | `com/aptisprep/dto/view/StudentCatalogView.java` | `StudentCatalogViewDTO` | `com.aptisprep.mvc.student.dto` | DTO học viên & dashboard |
| 234 | `com/aptisprep/controller/admin/AdminCatalogController.java` | `AdminCatalogController` | `com.aptisprep.mvc.catalog.controller` | Controller ngân hàng câu hỏi & đề thi |
| 235 | `com/aptisprep/controller/admin/AdminDashboardController.java` | `AdminDashboardController` | `com.aptisprep.mvc.student.controller` | Controller quản lý học viên & dashboard |
| 236 | `com/aptisprep/controller/admin/AdminFlashcardController.java` | `AdminFlashcardController` | `com.aptisprep.mvc.flashcard.controller` | Controller flashcard |
| 237 | `com/aptisprep/controller/admin/AdminLessonController.java` | `AdminLessonController` | `com.aptisprep.mvc.learning.controller` | Controller bài học & lộ trình |
| 238 | `com/aptisprep/controller/admin/AdminResultController.java` | `AdminResultController` | `com.aptisprep.mvc.exam.controller` | Controller phòng thi & kết quả |
| 239 | `com/aptisprep/controller/admin/AdminScoringController.java` | `AdminScoringController` | `com.aptisprep.mvc.scoring.controller` | Controller cấu hình chứng chỉ & thang điểm |
| 240 | `com/aptisprep/controller/admin/AdminStudentController.java` | `AdminStudentController` | `com.aptisprep.mvc.student.controller` | Controller quản lý học viên & dashboard |
| 241 | `com/aptisprep/controller/auth/AuthController.java` | `AuthController` | `com.aptisprep.mvc.auth.controller` | Controller xác thực & mật khẩu |
| 242 | `com/aptisprep/controller/auth/PasswordController.java` | `PasswordController` | `com.aptisprep.mvc.auth.controller` | Controller xác thực & mật khẩu |
| 243 | `com/aptisprep/controller/student/StudentAccountController.java` | `StudentAccountController` | `com.aptisprep.mvc.account.controller` | Controller hồ sơ cá nhân & mục tiêu |
| 244 | `com/aptisprep/controller/student/StudentDashboardController.java` | `StudentDashboardController` | `com.aptisprep.mvc.student.controller` | Controller quản lý học viên & dashboard |
| 245 | `com/aptisprep/controller/student/StudentExamCatalogController.java` | `StudentExamCatalogController` | `com.aptisprep.mvc.exam.controller` | Controller phòng thi & kết quả |
| 246 | `com/aptisprep/controller/student/StudentExamController.java` | `StudentExamController` | `com.aptisprep.mvc.exam.controller` | Controller phòng thi & kết quả |
| 247 | `com/aptisprep/controller/student/StudentFlashcardController.java` | `StudentFlashcardController` | `com.aptisprep.mvc.flashcard.controller` | Controller flashcard |
| 248 | `com/aptisprep/controller/student/StudentLearningPathController.java` | `StudentLearningPathController` | `com.aptisprep.mvc.learning.controller` | Controller bài học & lộ trình |
| 249 | `com/aptisprep/controller/student/StudentLessonController.java` | `StudentLessonController` | `com.aptisprep.mvc.learning.controller` | Controller bài học & lộ trình |

---

## 3. DANH SÁCH VI PHẠM QUY TẮC THEO TỪNG CONTROLLER & SERVICE

### Vi phạm ở Controller

| Controller | Danh sách vi phạm phát hiện | Giải pháp khắc phục |
|---|---|---|
| `AiAssessmentController` | Imports Entity: com.aptisprep.entity.User | Chuyển phụ thuộc sang Service Interface, bỏ import Entity/Repository, bóc logic sang ServiceImpl. |
| `LessonAudioController` | Injects concrete class instead of interface: LessonService<br>Injects/Imports Repository: com.aptisprep.repository.LessonResponseRepository, com.aptisprep.repository.UserRepository<br>Imports Entity: com.aptisprep.entity.Lesson, com.aptisprep.entity.LessonResponse, com.aptisprep.entity.User | Chuyển triệt để UserRepository & LessonResponseRepository sang LessonAudioService hoặc LessonTeachingService. |
| `ListPagination` | Injects/Imports Impl: org.springframework.data.domain.PageImpl | Chuyển phụ thuộc sang Service Interface, bỏ import Entity/Repository, bóc logic sang ServiceImpl. |
| `AdminLessonController` | Imports Entity: com.aptisprep.entity.Lesson<br>Injects concrete class instead of interface: LessonService | Chuyển phụ thuộc sang Service Interface, bỏ import Entity/Repository, bóc logic sang ServiceImpl. |
| `AdminResultController` | Injects concrete class instead of interface: ExamAttemptService | Chuyển phụ thuộc sang Service Interface, bỏ import Entity/Repository, bóc logic sang ServiceImpl. |
| `AuthController` | Imports Entity: com.aptisprep.entity.User | Chuyển phụ thuộc sang Service Interface, bỏ import Entity/Repository, bóc logic sang ServiceImpl. |
| `StudentAccountController` | Injects concrete class instead of interface: ExamAttemptService | Chuyển phụ thuộc sang Service Interface, bỏ import Entity/Repository, bóc logic sang ServiceImpl. |
| `StudentDashboardController` | Injects concrete class instead of interface: LearningPathService | Chuyển phụ thuộc sang Service Interface, bỏ import Entity/Repository, bóc logic sang ServiceImpl. |
| `StudentExamCatalogController` | Injects concrete class instead of interface: ExamAttemptService | Chuyển phụ thuộc sang Service Interface, bỏ import Entity/Repository, bóc logic sang ServiceImpl. |
| `StudentExamController` | Injects concrete class instead of interface: ExamAttemptService | Chuyển phụ thuộc sang Service Interface, bỏ import Entity/Repository, bóc logic sang ServiceImpl. |
| `StudentLearningPathController` | Injects concrete class instead of interface: LearningPathService<br>Injects concrete class instead of interface: LessonService | Chuyển phụ thuộc sang Service Interface, bỏ import Entity/Repository, bóc logic sang ServiceImpl. |
| `StudentLessonController` | Imports Entity: com.aptisprep.entity.Lesson, com.aptisprep.entity.LessonMaterial<br>Injects concrete class instead of interface: LessonTeachingService<br>Injects concrete class instead of interface: LessonService | Chuyển phụ thuộc sang Service Interface, bỏ import Entity/Repository, bóc logic sang ServiceImpl. |

### Vi phạm ở Service
1.  **17 Service Class có `@Service` nhưng không phải `*ServiceImpl` hoặc thiếu Interface**:
    *   `PublishedExamCatalogService`, `ScoringSchemeUsageService`, `AttemptOutboxService`, `AttemptResultReader`, `ExamAttemptService`, `ExamSnapshotService`, `AiAssessmentService`, `AttemptFinalizationService`, `GeminiGradingService`, `ObjectiveGradingService`, `LearningPathService`, `LessonCheckService`, `LessonService`, `LessonTeachingService`, `PlacementExamCatalogService`, `CertificateSchemeService`, `AudioDurationService`.
    *   *Khắc phục*: Tách thành `interface XxxService` + `impl/XxxServiceImpl` đối với Service thật; bỏ `@Service` chuyển thành `@Component` package-private đối với Helper.
2.  **Truy cập Repository chéo Feature (Cross-Feature Repository Access)**:
    *   `FlashcardSetServiceImpl` đang inject trực tiếp `LessonRepository` (feature `learning`) thay vì gọi `LessonService`.
    *   `PlacementServiceImpl` đang inject trực tiếp `MockExamRepository` (feature `catalog`).
    *   `ExamAttemptService` đang inject trực tiếp `UserRepository` (feature `auth`).
    *   *Khắc phục*: Thay thế toàn bộ repository chéo bằng Service Interface của feature sở hữu repository đó.


---

## 4. ĐỒ THỊ PHỤ THUỘC GIỮA CÁC FEATURE & CÁC VÒNG CẦN PHÁ

### Đồ thị phụ thuộc hiện tại:
```text
scoring  ───────────────┐
   │                    │
storage ──────┐         │
   │          ▼         ▼
auth ────> account ──> learning <───> flashcard
             │            ▲
             ▼            │
          placement ───> exam <───> catalog
                          │  ▲
                          ▼  │
                         grading
```

### 10 Chu trình phụ thuộc vòng (Cyclic Dependencies) 2 chiều & Phương án phá vòng:

| Vòng lặp phát hiện | Nguyên nhân mã nguồn hiện tại | Phương án phá vòng chuẩn kiến trúc |
|---|---|---|
| **`flashcard <---> learning`** | `LessonService` gọi `FlashcardSetService.forLesson()`; ngược lại `FlashcardSetService` inject `LessonRepository`. | `FlashcardSetService` gọi `LessonService` (Interface). Không cho flashcard truy cập entity `Lesson` trực tiếp ngoài JPA ID. |
| **`account <---> placement`** | `StudentAccountController` gọi `PlacementService`; `PlacementService` gọi `AccountProfileService` để cập nhật profile. | Giữ chiều gọi Service 1 chiều: `account` gọi `PlacementService` qua Interface; `PlacementService` gọi `AccountProfileService` qua Interface. |
| **`account <---> learning`** | `GoalPathListener` nằm trong account nhưng gọi `LearningPathService`; `LearningPathService` gọi `AccountAccessContract`. | Chuyển `GoalPathListener` sang package `learning.service.impl` để lắng nghe `GoalChangedEvent` (đặt tại `common.event`). Khi đó `account` chỉ bắn event, không gọi sang `learning`. |
| **`account <---> exam`** | `StudentAccountController` gọi `ExamAttemptService`; `ExamAttemptService` gọi `AccountAccessService`. | `StudentAccountController` chỉ gọi `ExamAttemptService` (Interface); `exam` gọi `AccountAccessService` (Interface). |
| **`account <---> auth`** | `AccountProfileService` inject `UserRepository`; `AuthController` gọi `AccountAccessService`. | Đặt `User` và `UserRepository` tại `auth`. `AccountProfileService` gọi `UserService` (Interface). |
| **`catalog <---> learning`** | `Lesson` liên kết `QuestionEntity`; `CatalogService` tra cứu bài học. | `catalog` chỉ tra cứu qua ID hoặc `LessonService` Interface. |
| **`catalog <---> exam`** | `ExamAttempt` tạo từ `MockExam`; `CatalogService` kiểm tra bài làm đã thi. | Tách interface `ExamUsageService` hoặc dùng truy vấn qua ID đề thi. |
| **`exam <---> scoring`** | `ExamStartValidator` gọi `ScoringPolicyService`; `ScoringAdminService` kiểm tra attempt đã dùng scheme. | Thay kiểm tra scheme bằng event hoặc `ScoringSchemeUsageService` interface trong `catalog`. |
| **`exam <---> learning`** | `LearningPathService` gọi `ExamAttemptService` (kiểm tra checkpoint); `ExamAttemptService` gọi learning. | Khi bài thi Checkpoint hoàn thành, bắn `CheckpointAttemptCompletedEvent` để `learning` lắng nghe và mở chặng. |
| **`exam <---> grading`** | `ExamAttemptService` nộp bài gọi `AiAssessmentService`/`ObjectiveGradingService`; `AttemptFinalizationService` cập nhật `ExamAttempt`. | Sử dụng `AttemptSubmittedEvent` qua `AttemptGradingOrchestrationListener` để kích hoạt chấm điểm bất đồng bộ. |


---

## 5. DANH SÁCH NGOẠI LỆ AJAX / STREAM BẮT BUỘC PHẢI GIỮ

Các endpoint dưới đây phục vụ autosave ngầm, kiểm soát bảo mật lượt nghe bằng token, streaming tài nguyên media và tương tác học tập real-time. Các endpoint này được giữ nguyên URL và trả về DTO/Resource theo ngoại lệ B:

| STT | Endpoint | Phương thức | Kiểu trả về | Mục đích nghiệp vụ bắt buộc |
|:---:|---|:---:|---|---|
| 1 | `/student/attempts/{id}/answers/autosave` | POST | `ResponseEntity<Map<String, Object>>` | Tự động lưu đáp án trắc nghiệm/văn bản thi trực tuyến |
| 2 | `/student/attempts/{id}/answers/audio/autosave` | POST | `ResponseEntity<Map<String, Object>>` | Tự động lưu file ghi âm Speaking bài thi |
| 3 | `/student/attempts/{id}/media/{mediaId}/play` | POST | `ResponseEntity<?>` | Cấp token lượt nghe bảo mật (tối đa 2 lượt, BR-06) |
| 4 | `/student/attempts/{id}/media/{mediaId}/content` | GET | `ResponseEntity<Resource>` | Stream audio bài thi Nghe theo token xác thực |
| 5 | `/student/attempts/{id}/review/media/{mediaId}` | GET | `ResponseEntity<Resource>` | Stream audio trong chế độ xem lại kết quả bài thi |
| 6 | `/student/attempts/{id}/review/recordings/{answerId}` | GET | `ResponseEntity<Resource>` | Nghe lại bản ghi âm Speaking của thí sinh khi review |
| 7 | `/student/attempts/{id}/answers/speaking-audio` | GET | `ResponseEntity<Resource>` | Stream bản ghi âm bài nói đang làm dở |
| 8 | `/student/lessons/{id}/response/autosave` | POST | `@ResponseBody void` | Tự động lưu bài viết thực hành trong bài học |
| 9 | `/student/lessons/{id}/listening/start` | POST | `@ResponseBody Playback` | Khởi tạo phiên nghe bài học có xác thực |
| 10 | `/student/lessons/{id}/listening/heartbeat` | POST | `@ResponseBody Playback` | Báo cáo tiến độ nghe liên tục để mở khóa câu hỏi |
| 11 | `/student/lessons/{id}/audio-played` | POST | `@ResponseBody void` | Endpoint từ chối hack tua audio không hợp lệ |
| 12 | `/student/lessons/{id}/section` | POST | `@ResponseBody void` | Đồng bộ chuyển phần học lý thuyết |
| 13 | `/student/lessons/{id}/audio` | GET | `ResponseEntity<Resource>` | Stream file âm thanh bài học bảo vệ |
| 14 | `/student/lessons/{id}/recording` | GET | `ResponseEntity<Resource>` | Stream file ghi âm học viên trong bài học |
| 15 | `/student/lessons/{id}/image` | GET | `ResponseEntity<Resource>` | Phục vụ hình ảnh minh họa bài học |
| 16 | `/student/lessons/{id}/video` | GET | `ResponseEntity<Resource>` | Stream video bài giảng bài học |
| 17 | `/student/lessons/{id}/history-media/{item}` | GET | `ResponseEntity<Resource>` | Stream media lịch sử phiên học cũ |
| 18 | `/student/lessons/{id}/submission-audio/{id}` | GET | `ResponseEntity<Resource>` | Stream audio bài tập đã nộp của học viên |
| 19 | `/admin/media/{id}/content` | GET | `ResponseEntity<Resource>` | Quản trị viên nghe thử media câu hỏi khi biên soạn |
| 20 | `/student/flashcards/{setId}/cards/{cardId}/state` | POST | `ResponseEntity<Void>` | Cập nhật đánh giá nhớ thẻ từ vựng theo thuật toán SRS |


---

## 6. KẾ HOẠCH THỰC HIỆN CÁC BƯỚC TIẾP THEO (BƯỚC 1 -> BƯỚC 6)

*   **Bước 1**: Đã xác nhận baseline: `mvn test` chạy **345 tests, 0 failures, 0 errors, 6 skipped** thành công.
*   **Bước 2**: Tạo cấu trúc package `com.aptisprep.mvc.common` và chuyển `config`, `security`, `exception`, `enums`, `event`, `util`. Kiểm tra `mvn test`.
*   **Bước 3**: Chuyển từng feature một theo thứ tự ít phụ thuộc nhất:
    1. `scoring`
    2. `storage`
    3. `auth`
    4. `account`
    5. `flashcard`
    6. `learning`
    7. `catalog`
    8. `exam`
    9. `grading`
    10. `placement`
    11. `student`
    *Mỗi feature sau khi chuyển đều chạy `mvn spotless:apply` và `mvn test` đảm bảo xanh trước khi commit riêng.*
*   **Bước 4**: Di chuyển các test tương ứng sang package mới.
*   **Bước 5**: Bổ sung bộ test kiểm tra kiến trúc bằng **ArchUnit** để khóa cứng các quy tắc:
    - Controller không phụ thuộc Repository / Entity / Impl.
    - `@Service` chỉ xuất hiện ở class `*ServiceImpl` trong `..service.impl..`.
    - Interface service không chứa logic cài đặt.
    - Không tồn tại `@RestController`.
    - Feature không dùng Repository của feature khác.
*   **Bước 6**: Rà soát tổng thể bằng grep & ArchUnit, xuất báo cáo hoàn thành.
