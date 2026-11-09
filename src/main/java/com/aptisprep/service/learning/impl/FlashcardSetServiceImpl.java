package com.aptisprep.service.learning.impl;

import com.aptisprep.dto.form.FlashcardSetForm;
import com.aptisprep.dto.form.FlashcardSetForm.Row;
import com.aptisprep.dto.view.FlashcardViews.*;
import com.aptisprep.entity.*;
import com.aptisprep.enums.*;
import com.aptisprep.exception.*;
import com.aptisprep.repository.*;
import com.aptisprep.service.learning.FlashcardSetService;
import com.aptisprep.service.learning.LessonService;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FlashcardSetServiceImpl implements FlashcardSetService {
    private final FlashcardSetRepository flashcardSetRepository;
    private final FlashcardRepository flashcardRepository;
    private final UserRepository userRepository;
    private final UserFlashcardProgressRepository userFlashcardProgressRepository;
    private final LessonRepository lessonRepository;
    @PersistenceContext private EntityManager em;

    public FlashcardSetServiceImpl(
            FlashcardSetRepository flashcardSetRepository,
            FlashcardRepository flashcardRepository,
            UserRepository userRepository,
            UserFlashcardProgressRepository userFlashcardProgressRepository,
            LessonRepository lessonRepository) {
        this.flashcardSetRepository = flashcardSetRepository;
        this.flashcardRepository = flashcardRepository;
        this.userRepository = userRepository;
        this.userFlashcardProgressRepository = userFlashcardProgressRepository;
        this.lessonRepository = lessonRepository;
    }

    public static List<SkillSets> groupBySkillAndLevel(List<SetView> values) {
        List<SkillSets> branches = new ArrayList<>();
        for (String skill : LessonService.SKILLS) {
            List<LevelSets> levels = new ArrayList<>();
            int setCount = 0;
            for (String level : LessonService.LEVELS) {
                List<SetView> grouped = lessonSetsFor(values, skill, level);
                if (grouped.isEmpty()) continue;
                levels.add(new LevelSets(level, grouped));
                setCount += grouped.size();
            }
            if (!levels.isEmpty()) branches.add(new SkillSets(skill, setCount, levels));
        }
        return List.copyOf(branches);
    }

    @Transactional(readOnly = true)
    public StudySetView studySet(Integer id, String email) {
        FlashcardSet set = requireForStudent(id, email);
        Lesson lesson = set.getLesson();
        LessonLink link =
                lesson == null
                        ? null
                        : new LessonLink(lesson.getId(), lesson.getTitle(), lesson.getSkill());
        return new StudySetView(
                set.getId(), set.getTitle(), set.getDescription(), set.getCefrLevel(), link);
    }

    public List<SetView> list(boolean admin) {
        return view(
                (admin
                                ? flashcardSetRepository.findByOwnerIsNullOrderByIdDesc()
                                : flashcardSetRepository.findByStatusOrderByIdDesc("PUBLISHED"))
                        .stream()
                                .filter(
                                        s ->
                                                s.getOwner() == null
                                                        && !"ARCHIVED".equals(s.getStatus()))
                                .toList());
    }

    public List<SetView> listForStudent(String email) {
        User u = learner(email);
        ArrayList<FlashcardSet> all = new ArrayList<FlashcardSet>();
        all.addAll(
                flashcardSetRepository.findByStatusOrderByIdDesc("PUBLISHED").stream()
                        .filter(this::publishedLessonSet)
                        .toList());
        all.addAll(
                flashcardSetRepository.findByOwnerIdOrderByIdDesc(u.getId()).stream()
                        .filter(s -> !"ARCHIVED".equals(s.getStatus()))
                        .toList());
        return view(all);
    }

    private List<SetView> view(List<FlashcardSet> values) {
        return values.stream()
                .map(
                        s ->
                                new SetView(
                                        s.getId(),
                                        s.getTitle(),
                                        s.getDescription(),
                                        s.getCefrLevel(),
                                        s.getStatus(),
                                        flashcardRepository
                                                .findBySetIdAndStatusOrderByDisplayOrderAsc(
                                                        s.getId(), "ACTIVE")
                                                .size(),
                                        s.getOwner() != null,
                                        s.getLesson() == null ? null : s.getLesson().getId(),
                                        s.getLesson() == null ? null : s.getLesson().getTitle(),
                                        s.getLesson() == null ? null : s.getLesson().getSkill()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Integer, SetView> lessonSets() {
        LinkedHashMap<Integer, SetView> result = new LinkedHashMap<Integer, SetView>();
        for (SetView set :
                view(
                        flashcardSetRepository.findByStatusOrderByIdDesc("PUBLISHED").stream()
                                .filter(
                                        s ->
                                                s.getOwner() == null
                                                        && s.getLesson() != null
                                                        && "PUBLISHED"
                                                                .equals(s.getLesson().getStatus()))
                                .toList())) result.put(set.lessonId(), set);
        return result;
    }

    public FlashcardSet require(Integer id, boolean admin) {
        return flashcardSetRepository
                .findById(id)
                .filter(s -> s.getOwner() == null && (admin || "PUBLISHED".equals(s.getStatus())))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ thẻ"));
    }

    public FlashcardSet requireForStudent(Integer id, String email) {
        User u = learner(email);
        return flashcardSetRepository
                .findById(id)
                .filter(
                        s ->
                                publishedLessonSet(s)
                                        || (s.getOwner() != null
                                                && s.getOwner().getId().equals(u.getId())
                                                && !"ARCHIVED".equals(s.getStatus())))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ thẻ"));
    }

    private boolean publishedLessonSet(FlashcardSet set) {
        return set.getOwner() == null
                && set.getLesson() != null
                && "PUBLISHED".equals(set.getStatus())
                && "PUBLISHED".equals(set.getLesson().getStatus());
    }

    public FlashcardSetForm form(Integer id) {
        return toForm(require(id, true));
    }

    private FlashcardSetForm toForm(FlashcardSet s) {
        FlashcardSetForm f = new FlashcardSetForm();
        f.setTitle(s.getTitle());
        f.setDescription(s.getDescription());
        f.setCefrLevel(s.getCefrLevel());
        f.setStatus(s.getStatus());
        f.setLessonId(s.getLesson() == null ? null : s.getLesson().getId());
        for (Flashcard c :
                flashcardRepository.findBySetIdAndStatusOrderByDisplayOrderAsc(
                        s.getId(), "ACTIVE")) {
            Row r = new FlashcardSetForm.Row();
            r.setId(c.getId());
            r.setTerm(c.getFrontContent());
            r.setDefinition(c.getBackContent());
            f.getRows().add(r);
        }
        return f;
    }

    private Integer save(Integer id, FlashcardSetForm f, Lesson lesson, User owner) {
        if (!LessonService.LEVELS.contains(f.getCefrLevel())
                || !LessonService.STATUSES.contains(f.getStatus()))
            throw new BusinessRuleException("Cấp độ hoặc trạng thái không hợp lệ");
        if (f.getRows() == null || f.getRows().isEmpty() || f.getRows().size() > 200)
            throw new BusinessRuleException("Bộ thẻ cần từ 1 đến 200 cặp từ và nghĩa");
        FlashcardSet set =
                id == null
                        ? new FlashcardSet()
                        : flashcardSetRepository
                                .findById(id)
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Không tìm thấy bộ thẻ"));
        if (id != null) em.lock(set, LockModeType.PESSIMISTIC_WRITE);
        List<Flashcard> existing =
                id == null
                        ? List.<Flashcard>of()
                        : flashcardRepository.findBySetIdAndStatusOrderByDisplayOrderAsc(
                                id, "ACTIVE");
        HashMap<Integer, Flashcard> byId = new HashMap<Integer, Flashcard>();
        existing.forEach(c -> byId.put(c.getId(), c));
        HashSet<Integer> seen = new HashSet<Integer>();
        for (Row row : f.getRows()) {
            if (row.getTerm() == null
                    || row.getTerm().isBlank()
                    || row.getDefinition() == null
                    || row.getDefinition().isBlank()
                    || row.getTerm().length() > 200
                    || row.getDefinition().length() > 1000)
                throw new BusinessRuleException(
                        "Mỗi thẻ cần thuật ngữ và định nghĩa. Tối đa 200 ký tự cho thuật ngữ và"
                                + " 1000 cho định nghĩa");
            if (row.getId() != null && (!byId.containsKey(row.getId()) || !seen.add(row.getId())))
                throw new BusinessRuleException("Thẻ không thuộc bộ này hoặc bị lặp");
        }
        if (lesson != null) {
            FlashcardSet linked =
                    flashcardSetRepository
                            .findFirstByLessonIdAndOwnerIsNullOrderByIdDesc(lesson.getId())
                            .orElse(null);
            if (linked != null && !Objects.equals(linked.getId(), id))
                throw new BusinessRuleException("Bài học này đã có một bộ thẻ");
        }
        set.setTitle(f.getTitle().trim());
        set.setDescription(f.getDescription());
        set.setCefrLevel(f.getCefrLevel());
        set.setStatus(f.getStatus());
        set.setLesson(lesson);
        set.setOwner(owner);
        set.setVisibility(owner == null ? "PUBLIC" : "PRIVATE");
        flashcardSetRepository.save(set);
        int position = existing.stream().mapToInt(Flashcard::getDisplayOrder).max().orElse(0) + 1;
        for (Flashcard c : existing) {
            c.setStatus("ARCHIVED");
            flashcardRepository.save(c);
        }
        flashcardRepository.flush();
        for (Row row : f.getRows()) {
            Flashcard c = row.getId() == null ? new Flashcard() : byId.get(row.getId());
            c.setSet(set);
            c.setFrontContent(row.getTerm().trim());
            c.setBackContent(row.getDefinition().trim());
            c.setCefrLevel(f.getCefrLevel());
            c.setDisplayOrder(position++);
            c.setStatus("ACTIVE");
            flashcardRepository.save(c);
        }
        return set.getId();
    }

    public Integer saveAdmin(Integer id, FlashcardSetForm f) {
        if (id != null) require(id, true);
        if (f.getLessonId() == null)
            throw new BusinessRuleException("Hãy chọn bài học sử dụng bộ thẻ này");
        Lesson lesson =
                lessonRepository
                        .findById(f.getLessonId())
                        .filter(l -> !"ARCHIVED".equals(l.getStatus()))
                        .orElseThrow(() -> new BusinessRuleException("Bài học không còn khả dụng"));
        if (!lesson.getCefrLevel().equals(f.getCefrLevel()))
            throw new BusinessRuleException("Cấp độ bộ thẻ phải trùng với cấp độ bài học");
        return save(id, f, lesson, null);
    }

    private User learner(String email) {
        User u =
                userRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (u.getStatus() != UserStatus.ACTIVE || u.getRole() != Role.STUDENT)
            throw new BusinessRuleException("Tài khoản không được phép học");
        return u;
    }

    public FlashcardSetForm personalForm(Integer id, String email) {
        FlashcardSet set = requireForStudent(id, email);
        if (set.getOwner() == null)
            throw new BusinessRuleException("Không thể sửa bộ thẻ thư viện");
        return toForm(set);
    }

    public Integer savePersonal(Integer id, FlashcardSetForm f, String email) {
        User u = learner(email);
        if (id != null && requireForStudent(id, email).getOwner() == null)
            throw new BusinessRuleException("Không thể sửa bộ thẻ thư viện");
        f.setStatus("PUBLISHED");
        f.setLessonId(null);
        return save(id, f, null, u);
    }

    public void archivePersonal(Integer id, String email) {
        User u = learner(email);
        FlashcardSet set =
                flashcardSetRepository
                        .findById(id)
                        .filter(
                                s ->
                                        s.getOwner() != null
                                                && s.getOwner().getId().equals(u.getId())
                                                && !"ARCHIVED".equals(s.getStatus()))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy bộ thẻ cá nhân"));
        set.setStatus("ARCHIVED");
        flashcardSetRepository.save(set);
    }

    public List<CardView> study(Integer id, String email) {
        User u = learner(email);
        requireForStudent(id, email);
        return flashcardRepository.findBySetIdAndStatusOrderByDisplayOrderAsc(id, "ACTIVE").stream()
                .map(
                        c -> {
                            UserFlashcardProgress p =
                                    userFlashcardProgressRepository
                                            .findByUserIdAndFlashcardId(u.getId(), c.getId())
                                            .orElse(null);
                            return new CardView(
                                    c.getId(),
                                    c.getFrontContent(),
                                    c.getBackContent(),
                                    p == null ? "LEARNING" : p.getStatus(),
                                    p == null ? null : p.getNextReviewAt());
                        })
                .sorted(
                        Comparator.comparing(
                                CardView::nextReviewAt,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
    }

    public FlashcardSet publishedForLesson(Integer lessonId) {
        return flashcardSetRepository
                .findFirstByLessonIdAndOwnerIsNullAndStatusOrderByIdDesc(lessonId, "PUBLISHED")
                .orElse(null);
    }

    public FlashcardSet forLesson(Integer lessonId) {
        return flashcardSetRepository
                .findFirstByLessonIdAndOwnerIsNullOrderByIdDesc(lessonId)
                .orElse(null);
    }

    public List<Lesson> availableLessons() {
        return lessonRepository.findAllByOrderBySkillAscCefrLevelAscDisplayOrderAsc().stream()
                .filter(l -> !"ARCHIVED".equals(l.getStatus()))
                .toList();
    }

    public void prepareForLesson(FlashcardSetForm form, Integer lessonId) {
        if (lessonId == null) return;
        Lesson lesson =
                lessonRepository
                        .findById(lessonId)
                        .filter(l -> !"ARCHIVED".equals(l.getStatus()))
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
        form.setLessonId(lesson.getId());
        form.setCefrLevel(lesson.getCefrLevel());
    }

    public void mark(Integer setId, Integer cardId, String state, String email) {
        mark(setId, cardId, state, email, UUID.randomUUID().toString());
    }

    public void mark(Integer setId, Integer cardId, String state, String email, String reviewKey) {
        if (!"LEARNING".equals(state) && !"MASTERED".equals(state))
            throw new BusinessRuleException("Trạng thái không hợp lệ");
        if (reviewKey == null || !reviewKey.matches("[A-Za-z0-9-]{16,80}"))
            throw new BusinessRuleException("Mã lượt ôn không hợp lệ");

        User user = learner(email);
        em.lock(user, LockModeType.PESSIMISTIC_WRITE);
        requireForStudent(setId, email);
        Flashcard card =
                flashcardRepository
                        .findById(cardId)
                        .filter(
                                value ->
                                        value.getSet() != null
                                                && value.getSet().getId().equals(setId)
                                                && "ACTIVE".equals(value.getStatus()))
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ"));
        String key = user.getId() + ":" + cardId + ":" + reviewKey;
        FlashcardReview prior = em.find(FlashcardReview.class, key);
        if (prior != null) {
            if (!state.equals(prior.getState()))
                throw new BusinessRuleException("Mã lượt ôn đã được dùng cho nội dung khác");
            return;
        }

        UserFlashcardProgress value =
                userFlashcardProgressRepository
                        .findByUserIdAndFlashcardId(user.getId(), cardId)
                        .orElseGet(
                                () -> {
                                    UserFlashcardProgress created = new UserFlashcardProgress();
                                    created.setUser(user);
                                    created.setFlashcard(card);
                                    created.setReviewCount(0);
                                    return created;
                                });
        LocalDateTime now = LocalDateTime.now();
        value.setReviewCount(value.getReviewCount() + 1);
        value.setLastReviewedAt(now);
        value.setStatus(state);
        value.setMasteryLevel("MASTERED".equals(state) ? 5 : 1);
        int previous = value.getIntervalDays() == null ? 0 : value.getIntervalDays();
        double ease = value.getEaseFactor() == null ? 2.5 : value.getEaseFactor().doubleValue();
        int days =
                "MASTERED".equals(state)
                        ? Math.min(365, previous == 0 ? 1 : (int) Math.ceil(previous * ease))
                        : 0;
        value.setIntervalDays(days);
        value.setNextReviewAt(days == 0 ? now.plusMinutes(10) : now.plusDays(days));
        userFlashcardProgressRepository.save(value);
        em.persist(new FlashcardReview(key, user.getId(), cardId, state, now));
    }

    private static List<SetView> lessonSetsFor(List<SetView> values, String skill, String level) {
        List<SetView> result = new ArrayList<>();
        for (SetView set : values) {
            if (!set.personal() && skill.equals(set.lessonSkill()) && level.equals(set.level())) {
                result.add(set);
            }
        }
        return List.copyOf(result);
    }
}
