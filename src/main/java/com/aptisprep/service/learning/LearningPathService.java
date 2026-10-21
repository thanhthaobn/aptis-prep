package com.aptisprep.service.learning;

import com.aptisprep.entity.*;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.enums.GoalStatus;
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
    private final LearningPathRepository paths; private final LearningPathItemRepository items; private final UserGoalRepository goals;
    private final LessonRepository lessons; private final LessonProgressRepository progress; private final UserRepository users;
    private final ObjectProvider<ExamAttemptContract> attempts; private final ExamCatalogContract catalog; private final AccountAccessContract accounts;
    public LearningPathService(LearningPathRepository p,LearningPathItemRepository i,LessonRepository l,LessonProgressRepository lp,UserRepository u,UserGoalRepository g,ObjectProvider<ExamAttemptContract> a,ExamCatalogContract c,AccountAccessContract ac){paths=p;items=i;lessons=l;progress=lp;users=u;goals=g;attempts=a;catalog=c;accounts=ac;}
    @Override @Transactional public void ensurePaths(Integer userId){
        syncPaths(active(userId),false);
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
        return syncPaths(user,true);
    }
    @Override @Transactional(readOnly=true) public PathProgress summary(Integer userId,Integer pathId){active(userId);var path=paths.findById(pathId).filter(x->x.getUser().getId().equals(userId)).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy lộ trình"));return summaryOf(path);}
    @Override @Transactional public void completeLesson(Integer userId,Integer lessonId){
        var user=active(userId);var lesson=lessons.findById(lessonId).filter(x->"PUBLISHED".equals(x.getStatus())).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bài học"));var p=progress.findByUserIdAndLessonId(userId,lessonId).orElseGet(()->{var x=new LessonProgress();x.setUser(user);x.setLesson(lesson);return x;});if(!"COMPLETED".equals(p.getStatus())){p.setStatus("COMPLETED");p.setCompletedAt(LocalDateTime.now());}p.setLastAccessedAt(LocalDateTime.now());progress.save(p);for(var item:items.findByLearningPathUserIdAndLearningPathStatusAndLessonId(userId,"ACTIVE",lessonId)){if(!"COMPLETED".equals(item.getStatus())){item.setStatus("COMPLETED");item.setCompletedAt(p.getCompletedAt());items.save(item);}}
    }
    @Override @Transactional public Integer startCheckpoint(Integer userId,Integer pathId,String requestKey){active(userId);var path=paths.findById(pathId).filter(x->x.getUser().getId().equals(userId)&&"ACTIVE".equals(x.getStatus())).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy lộ trình"));var required=items.findByLearningPathIdOrderByPositionAsc(pathId).stream().filter(LearningPathItem::isRequired).toList();if(required.isEmpty()||required.stream().anyMatch(x->!"COMPLETED".equals(x.getStatus())))throw new BusinessRuleException("Hãy hoàn thành các bài bắt buộc trước khi làm checkpoint");if(path.getCheckpointExamId()==null){String level=path.getTargetLevel();if(level==null||level.isBlank())level=required.get(required.size()-1).getLesson().getCefrLevel();path.setTargetLevel(level);path.setCheckpointExamId(catalog.checkpoint(path.getSkill(),level).examId());paths.save(path);}var engine=attempts.getIfAvailable();if(engine==null)throw new BusinessRuleException("Hệ thống làm đề đang được ghép nối; vui lòng thử lại sau");return engine.start(userId,path.getCheckpointExamId(),requestKey).attemptId();}
    @Override @Transactional public void applyCheckpoint(Integer userId,Integer attemptId){
        active(userId);
        var engine=attempts.getIfAvailable();
        if(engine==null)throw new BusinessRuleException("Chưa có dịch vụ kết quả bài thi");
        AttemptResult result=engine.result(userId,attemptId);
        if(result==null||!"COMPLETED".equalsIgnoreCase(result.state())||!"CHECKPOINT".equalsIgnoreCase(result.examType()))throw new BusinessRuleException("Checkpoint chưa hoàn tất");
        if(paths.findByUserIdAndSourceAttemptId(userId,attemptId).isPresent())return;
        var path=paths.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(userId,"ACTIVE").stream().filter(x->Objects.equals(x.getCheckpointExamId(),result.examId())).findFirst().orElseThrow(()->new ResourceNotFoundException("Kết quả không thuộc lộ trình hiện tại"));
        entityManager.lock(path,LockModeType.PESSIMISTIC_WRITE);
        if(Objects.equals(path.getSourceAttemptId(),attemptId))return;
        accounts.applyAssessment(userId,attemptId);
        boolean passed=result.skills()!=null&&result.skills().stream().anyMatch(x->x.passed()&&normalizeSkill(path.getSkill()).equals(normalizeSkill(x.skill())));
        path.setSourceAttemptId(attemptId);
        if(passed&&path.getCompletedAt()==null){
            path.setCompletedAt(LocalDateTime.now());
            openNextStage(path, result);
        }
        paths.save(path);
    }
    private void openNextStage(LearningPath current,AttemptResult result){
        String achieved=result.skills().stream().filter(x->x.passed()&&normalizeSkill(current.getSkill()).equals(normalizeSkill(x.skill()))).map(SkillResult::estimatedLevel).filter(Objects::nonNull).findFirst().orElse(current.getTargetLevel());
        String next=nextLevel(achieved);
        if(next==null)return;
        var selected=lessons.findByStatusOrderBySkillAscCefrLevelAscDisplayOrderAsc("PUBLISHED").stream().filter(lesson->current.getSkill().equals(lesson.getSkill())&&next.equalsIgnoreCase(lesson.getCefrLevel())).toList();
        if(selected.isEmpty())return;
        current.setStatus("COMPLETED");
        paths.saveAndFlush(current);
        var replacement=new LearningPath();replacement.setUser(current.getUser());replacement.setSkill(current.getSkill());replacement.setPathSource("CHECKPOINT");replacement.setFromLevel(achieved);replacement.setTargetLevel(next);replacement.setStatus("ACTIVE");replacement=paths.save(replacement);
        int position=1;
        for(var lesson:selected){var item=new LearningPathItem();item.setLearningPath(replacement);item.setLesson(lesson);item.setPosition(position++);item.setRequired(true);var done=progress.findByUserIdAndLessonId(current.getUser().getId(),lesson.getId()).filter(x->"COMPLETED".equals(x.getStatus())).orElse(null);item.setStatus(done==null?"NOT_STARTED":"COMPLETED");item.setCompletedAt(done==null?null:done.getCompletedAt());items.save(item);}
    }
    private record DesiredPath(String skill,String source,Integer goalSkillId,String targetLevel,List<Lesson> lessons){}
    private int syncPaths(User user,boolean refreshContent){
        var published=lessons.findByStatusOrderBySkillAscCefrLevelAscDisplayOrderAsc("PUBLISHED");
        var activeGoal=goals.findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(),GoalStatus.ACTIVE).orElse(null);
        Map<String,UserGoalSkill> targets=new HashMap<>();
        if(activeGoal!=null)for(var target:activeGoal.getSkills())targets.put(normalizeSkill(target.getSkill()),target);
        int changed=0;
        for(String skill:LessonService.SKILLS){
            var target=targets.get(normalizeSkill(skill));
            var selected=published.stream().filter(l->skill.equals(l.getSkill())).filter(l->target==null||levelRank(l.getCefrLevel())<=levelRank(target.getTargetLevel())).toList();
            var current=paths.findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(user.getId(),skill,"ACTIVE").orElse(null);
            if(selected.isEmpty()){
                if(current!=null&&items.findByLearningPathIdOrderByPositionAsc(current.getId()).isEmpty()){current.setStatus("ARCHIVED");paths.save(current);changed++;}
                continue;
            }
            var desired=new DesiredPath(skill,target==null?"ORIENTATION":"GOAL",target==null?null:target.getId(),target==null?null:target.getTargetLevel(),selected);
            var currentItems=current==null?List.<LearningPathItem>of():items.findByLearningPathIdOrderByPositionAsc(current.getId());
            boolean sameMetadata=current!=null&&Objects.equals(current.getPathSource(),desired.source())&&Objects.equals(current.getGoalSkillId(),desired.goalSkillId())&&Objects.equals(current.getTargetLevel(),desired.targetLevel());
            boolean sameLessons=currentItems.stream().map(i->i.getLesson().getId()).toList().equals(selected.stream().map(Lesson::getId).toList());
            if(current!=null&&sameMetadata&&(!refreshContent||sameLessons))continue;
            if(current!=null){current.setStatus("ARCHIVED");paths.saveAndFlush(current);}
            createPath(user,desired);changed++;
        }
        return changed;
    }
    private void createPath(User user,DesiredPath desired){
        var path=new LearningPath();path.setUser(user);path.setSkill(desired.skill());path.setPathSource(desired.source());path.setGoalSkillId(desired.goalSkillId());path.setTargetLevel(desired.targetLevel());path.setStatus("ACTIVE");path=paths.save(path);
        int position=1;
        for(var lesson:desired.lessons()){
            var item=new LearningPathItem();item.setLearningPath(path);item.setLesson(lesson);item.setPosition(position++);item.setRequired(true);
            var done=progress.findByUserIdAndLessonId(user.getId(),lesson.getId()).filter(x->"COMPLETED".equals(x.getStatus())).orElse(null);
            item.setStatus(done==null?"NOT_STARTED":"COMPLETED");item.setCompletedAt(done==null?null:done.getCompletedAt());items.save(item);
        }
    }
    private static int levelRank(String value){int rank=LessonService.LEVELS.indexOf(value==null?"":value.toUpperCase(Locale.ROOT));return rank<0?Integer.MAX_VALUE:rank;}
    private PathProgress summaryOf(LearningPath path){var list=items.findByLearningPathIdOrderByPositionAsc(path.getId());int total=list.size();int completed=(int)list.stream().filter(x->"COMPLETED".equals(x.getStatus())).count();int required=(int)list.stream().filter(LearningPathItem::isRequired).count();int completedRequired=(int)list.stream().filter(LearningPathItem::isRequired).filter(x->"COMPLETED".equals(x.getStatus())).count();var next=list.stream().filter(x->!"COMPLETED".equals(x.getStatus())).findFirst().orElse(null);String checkpoint=path.getCompletedAt()!=null?"passed":required==0?"not_configured":completedRequired==required?"available":"locked";return new PathProgress(path.getId(),path.getSkill(),path.getPathSource(),path.getStatus(),completed,total,LearningProgressCalculator.percent(completed,total),completedRequired,required,next==null?null:next.getLesson().getId(),next==null?null:next.getLesson().getTitle(),checkpoint);}
    private User active(Integer id){var u=users.findById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản"));if(u.getStatus()!=UserStatus.ACTIVE)throw new BusinessRuleException("Tài khoản không hoạt động");return u;}
    private User active(String email){var u=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản"));if(u.getStatus()!=UserStatus.ACTIVE)throw new BusinessRuleException("Tài khoản không hoạt động");return u;}
    private static String normalizeSkill(String value){return value==null?"":value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+","_");}
    private static String nextLevel(String value){return switch(value==null?"":value.trim().toUpperCase(Locale.ROOT)){case "A1"->"A2";case "A2"->"B1";case "B1"->"B2";case "B2"->"C1";case "C1"->"C2";default->null;};}
}
