package com.aptisprep.service.learning;

import com.aptisprep.entity.*;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.exception.*;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.LearningPathContract;
import com.aptisprep.service.contract.IntegrationModels.PathProgress;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import com.aptisprep.service.contract.*;
import com.aptisprep.service.contract.IntegrationModels.*;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.LockModeType;

@Service
public class LearningPathService implements LearningPathContract {
    @PersistenceContext private EntityManager entityManager;
    private final LearningPathRepository paths; private final LearningPathItemRepository items;
    private final LessonRepository lessons; private final LessonProgressRepository progress; private final UserRepository users;
    private final ObjectProvider<ExamAttemptContract> attempts; private final ExamCatalogContract catalog; private final AccountAccessContract accounts;
    public LearningPathService(LearningPathRepository p,LearningPathItemRepository i,LessonRepository l,LessonProgressRepository lp,UserRepository u,ObjectProvider<ExamAttemptContract> a,ExamCatalogContract c,AccountAccessContract ac){paths=p;items=i;lessons=l;progress=lp;users=u;attempts=a;catalog=c;accounts=ac;}
    @Override @Transactional public void ensurePaths(Integer userId){
        var user=active(userId); var published=lessons.findByStatusOrderBySkillAscCefrLevelAscDisplayOrderAsc("PUBLISHED");
        for(String skill:LessonService.SKILLS){if(paths.findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(userId,skill,"ACTIVE").isPresent())continue;var path=new LearningPath();path.setUser(user);path.setSkill(skill);path.setPathSource("ORIENTATION");path.setStatus("ACTIVE");path=paths.save(path);int position=1;for(var lesson:published){if(!skill.equals(lesson.getSkill()))continue;var item=new LearningPathItem();item.setLearningPath(path);item.setLesson(lesson);item.setPosition(position++);item.setRequired(true);var done=progress.findByUserIdAndLessonId(userId,lesson.getId()).filter(x->"COMPLETED".equals(x.getStatus())).orElse(null);item.setStatus(done==null?"NOT_STARTED":"COMPLETED");item.setCompletedAt(done==null?null:done.getCompletedAt());items.save(item);}}
    }
    @Override @Transactional(readOnly=true) public List<PathProgress> summaries(Integer userId){
        users.findById(userId).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản"));
        return paths.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(userId,"ACTIVE").stream().map(this::summaryOf).toList();
    }
    @Transactional public List<PathProgress> ensureAndSummaries(String email){var user=active(email);ensurePaths(user.getId());return summaries(user.getId());}
    @Transactional(readOnly=true) public Integer userId(String email){return active(email).getId();}
    /** Refresh only changed orientation paths; preserve old items and all lesson completion records. */
    @Transactional
    public int refreshPaths(String email) {
        var user = active(email);
        entityManager.lock(user, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(user);
        if (user.getStatus() != UserStatus.ACTIVE) throw new BusinessRuleException("Tài khoản không hoạt động");
        var published = lessons.findByStatusOrderBySkillAscCefrLevelAscDisplayOrderAsc("PUBLISHED");
        int changed = 0;
        for (String skill : LessonService.SKILLS) {
            var current = paths.findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(user.getId(), skill, "ACTIVE").orElse(null);
            if (current != null && !"ORIENTATION".equals(current.getPathSource())) continue;
            var selected = published.stream().filter(lesson -> skill.equals(lesson.getSkill())).toList();
            var currentItems = current == null ? List.<LearningPathItem>of() : items.findByLearningPathIdOrderByPositionAsc(current.getId());
            if (current != null && currentItems.stream().map(item -> item.getLesson().getId()).toList()
                    .equals(selected.stream().map(Lesson::getId).toList())) continue;
            if (current != null) {
                current.setStatus("ARCHIVED");
                paths.saveAndFlush(current); // Release the filtered unique index before inserting its replacement.
            }
            var replacement = new LearningPath();
            replacement.setUser(user);
            replacement.setSkill(skill);
            replacement.setPathSource("ORIENTATION");
            replacement.setStatus("ACTIVE");
            replacement = paths.save(replacement);
            int position = 1;
            for (var lesson : selected) {
                var item = new LearningPathItem();
                item.setLearningPath(replacement);
                item.setLesson(lesson);
                item.setPosition(position++);
                item.setRequired(true);
                var completion = progress.findByUserIdAndLessonId(user.getId(), lesson.getId())
                        .filter(record -> "COMPLETED".equals(record.getStatus())).orElse(null);
                item.setStatus(completion == null ? "NOT_STARTED" : "COMPLETED");
                item.setCompletedAt(completion == null ? null : completion.getCompletedAt());
                items.save(item);
            }
            changed++;
        }
        return changed;
    }
    @Override @Transactional(readOnly=true) public PathProgress summary(Integer userId,Integer pathId){active(userId);var path=paths.findById(pathId).filter(x->x.getUser().getId().equals(userId)).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy lộ trình"));return summaryOf(path);}
    @Override @Transactional public void completeLesson(Integer userId,Integer lessonId){
        var user=active(userId);var lesson=lessons.findById(lessonId).filter(x->"PUBLISHED".equals(x.getStatus())).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bài học"));var p=progress.findByUserIdAndLessonId(userId,lessonId).orElseGet(()->{var x=new LessonProgress();x.setUser(user);x.setLesson(lesson);return x;});if(!"COMPLETED".equals(p.getStatus())){p.setStatus("COMPLETED");p.setCompletedAt(LocalDateTime.now());}p.setLastAccessedAt(LocalDateTime.now());progress.save(p);for(var item:items.findByLearningPathUserIdAndLearningPathStatusAndLessonId(userId,"ACTIVE",lessonId)){if(!"COMPLETED".equals(item.getStatus())){item.setStatus("COMPLETED");item.setCompletedAt(p.getCompletedAt());items.save(item);}}
    }
    @Override @Transactional public Integer startCheckpoint(Integer userId,Integer pathId,String requestKey){active(userId);var path=paths.findById(pathId).filter(x->x.getUser().getId().equals(userId)&&"ACTIVE".equals(x.getStatus())).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy lộ trình"));var required=items.findByLearningPathIdOrderByPositionAsc(pathId).stream().filter(LearningPathItem::isRequired).toList();if(required.isEmpty()||required.stream().anyMatch(x->!"COMPLETED".equals(x.getStatus())))throw new BusinessRuleException("Hãy hoàn thành các bài bắt buộc trước khi làm checkpoint");if(path.getCheckpointExamId()==null){String level=path.getTargetLevel();if(level==null||level.isBlank())level=required.get(required.size()-1).getLesson().getCefrLevel();path.setTargetLevel(level);path.setCheckpointExamId(catalog.checkpoint(path.getSkill(),level).examId());paths.save(path);}var engine=attempts.getIfAvailable();if(engine==null)throw new BusinessRuleException("Hệ thống làm đề đang được ghép nối; vui lòng thử lại sau");return engine.start(userId,path.getCheckpointExamId(),requestKey).attemptId();}
    @Override @Transactional public void applyCheckpoint(Integer userId,Integer attemptId){active(userId);var engine=attempts.getIfAvailable();if(engine==null)throw new BusinessRuleException("Chưa có dịch vụ kết quả bài thi");AttemptResult result=engine.result(userId,attemptId);if(result==null||!"COMPLETED".equalsIgnoreCase(result.state())||!"CHECKPOINT".equalsIgnoreCase(result.examType()))throw new BusinessRuleException("Checkpoint chưa hoàn tất");var path=paths.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(userId,"ACTIVE").stream().filter(x->Objects.equals(x.getCheckpointExamId(),result.examId())).findFirst().orElseThrow(()->new ResourceNotFoundException("Kết quả không thuộc lộ trình hiện tại"));accounts.applyAssessment(userId,attemptId);boolean passed=result.skills()!=null&&result.skills().stream().anyMatch(x->x.passed()&&normalizeSkill(path.getSkill()).equals(normalizeSkill(x.skill())));path.setSourceAttemptId(attemptId);path.setCompletedAt(passed?LocalDateTime.now():null);paths.save(path);}
    private PathProgress summaryOf(LearningPath path){var list=items.findByLearningPathIdOrderByPositionAsc(path.getId());int total=list.size();int completed=(int)list.stream().filter(x->"COMPLETED".equals(x.getStatus())).count();int required=(int)list.stream().filter(LearningPathItem::isRequired).count();int completedRequired=(int)list.stream().filter(LearningPathItem::isRequired).filter(x->"COMPLETED".equals(x.getStatus())).count();var next=list.stream().filter(x->!"COMPLETED".equals(x.getStatus())).findFirst().orElse(null);String checkpoint=path.getCompletedAt()!=null?"passed":required==0?"not_configured":completedRequired==required?"available":"locked";return new PathProgress(path.getId(),path.getSkill(),path.getPathSource(),path.getStatus(),completed,total,LearningProgressCalculator.percent(completed,total),completedRequired,required,next==null?null:next.getLesson().getId(),next==null?null:next.getLesson().getTitle(),checkpoint);}
    private User active(Integer id){var u=users.findById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản"));if(u.getStatus()!=UserStatus.ACTIVE)throw new BusinessRuleException("Tài khoản không hoạt động");return u;}
    private User active(String email){var u=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản"));if(u.getStatus()!=UserStatus.ACTIVE)throw new BusinessRuleException("Tài khoản không hoạt động");return u;}
    private static String normalizeSkill(String value){return value==null?"":value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+","_");}
}
