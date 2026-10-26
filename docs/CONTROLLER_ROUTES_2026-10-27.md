# Danh sách endpoint thực tế — 27/10/2026

Tổng: 18 controller, 123 endpoint. Dữ liệu lấy từ RequestMappingHandlerMapping trên H2 tạm; không ghi DB ứng dụng.

Mỗi handler có một URL và một HTTP method; không trùng cặp HTTP method + URL, không map hàm private và không dùng trùng tên các handler trong cùng controller.

| HTTP | URL đầy đủ | Hàm nhận request | Loại trả về |
| --- | --- | --- | --- |
| GET | `/` | `HomeController.home()` | view / redirect |
| GET | `/access-denied` | `HomeController.accessDenied()` | view / redirect |
| GET | `/admin/assessments/{key}` | `AiAssessmentController.showAdminAssessment()` | view / redirect |
| GET | `/admin/certificates/create` | `AdminScoringController.createCertificate()` | view / redirect |
| GET | `/admin/certificates/{id}/edit` | `AdminScoringController.editCertificate()` | view / redirect |
| GET | `/admin/certificates/{id}` | `AdminScoringController.certificate()` | view / redirect |
| GET | `/admin/certificates` | `AdminScoringController.showCertificates()` | view / redirect |
| GET | `/admin/dashboard` | `AdminDashboardController.dashboard()` | view / redirect |
| GET | `/admin/exams/create` | `AdminCatalogController.createExam()` | view / redirect |
| GET | `/admin/exams/{id}/edit` | `AdminCatalogController.editExam()` | view / redirect |
| GET | `/admin/exams/{id}` | `AdminCatalogController.exam()` | view / redirect |
| GET | `/admin/exams` | `AdminCatalogController.exams()` | view / redirect |
| GET | `/admin/flashcards/create` | `AdminFlashcardController.showCreateForm()` | view / redirect |
| GET | `/admin/flashcards/{id}/edit` | `AdminFlashcardController.showEditForm()` | view / redirect |
| GET | `/admin/flashcards` | `AdminFlashcardController.showFlashcardList()` | view / redirect |
| GET | `/admin/lessons/create` | `AdminLessonController.showCreateForm()` | view / redirect |
| GET | `/admin/lessons/{id}/edit` | `AdminLessonController.showEditForm()` | view / redirect |
| GET | `/admin/lessons/{id}` | `AdminLessonController.detail()` | view / redirect |
| GET | `/admin/lessons` | `AdminLessonController.list()` | view / redirect |
| GET | `/admin/questions/create` | `AdminCatalogController.createQuestion()` | view / redirect |
| GET | `/admin/questions/{id}/edit` | `AdminCatalogController.editQuestion()` | view / redirect |
| GET | `/admin/questions/{id}/media/{mediaId}/content` | `AdminCatalogController.mediaContent()` | resource / response |
| GET | `/admin/questions/{id}` | `AdminCatalogController.question()` | view / redirect |
| GET | `/admin/questions` | `AdminCatalogController.questions()` | view / redirect |
| GET | `/admin/results/{id}` | `AdminResultController.detail()` | view / redirect |
| GET | `/admin/results` | `AdminResultController.list()` | view / redirect |
| GET | `/admin/scoring-schemes/create` | `AdminScoringController.createScheme()` | view / redirect |
| GET | `/admin/scoring-schemes/{id}/edit` | `AdminScoringController.editScheme()` | view / redirect |
| GET | `/admin/scoring-schemes/{id}` | `AdminScoringController.scheme()` | view / redirect |
| GET | `/admin/scoring-schemes` | `AdminScoringController.showScoringSchemes()` | view / redirect |
| GET | `/admin/settings` | `AdminScoringController.settings()` | view / redirect |
| GET | `/admin/students/{id}` | `AdminStudentController.detail()` | view / redirect |
| GET | `/admin/students` | `AdminStudentController.list()` | view / redirect |
| GET | `/lesson-media/{id}/audio` | `LessonAudioController.audio()` | resource / response |
| GET | `/lesson-media/{id}/image` | `LessonAudioController.image()` | resource / response |
| GET | `/lesson-media/{id}/video` | `LessonAudioController.video()` | resource / response |
| GET | `/login` | `AuthController.login()` | view / redirect |
| GET | `/register` | `AuthController.registerForm()` | view / redirect |
| GET | `/student/assessments/{key}` | `AiAssessmentController.showStudentAssessment()` | view / redirect |
| GET | `/student/attempts/{id}/answers/{answerId}/audio` | `StudentExamController.speakingAudio()` | resource / response |
| GET | `/student/attempts/{id}/media/{mediaId}/content` | `StudentExamController.mediaContent()` | resource / response |
| GET | `/student/attempts/{id}/result` | `StudentContentController.result()` | view / redirect |
| GET | `/student/attempts/{id}` | `StudentExamController.attempt()` | view / redirect |
| GET | `/student/dashboard` | `StudentDashboardController.dashboard()` | view / redirect |
| GET | `/student/exams/{id}` | `StudentContentController.detail()` | view / redirect |
| GET | `/student/exams` | `StudentContentController.list()` | view / redirect |
| GET | `/student/flashcards/create` | `StudentFlashcardController.showCreateForm()` | view / redirect |
| GET | `/student/flashcards/{id}/edit` | `StudentFlashcardController.showEditForm()` | view / redirect |
| GET | `/student/flashcards/{id}` | `StudentFlashcardController.study()` | view / redirect |
| GET | `/student/flashcards` | `StudentFlashcardController.list()` | view / redirect |
| GET | `/student/goals` | `StudentAccountController.goals()` | view / redirect |
| GET | `/student/learning-path/{skill}` | `StudentLearningPathController.skill()` | view / redirect |
| GET | `/student/learning-path` | `StudentLearningPathController.paths()` | view / redirect |
| GET | `/student/lessons/{id}/history/{kind}/{historyId}/media/{media}` | `LessonAudioController.historyMedia()` | resource / response |
| GET | `/student/lessons/{id}/recording` | `LessonAudioController.recording()` | resource / response |
| GET | `/student/lessons/{id}/submissions/{submissionId}/audio` | `LessonAudioController.submissionAudio()` | resource / response |
| GET | `/student/lessons/{id}` | `StudentLessonController.detail()` | view / redirect |
| GET | `/student/lessons` | `StudentLessonController.list()` | view / redirect |
| GET | `/student/orientation` | `StudentDashboardController.orientation()` | view / redirect |
| GET | `/student/placement/result` | `StudentAccountController.result()` | view / redirect |
| GET | `/student/placement` | `StudentAccountController.placement()` | view / redirect |
| GET | `/student/profile/edit` | `StudentAccountController.profileEdit()` | view / redirect |
| GET | `/student/profile` | `StudentAccountController.profile()` | view / redirect |
| GET | `/student/results` | `StudentContentController.results()` | view / redirect |
| POST | `/admin/assessments/{key}/retry` | `AiAssessmentController.retryAdminAssessment()` | view / redirect |
| POST | `/admin/certificates/{id}/disable` | `AdminScoringController.disableCertificate()` | view / redirect |
| POST | `/admin/certificates/{id}` | `AdminScoringController.updateCertificate()` | view / redirect |
| POST | `/admin/certificates` | `AdminScoringController.saveCertificate()` | view / redirect |
| POST | `/admin/exams/{id}/edit` | `AdminCatalogController.updateExam()` | view / redirect |
| POST | `/admin/exams/{id}/publish` | `AdminCatalogController.publishExam()` | view / redirect |
| POST | `/admin/exams` | `AdminCatalogController.saveExam()` | view / redirect |
| POST | `/admin/flashcards/{id}` | `AdminFlashcardController.updateFlashcardSet()` | view / redirect |
| POST | `/admin/flashcards` | `AdminFlashcardController.createFlashcardSet()` | view / redirect |
| POST | `/admin/lessons/{id}/archive` | `AdminLessonController.archive()` | view / redirect |
| POST | `/admin/lessons/{id}/duplicate` | `AdminLessonController.duplicate()` | view / redirect |
| POST | `/admin/lessons/{id}/move` | `AdminLessonController.move()` | view / redirect |
| POST | `/admin/lessons/{id}` | `AdminLessonController.updateLesson()` | view / redirect |
| POST | `/admin/lessons` | `AdminLessonController.createLesson()` | view / redirect |
| POST | `/admin/questions/{id}/archive` | `AdminCatalogController.archiveQuestion()` | view / redirect |
| POST | `/admin/questions/{id}/edit` | `AdminCatalogController.updateQuestion()` | view / redirect |
| POST | `/admin/questions/{id}/media/{mediaId}/delete` | `AdminCatalogController.deleteMedia()` | view / redirect |
| POST | `/admin/questions/{id}/media/{mediaId}/move` | `AdminCatalogController.moveMedia()` | view / redirect |
| POST | `/admin/questions/{id}/media/{mediaId}` | `AdminCatalogController.updateMedia()` | view / redirect |
| POST | `/admin/questions/{id}/media` | `AdminCatalogController.addMedia()` | view / redirect |
| POST | `/admin/questions/{id}/publish` | `AdminCatalogController.publishQuestion()` | view / redirect |
| POST | `/admin/questions/{id}/restore` | `AdminCatalogController.restoreQuestion()` | view / redirect |
| POST | `/admin/questions` | `AdminCatalogController.saveQuestion()` | view / redirect |
| POST | `/admin/scoring-schemes/{id}/disable` | `AdminScoringController.disableScheme()` | view / redirect |
| POST | `/admin/scoring-schemes/{id}` | `AdminScoringController.updateScheme()` | view / redirect |
| POST | `/admin/scoring-schemes/{schemeId}/bands/{bandId}/delete` | `AdminScoringController.deleteBand()` | view / redirect |
| POST | `/admin/scoring-schemes/{schemeId}/bands` | `AdminScoringController.saveBand()` | view / redirect |
| POST | `/admin/scoring-schemes/{schemeId}/scale-rule` | `AdminScoringController.saveScaleRule()` | view / redirect |
| POST | `/admin/scoring-schemes` | `AdminScoringController.saveScheme()` | view / redirect |
| POST | `/admin/students/{id}/lock` | `AdminStudentController.lock()` | view / redirect |
| POST | `/admin/students/{id}/unlock` | `AdminStudentController.unlock()` | view / redirect |
| POST | `/register` | `AuthController.register()` | view / redirect |
| POST | `/student/assessments/{key}/retry` | `AiAssessmentController.retryStudentAssessment()` | view / redirect |
| POST | `/student/attempts/{id}/answers/audio` | `StudentExamController.audio()` | view / redirect |
| POST | `/student/attempts/{id}/answers/autosave` | `StudentExamController.autosave()` | response body |
| POST | `/student/attempts/{id}/answers` | `StudentExamController.answer()` | view / redirect |
| POST | `/student/attempts/{id}/media/{mediaId}/play` | `StudentExamController.playMedia()` | response body |
| POST | `/student/attempts/{id}/retry-grading` | `StudentContentController.retryGrading()` | view / redirect |
| POST | `/student/attempts/{id}/submit` | `StudentExamController.submit()` | view / redirect |
| POST | `/student/exams/{id}/start` | `StudentExamController.start()` | view / redirect |
| POST | `/student/flashcards/{id}/archive` | `StudentFlashcardController.archive()` | view / redirect |
| POST | `/student/flashcards/{id}` | `StudentFlashcardController.updatePersonalSet()` | view / redirect |
| POST | `/student/flashcards/{setId}/cards/{cardId}/state` | `StudentFlashcardController.mark()` | response body |
| POST | `/student/flashcards` | `StudentFlashcardController.createPersonalSet()` | view / redirect |
| POST | `/student/goals` | `StudentAccountController.saveGoal()` | view / redirect |
| POST | `/student/learning-path/refresh` | `StudentLearningPathController.refresh()` | view / redirect |
| POST | `/student/learning-path/{pathId}/checkpoint/start` | `StudentLearningPathController.startCheckpoint()` | view / redirect |
| POST | `/student/lessons/{id}/assess` | `StudentLessonController.assess()` | view / redirect |
| POST | `/student/lessons/{id}/audio-played` | `StudentLessonController.audioPlayed()` | response body |
| POST | `/student/lessons/{id}/check` | `StudentLessonController.check()` | view / redirect |
| POST | `/student/lessons/{id}/complete` | `StudentLessonController.complete()` | view / redirect |
| POST | `/student/lessons/{id}/listening/heartbeat` | `StudentLessonController.listeningHeartbeat()` | response body |
| POST | `/student/lessons/{id}/listening/start` | `StudentLessonController.startListening()` | response body |
| POST | `/student/lessons/{id}/recording` | `StudentLessonController.recording()` | view / redirect |
| POST | `/student/lessons/{id}/response/autosave` | `StudentLessonController.autosave()` | response body |
| POST | `/student/lessons/{id}/response` | `StudentLessonController.response()` | view / redirect |
| POST | `/student/lessons/{id}/section` | `StudentLessonController.section()` | response body |
| POST | `/student/placement/start` | `StudentAccountController.start()` | view / redirect |
| POST | `/student/profile/edit` | `StudentAccountController.updateProfile()` | view / redirect |
