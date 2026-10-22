package com.aptisprep.service.learning;
import com.aptisprep.entity.*;
import com.aptisprep.repository.*;
import com.aptisprep.dto.form.FlashcardSetForm;
import com.aptisprep.enums.*;
import com.aptisprep.exception.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional
public class FlashcardSetService {
 private final FlashcardSetRepository sets; private final FlashcardRepository cards; private final UserRepository users; private final UserFlashcardProgressRepository progress; private final LessonRepository lessons;
 @PersistenceContext private EntityManager em;
 public FlashcardSetService(FlashcardSetRepository s,FlashcardRepository c,UserRepository u,UserFlashcardProgressRepository p,LessonRepository l){sets=s;cards=c;users=u;progress=p;lessons=l;}
 public record SetView(Integer id,String title,String description,String level,String status,int count,boolean personal,Integer lessonId,String lessonTitle,String lessonSkill){}
 public record LevelSets(String level,List<SetView> sets){}
 public record SkillSets(String skill,int count,List<LevelSets> levels){}
 public static List<SkillSets> groupBySkillAndLevel(List<SetView> values){
  var branches=new ArrayList<SkillSets>();
  for(var skill:LessonService.SKILLS){
   var levels=new ArrayList<LevelSets>();
   for(var level:LessonService.LEVELS){
    var grouped=values.stream().filter(s->!s.personal()&&skill.equals(s.lessonSkill())&&level.equals(s.level())).toList();
    if(!grouped.isEmpty())levels.add(new LevelSets(level,grouped));
   }
   if(!levels.isEmpty())branches.add(new SkillSets(skill,levels.stream().mapToInt(l->l.sets().size()).sum(),levels));
  }
  return List.copyOf(branches);
 }
 public record CardView(Integer id,String term,String definition,String state){}
 public record LessonLink(Integer id,String title,String skill){}
 public record StudySetView(Integer id,String title,String description,String cefrLevel,LessonLink lesson){}
 @Transactional(readOnly=true)
 public StudySetView studySet(Integer id,String email){
  var set=requireForStudent(id,email);var lesson=set.getLesson();
  var link=lesson==null?null:new LessonLink(lesson.getId(),lesson.getTitle(),lesson.getSkill());
  return new StudySetView(set.getId(),set.getTitle(),set.getDescription(),set.getCefrLevel(),link);
 }
 public List<SetView> list(boolean admin){return view((admin?sets.findByOwnerIsNullOrderByIdDesc():sets.findByStatusOrderByIdDesc("PUBLISHED")).stream().filter(s->s.getOwner()==null&&!"ARCHIVED".equals(s.getStatus())).toList());}
 public List<SetView> listForStudent(String email){var u=learner(email);var all=new ArrayList<FlashcardSet>();all.addAll(sets.findByStatusOrderByIdDesc("PUBLISHED").stream().filter(this::publishedLessonSet).toList());all.addAll(sets.findByOwnerIdOrderByIdDesc(u.getId()).stream().filter(s->!"ARCHIVED".equals(s.getStatus())).toList());return view(all);}
 private List<SetView> view(List<FlashcardSet> values){return values.stream().map(s->new SetView(s.getId(),s.getTitle(),s.getDescription(),s.getCefrLevel(),s.getStatus(),cards.findBySetIdAndStatusOrderByDisplayOrderAsc(s.getId(),"ACTIVE").size(),s.getOwner()!=null,s.getLesson()==null?null:s.getLesson().getId(),s.getLesson()==null?null:s.getLesson().getTitle(),s.getLesson()==null?null:s.getLesson().getSkill())).toList();}
 @Transactional(readOnly=true)
 public Map<Integer,SetView> lessonSets(){var result=new LinkedHashMap<Integer,SetView>();for(var set:view(sets.findByStatusOrderByIdDesc("PUBLISHED").stream().filter(s->s.getOwner()==null&&s.getLesson()!=null&&"PUBLISHED".equals(s.getLesson().getStatus())).toList()))result.put(set.lessonId(),set);return result;}
 public FlashcardSet require(Integer id,boolean admin){return sets.findById(id).filter(s->s.getOwner()==null&&(admin||"PUBLISHED".equals(s.getStatus()))).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bộ thẻ"));}
 public FlashcardSet requireForStudent(Integer id,String email){var u=learner(email);return sets.findById(id).filter(s->publishedLessonSet(s)||(s.getOwner()!=null&&s.getOwner().getId().equals(u.getId())&&!"ARCHIVED".equals(s.getStatus()))).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bộ thẻ"));}
 private boolean publishedLessonSet(FlashcardSet set){return set.getOwner()==null&&set.getLesson()!=null&&"PUBLISHED".equals(set.getStatus())&&"PUBLISHED".equals(set.getLesson().getStatus());}
 public FlashcardSetForm form(Integer id){return toForm(require(id,true));}
 private FlashcardSetForm toForm(FlashcardSet s){var f=new FlashcardSetForm();f.setTitle(s.getTitle());f.setDescription(s.getDescription());f.setCefrLevel(s.getCefrLevel());f.setStatus(s.getStatus());f.setLessonId(s.getLesson()==null?null:s.getLesson().getId());for(var c:cards.findBySetIdAndStatusOrderByDisplayOrderAsc(s.getId(),"ACTIVE")){var r=new FlashcardSetForm.Row();r.setId(c.getId());r.setTerm(c.getFrontContent());r.setDefinition(c.getBackContent());f.getRows().add(r);}return f;}
 private Integer save(Integer id,FlashcardSetForm f,Lesson lesson,User owner){
  if(!LessonService.LEVELS.contains(f.getCefrLevel())||!LessonService.STATUSES.contains(f.getStatus()))throw new BusinessRuleException("Cấp độ hoặc trạng thái không hợp lệ");
  if(f.getRows()==null||f.getRows().isEmpty()||f.getRows().size()>200)throw new BusinessRuleException("Bộ thẻ cần từ 1 đến 200 cặp từ và nghĩa");
  var set=id==null?new FlashcardSet():sets.findById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bộ thẻ"));if(id!=null)em.lock(set,LockModeType.PESSIMISTIC_WRITE);
  var existing=id==null?List.<Flashcard>of():cards.findBySetIdAndStatusOrderByDisplayOrderAsc(id,"ACTIVE");var byId=new HashMap<Integer,Flashcard>();existing.forEach(c->byId.put(c.getId(),c));var seen=new HashSet<Integer>();
  for(var row:f.getRows()){if(row.getTerm()==null||row.getTerm().isBlank()||row.getDefinition()==null||row.getDefinition().isBlank()||row.getTerm().length()>200||row.getDefinition().length()>1000)throw new BusinessRuleException("Mỗi thẻ cần thuật ngữ và định nghĩa. Tối đa 200 ký tự cho thuật ngữ và 1000 cho định nghĩa");if(row.getId()!=null&&(!byId.containsKey(row.getId())||!seen.add(row.getId())))throw new BusinessRuleException("Thẻ không thuộc bộ này hoặc bị lặp");}
  if(lesson!=null){var linked=sets.findFirstByLessonIdAndOwnerIsNullOrderByIdDesc(lesson.getId()).orElse(null);if(linked!=null&&!Objects.equals(linked.getId(),id))throw new BusinessRuleException("Bài học này đã có một bộ thẻ");}
  set.setTitle(f.getTitle().trim());set.setDescription(f.getDescription());set.setCefrLevel(f.getCefrLevel());set.setStatus(f.getStatus());set.setLesson(lesson);set.setOwner(owner);set.setVisibility(owner==null?"PUBLIC":"PRIVATE");sets.save(set);
  int position=existing.stream().mapToInt(Flashcard::getDisplayOrder).max().orElse(0)+1;
  for(var c:existing){c.setStatus("ARCHIVED");cards.save(c);}cards.flush();
  for(var row:f.getRows()){var c=row.getId()==null?new Flashcard():byId.get(row.getId());c.setSet(set);c.setFrontContent(row.getTerm().trim());c.setBackContent(row.getDefinition().trim());c.setCefrLevel(f.getCefrLevel());c.setDisplayOrder(position++);c.setStatus("ACTIVE");cards.save(c);}return set.getId();
 }
 public Integer saveAdmin(Integer id,FlashcardSetForm f){if(id!=null)require(id,true);if(f.getLessonId()==null)throw new BusinessRuleException("Hãy chọn bài học sử dụng bộ thẻ này");var lesson=lessons.findById(f.getLessonId()).filter(l->!"ARCHIVED".equals(l.getStatus())).orElseThrow(()->new BusinessRuleException("Bài học không còn khả dụng"));if(!lesson.getCefrLevel().equals(f.getCefrLevel()))throw new BusinessRuleException("Cấp độ bộ thẻ phải trùng với cấp độ bài học");return save(id,f,lesson,null);}
 private User learner(String email){var u=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản"));if(u.getStatus()!=UserStatus.ACTIVE||u.getRole()!=Role.STUDENT)throw new BusinessRuleException("Tài khoản không được phép học");return u;}
 public FlashcardSetForm personalForm(Integer id,String email){var set=requireForStudent(id,email);if(set.getOwner()==null)throw new BusinessRuleException("Không thể sửa bộ thẻ thư viện");return toForm(set);}
 public Integer savePersonal(Integer id,FlashcardSetForm f,String email){var u=learner(email);if(id!=null&&requireForStudent(id,email).getOwner()==null)throw new BusinessRuleException("Không thể sửa bộ thẻ thư viện");f.setStatus("PUBLISHED");f.setLessonId(null);return save(id,f,null,u);}
 public void archivePersonal(Integer id,String email){var u=learner(email);var set=sets.findById(id).filter(s->s.getOwner()!=null&&s.getOwner().getId().equals(u.getId())&&!"ARCHIVED".equals(s.getStatus())).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bộ thẻ cá nhân"));set.setStatus("ARCHIVED");sets.save(set);}
 public List<CardView> study(Integer id,String email){var u=learner(email);requireForStudent(id,email);return cards.findBySetIdAndStatusOrderByDisplayOrderAsc(id,"ACTIVE").stream().map(c->new CardView(c.getId(),c.getFrontContent(),c.getBackContent(),progress.findByUserIdAndFlashcardId(u.getId(),c.getId()).map(UserFlashcardProgress::getStatus).orElse("LEARNING"))).toList();}
 public Optional<FlashcardSet> publishedForLesson(Integer lessonId){return sets.findFirstByLessonIdAndOwnerIsNullAndStatusOrderByIdDesc(lessonId,"PUBLISHED");}
 public Optional<FlashcardSet> forLesson(Integer lessonId){return sets.findFirstByLessonIdAndOwnerIsNullOrderByIdDesc(lessonId);}
 public List<Lesson> availableLessons(){return lessons.findAllByOrderBySkillAscCefrLevelAscDisplayOrderAsc().stream().filter(l->!"ARCHIVED".equals(l.getStatus())).toList();}
 public void prepareForLesson(FlashcardSetForm form,Integer lessonId){if(lessonId==null)return;var lesson=lessons.findById(lessonId).filter(l->!"ARCHIVED".equals(l.getStatus())).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bài học"));form.setLessonId(lesson.getId());form.setCefrLevel(lesson.getCefrLevel());}
 public void mark(Integer setId,Integer cardId,String state,String email){if(!List.of("LEARNING","MASTERED").contains(state))throw new BusinessRuleException("Trạng thái không hợp lệ");var u=learner(email);em.lock(u,LockModeType.PESSIMISTIC_WRITE);requireForStudent(setId,email);var c=cards.findById(cardId).filter(x->x.getSet()!=null&&x.getSet().getId().equals(setId)&&"ACTIVE".equals(x.getStatus())).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy thẻ"));var p=progress.findByUserIdAndFlashcardId(u.getId(),cardId).orElseGet(()->{var n=new UserFlashcardProgress();n.setUser(u);n.setFlashcard(c);n.setReviewCount(0);return n;});if(!state.equals(p.getStatus()))p.setReviewCount(p.getReviewCount()+1);p.setLastReviewedAt(LocalDateTime.now());p.setStatus(state);p.setMasteryLevel("MASTERED".equals(state)?5:1);progress.save(p);}
}
