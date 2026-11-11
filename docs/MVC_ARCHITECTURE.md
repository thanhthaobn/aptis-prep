# MVC architecture

The application uses one top-level package per layer. Controllers coordinate HTTP input,
form/view DTOs and services; they do not access repositories or JPA entities directly.

```text
com.aptisprep
├── controller
│   ├── auth
│   ├── admin
│   └── student
├── dto
│   ├── form
│   └── view
├── entity
├── repository
├── service
│   ├── account
│   ├── admin
│   ├── exam
│   ├── grading
│   ├── learning
│   ├── scoring
│   └── storage
├── event
├── security
├── config
└── exception
```

## Page ownership

| Controller | Coordinated views |
| --- | --- |
| `HomeController` | `index`, `error/403` |
| `AuthController`, `PasswordController` | `auth/*` |
| `AdminDashboardController` | `admin/dashboard` |
| `AdminCatalogController` | `admin/catalog/*` |
| `AdminLessonController` | `admin/lesson/*` |
| `AdminFlashcardController` | `admin/flashcards/*` |
| `AdminResultController` | `admin/result/*` |
| `AdminScoringController` | `admin/settings/*` |
| `AdminStudentController` | `admin/student/*`, `admin/unlock-requests` |
| `StudentDashboardController` | `student/dashboard` |
| `StudentAccountController` | `student/account/*`, placement and account result pages |
| `StudentExamCatalogController` | exam catalog, result list/detail pages |
| `StudentExamController` | active attempt and attempt-review pages |
| `StudentLearningPathController` | `student/learning/*` |
| `StudentLessonController` | `student/lesson/*` |
| `StudentFlashcardController` | `student/flashcards/*` |
| `AiAssessmentController` | `student/assessment` |
| `LessonAudioController` | lesson audio HTTP response (no template) |

Form-bound classes belong to `dto.form` and carry Jakarta validation constraints. DTOs that
convert a JPA entity use the entity simple name plus `DTO`, and expose
`convertEntityToDto`/`convertDtoToEntity`. JPA entities provide an empty constructor, a full
field constructor and JavaBean accessors.
