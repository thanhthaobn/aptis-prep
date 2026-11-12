package com.aptisprep.mvc.service.learning.impl;

import com.aptisprep.mvc.dto.*;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import com.aptisprep.mvc.exception.*;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.learning.FlashcardSetService;
import com.aptisprep.mvc.service.learning.LessonService;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.text.Normalizer;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FlashcardSetServiceImpl implements FlashcardSetService {
    private FlashcardSetRepository flashcardSetRepository;
    private FlashcardRepository flashcardRepository;
    private UserRepository userRepository;
    private UserFlashcardProgressRepository userFlashcardProgressRepository;
    private LessonRepository lessonRepository;
    private LessonService lessonService;
    @PersistenceContext private EntityManager em;

    public FlashcardSetServiceImpl(
            FlashcardSetRepository flashcardSetRepository,
            FlashcardRepository flashcardRepository,
            UserRepository userRepository,
            UserFlashcardProgressRepository userFlashcardProgressRepository,
            LessonRepository lessonRepository,
            LessonService lessonService) {
        this.flashcardSetRepository = flashcardSetRepository;
        this.flashcardRepository = flashcardRepository;
        this.userRepository = userRepository;
        this.userFlashcardProgressRepository = userFlashcardProgressRepository;
        this.lessonRepository = lessonRepository;
        this.lessonService = lessonService;
    }

    @Override
    public Map<String, List<FlashcardSetDTO>> groupBySkillAndLevel(List<FlashcardSetDTO> values) {
        LinkedHashMap<String, List<FlashcardSetDTO>> branches = new LinkedHashMap<>();
        for (String skill : lessonService.getSkills()) {
            List<FlashcardSetDTO> grouped =
                    values.stream()
                            .filter(set -> !set.isPersonal())
                            .filter(set -> skill.equals(set.getLessonSkill()))
                            .sorted(Comparator.comparing(streamValue -> streamValue.getLevel(), Comparator.nullsLast((leftValue, rightValue) -> leftValue.compareTo(rightValue))))
                            .toList();
            if (!grouped.isEmpty()) branches.put(skill, grouped);
        }
        return branches;
    }

    @Transactional(readOnly = true)
    public FlashcardSetDTO studySet(Integer id, String email) {
        FlashcardSet set = requireForStudent(id, email);
        FlashcardSetDTO result = FlashcardSetDTO.convertEntityToDto(set);
        result.setLessonId(set.getLesson() == null ? null : set.getLesson().getId());
        result.setLessonTitle(set.getLesson() == null ? null : set.getLesson().getTitle());
        result.setLessonSkill(set.getLesson() == null ? null : set.getLesson().getSkill());
        return result;
    }

    public List<FlashcardSetDTO> list(boolean admin) {
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

    public List<FlashcardSetDTO> listForStudent(String email) {
        User u = learner(email);
        ArrayList<FlashcardSet> all = new ArrayList<FlashcardSet>();
        all.addAll(
                flashcardSetRepository.findByStatusOrderByIdDesc("PUBLISHED").stream()
                        .filter(streamValue -> publishedLessonSet(streamValue))
                        .toList());
        all.addAll(
                flashcardSetRepository.findByOwnerIdOrderByIdDesc(u.getId()).stream()
                        .filter(s -> !"ARCHIVED".equals(s.getStatus()))
                        .toList());
        return view(all);
    }

    @Override
    public List<FlashcardSetDTO> filteredListForStudent(
            String email,
            String source,
            String query,
            String skill,
            String level,
            String sort) {
        if (!List.of("all", "mine", "lesson").contains(source)
                || query == null
                || query.length() > 255
                || (!skill.isBlank() && !lessonService.getSkills().contains(skill))
                || (!level.isBlank() && !lessonService.getLevels().contains(level))
                || !List.of("learning", "newest").contains(sort)) {
            throw new BusinessRuleException("Bộ lọc bộ thẻ không hợp lệ");
        }
        String normalizedQuery = fold(query);
        Comparator<FlashcardSetDTO> order =
                "newest".equals(sort)
                        ? Comparator.comparing(
                                        (FlashcardSetDTO streamValue) -> streamValue.getId())
                                .reversed()
                        : Comparator.comparingInt(
                                        (FlashcardSetDTO set) ->
                                                set.isPersonal()
                                                        ? lessonService.getSkills().size()
                                                        : lessonService.getSkills().indexOf(set.getLessonSkill()))
                                .thenComparingInt(
                                        set -> lessonService.getLevels().indexOf(set.getLevel()))
                                .thenComparing(set -> fold(set.getTitle()))
                                .thenComparing((FlashcardSetDTO streamValue) -> streamValue.getId());
        return listForStudent(email).stream()
                .filter(
                        set ->
                                "all".equals(source)
                                        || ("mine".equals(source)
                                                ? set.isPersonal()
                                                : !set.isPersonal() && set.getLessonId() != null))
                .filter(set -> skill.isBlank() || skill.equals(set.getLessonSkill()))
                .filter(set -> level.isBlank() || level.equals(set.getLevel()))
                .filter(
                        set ->
                                normalizedQuery.isBlank()
                                        || fold(
                                                        set.getTitle()
                                                                + " "
                                                                + Objects.toString(set.getDescription(), "")
                                                                + " "
                                                                + Objects.toString(set.getLessonTitle(), ""))
                                                .contains(normalizedQuery))
                .sorted(order)
                .toList();
    }

    private String fold(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd');
    }

    private List<FlashcardSetDTO> view(List<FlashcardSet> values) {
        return values.stream()
                .map(
                        s -> {
                            FlashcardSetDTO result = FlashcardSetDTO.convertEntityToDto(s);
                            result.setCount(
                                    flashcardRepository
                                            .findBySetIdAndStatusOrderByDisplayOrderAsc(s.getId(), "ACTIVE")
                                            .size());
                            result.setPersonal(s.getOwner() != null);
                            result.setLessonId(s.getLesson() == null ? null : s.getLesson().getId());
                            result.setLessonTitle(s.getLesson() == null ? null : s.getLesson().getTitle());
                            result.setLessonSkill(s.getLesson() == null ? null : s.getLesson().getSkill());
                            return result;
                        })
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Integer, FlashcardSetDTO> lessonSets() {
        LinkedHashMap<Integer, FlashcardSetDTO> result = new LinkedHashMap<>();
        for (FlashcardSetDTO set :
                view(
                        flashcardSetRepository.findByStatusOrderByIdDesc("PUBLISHED").stream()
                                .filter(
                                        s ->
                                                s.getOwner() == null
                                                        && s.getLesson() != null
                                                        && "PUBLISHED"
                                                                .equals(s.getLesson().getStatus()))
                                .toList())) result.put(set.getLessonId(), set);
        return result;
    }

    public FlashcardSet require(Integer id, boolean admin) {
        return flashcardSetRepository
                .findById(id)
                .filter(s -> s.getOwner() == null && (admin || "PUBLISHED".equals(s.getStatus())))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ thẻ"));
    }

    @Override
    @Transactional(readOnly = true)
    public FlashcardSetDTO requireView(Integer id, boolean admin) {
        return FlashcardSetDTO.convertEntityToDto(require(id, admin));
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

    @Override
    @Transactional(readOnly = true)
    public FlashcardSetDTO requireForStudentView(Integer id, String email) {
        return FlashcardSetDTO.convertEntityToDto(requireForStudent(id, email));
    }

    private boolean publishedLessonSet(FlashcardSet set) {
        return set.getOwner() == null
                && set.getLesson() != null
                && "PUBLISHED".equals(set.getStatus())
                && "PUBLISHED".equals(set.getLesson().getStatus());
    }

    public FlashcardSetDTO form(Integer id) {
        return toForm(require(id, true));
    }

    private FlashcardSetDTO toForm(FlashcardSet s) {
        FlashcardSetDTO f = FlashcardSetDTO.convertEntityToDto(s);
        for (Flashcard c :
                flashcardRepository.findBySetIdAndStatusOrderByDisplayOrderAsc(
                        s.getId(), "ACTIVE")) {
            FlashcardDTO card = FlashcardDTO.convertEntityToDto(c);
            f.getFlashcards().add(card);
        }
        return f;
    }

    private Integer save(Integer id, FlashcardSetDTO f, Lesson lesson, User owner) {
        if (!lessonService.getLevels().contains(f.getCefrLevel())
                || !lessonService.getStatuses().contains(f.getStatus()))
            throw new BusinessRuleException("Cấp độ hoặc trạng thái không hợp lệ");
        if (f.getFlashcards() == null || f.getFlashcards().isEmpty() || f.getFlashcards().size() > 200)
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
        for (FlashcardDTO card : f.getFlashcards()) {
            if (card.getFrontContent() == null
                    || card.getFrontContent().isBlank()
                    || card.getBackContent() == null
                    || card.getBackContent().isBlank()
                    || card.getFrontContent().length() > 200
                    || card.getBackContent().length() > 1000)
                throw new BusinessRuleException(
                        "Mỗi thẻ cần thuật ngữ và định nghĩa. Tối đa 200 ký tự cho thuật ngữ và"
                                + " 1000 cho định nghĩa");
            if (card.getId() != null && (!byId.containsKey(card.getId()) || !seen.add(card.getId())))
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
        set = FlashcardSetDTO.convertDtoToEntity(f);
        set.setId(id);
        set.setTitle(f.getTitle().trim());
        set.setLesson(lesson);
        set.setOwner(owner);
        set.setVisibility(owner == null ? "PUBLIC" : "PRIVATE");
        flashcardSetRepository.save(set);
        int position = existing.stream().mapToInt(streamValue -> streamValue.getDisplayOrder()).max().orElse(0) + 1;
        for (Flashcard c : existing) {
            c.setStatus("ARCHIVED");
            flashcardRepository.save(c);
        }
        flashcardRepository.flush();
        for (FlashcardDTO card : f.getFlashcards()) {
            Flashcard c = FlashcardDTO.convertDtoToEntity(card);
            c.setSet(set);
            c.setFrontContent(card.getFrontContent().trim());
            c.setBackContent(card.getBackContent().trim());
            c.setCefrLevel(f.getCefrLevel());
            c.setDisplayOrder(position++);
            c.setStatus("ACTIVE");
            flashcardRepository.save(c);
        }
        return set.getId();
    }

    public Integer saveAdmin(Integer id, FlashcardSetDTO f) {
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

    public FlashcardSetDTO personalForm(Integer id, String email) {
        FlashcardSet set = requireForStudent(id, email);
        if (set.getOwner() == null)
            throw new BusinessRuleException("Không thể sửa bộ thẻ thư viện");
        return toForm(set);
    }

    public Integer savePersonal(Integer id, FlashcardSetDTO f, String email) {
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

    public List<FlashcardDTO> study(Integer id, String email) {
        User u = learner(email);
        requireForStudent(id, email);
        return flashcardRepository.findBySetIdAndStatusOrderByDisplayOrderAsc(id, "ACTIVE").stream()
                .map(
                        c -> {
                            UserFlashcardProgress p =
                                    userFlashcardProgressRepository
                                            .findByUserIdAndFlashcardId(u.getId(), c.getId())
                                            .orElse(null);
                            FlashcardDTO result = FlashcardDTO.convertEntityToDto(c);
                            result.setState(p == null ? "LEARNING" : p.getStatus());
                            result.setNextReviewAt(p == null ? null : p.getNextReviewAt());
                            return result;
                        })
                .sorted(
                        Comparator.comparing(
                                        streamValue -> streamValue.getNextReviewAt(),
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
    }

    public FlashcardSet publishedForLesson(Integer lessonId) {
        return flashcardSetRepository
                .findFirstByLessonIdAndOwnerIsNullAndStatusOrderByIdDesc(lessonId, "PUBLISHED")
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public FlashcardSetDTO publishedForLessonView(Integer lessonId) {
        return FlashcardSetDTO.convertEntityToDto(publishedForLesson(lessonId));
    }

    public FlashcardSet forLesson(Integer lessonId) {
        return flashcardSetRepository
                .findFirstByLessonIdAndOwnerIsNullOrderByIdDesc(lessonId)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public FlashcardSetDTO forLessonView(Integer lessonId) {
        return FlashcardSetDTO.convertEntityToDto(forLesson(lessonId));
    }

    public List<Lesson> availableLessons() {
        return lessonRepository.findAllByOrderBySkillAscCefrLevelAscDisplayOrderAsc().stream()
                .filter(l -> !"ARCHIVED".equals(l.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.aptisprep.mvc.dto.LessonDTO> availableLessonViews() {
        return availableLessons().stream()
                .map(com.aptisprep.mvc.dto.LessonDTO::convertEntityToDto)
                .toList();
    }

    public void prepareForLesson(FlashcardSetDTO form, Integer lessonId) {
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

}







