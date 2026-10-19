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
 private final FlashcardSetRepository sets; private final FlashcardRepository cards; private final UserRepository users; private final UserFlashcardProgressRepository progress;
 @PersistenceContext private EntityManager em;
 public FlashcardSetService(FlashcardSetRepository s,FlashcardRepository c,UserRepository u,UserFlashcardProgressRepository p){sets=s;cards=c;users=u;progress=p;}
 public record SetView(Integer id,String title,String description,String level,String status,int count,boolean personal){}
 public record CardView(Integer id,String term,String definition,String state){}
 public List<SetView> list(boolean admin){return view((admin?sets.findByOwnerIsNullOrderByIdDesc():sets.findByStatusOrderByIdDesc("PUBLISHED")).stream().filter(s->s.getOwner()==null&&!"ARCHIVED".equals(s.getStatus())).toList());}
 public List<SetView> listForStudent(String email){var u=learner(email);var all=new ArrayList<FlashcardSet>();all.addAll(sets.findByStatusOrderByIdDesc("PUBLISHED").stream().filter(s->s.getOwner()==null).toList());all.addAll(sets.findByOwnerIdOrderByIdDesc(u.getId()).stream().filter(s->!"ARCHIVED".equals(s.getStatus())).toList());return view(all);}
 private List<SetView> view(List<FlashcardSet> values){return values.stream().map(s->new SetView(s.getId(),s.getTitle(),s.getDescription(),s.getCefrLevel(),s.getStatus(),cards.findBySetIdAndStatusOrderByDisplayOrderAsc(s.getId(),"ACTIVE").size(),s.getOwner()!=null)).toList();}
 public FlashcardSet require(Integer id,boolean admin){return sets.findById(id).filter(s->s.getOwner()==null&&(admin||"PUBLISHED".equals(s.getStatus()))).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bộ thẻ"));}
 public FlashcardSet requireForStudent(Integer id,String email){var u=learner(email);return sets.findById(id).filter(s->(s.getOwner()==null&&"PUBLISHED".equals(s.getStatus()))||(s.getOwner()!=null&&s.getOwner().getId().equals(u.getId())&&!"ARCHIVED".equals(s.getStatus()))).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bộ thẻ"));}
 public FlashcardSetForm form(Integer id){return toForm(require(id,true));}
 private FlashcardSetForm toForm(FlashcardSet s){var f=new FlashcardSetForm();f.setTitle(s.getTitle());f.setDescription(s.getDescription());f.setCefrLevel(s.getCefrLevel());f.setStatus(s.getStatus());for(var c:cards.findBySetIdAndStatusOrderByDisplayOrderAsc(s.getId(),"ACTIVE")){var r=new FlashcardSetForm.Row();r.setId(c.getId());r.setTerm(c.getFrontContent());r.setDefinition(c.getBackContent());f.getRows().add(r);}return f;}
 public Integer save(Integer id,FlashcardSetForm f){
  if(!LessonService.LEVELS.contains(f.getCefrLevel())||!LessonService.STATUSES.contains(f.getStatus()))throw new BusinessRuleException("Cấp độ hoặc trạng thái không hợp lệ");
  if(f.getRows()==null||f.getRows().isEmpty()||f.getRows().size()>200)throw new BusinessRuleException("Bộ thẻ cần từ 1 đến 200 cặp từ và nghĩa");
  var set=id==null?new FlashcardSet():sets.findById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bộ thẻ"));if(id!=null)em.lock(set,LockModeType.PESSIMISTIC_WRITE);
  var existing=id==null?List.<Flashcard>of():cards.findBySetIdAndStatusOrderByDisplayOrderAsc(id,"ACTIVE");var byId=new HashMap<Integer,Flashcard>();existing.forEach(c->byId.put(c.getId(),c));var seen=new HashSet<Integer>();
  for(var row:f.getRows()){if(row.getTerm()==null||row.getTerm().isBlank()||row.getDefinition()==null||row.getDefinition().isBlank()||row.getTerm().length()>200||row.getDefinition().length()>1000)throw new BusinessRuleException("Mỗi thẻ cần thuật ngữ và định nghĩa; tối đa 200 ký tự cho thuật ngữ, 1000 cho định nghĩa");if(row.getId()!=null&&(!byId.containsKey(row.getId())||!seen.add(row.getId())))throw new BusinessRuleException("Thẻ không thuộc bộ này hoặc bị lặp");}
  set.setTitle(f.getTitle().trim());set.setDescription(f.getDescription());set.setCefrLevel(f.getCefrLevel());set.setStatus(f.getStatus());sets.save(set);
  int position=existing.stream().mapToInt(Flashcard::getDisplayOrder).max().orElse(0)+1;
  for(var c:existing){c.setStatus("ARCHIVED");cards.save(c);}cards.flush();
  for(var row:f.getRows()){var c=row.getId()==null?new Flashcard():byId.get(row.getId());c.setSet(set);c.setLesson(null);c.setFrontContent(row.getTerm().trim());c.setBackContent(row.getDefinition().trim());c.setCefrLevel(f.getCefrLevel());c.setDisplayOrder(position++);c.setStatus("ACTIVE");cards.save(c);}return set.getId();
 }
 public Integer saveAdmin(Integer id,FlashcardSetForm f){if(id!=null)require(id,true);return save(id,f);}
 private User learner(String email){var u=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản"));if(u.getStatus()!=UserStatus.ACTIVE||u.getRole()!=Role.STUDENT)throw new BusinessRuleException("Tài khoản không được phép học");return u;}
 public FlashcardSetForm personalForm(Integer id,String email){var set=requireForStudent(id,email);if(set.getOwner()==null)throw new BusinessRuleException("Không thể sửa bộ thẻ thư viện");return toForm(set);}
 public Integer savePersonal(Integer id,FlashcardSetForm f,String email){var u=learner(email);if(id!=null&&requireForStudent(id,email).getOwner()==null)throw new BusinessRuleException("Không thể sửa bộ thẻ thư viện");f.setStatus("PUBLISHED");Integer result=save(id,f);var set=sets.findById(result).orElseThrow();set.setOwner(u);set.setVisibility("PRIVATE");sets.save(set);return result;}
 public void archivePersonal(Integer id,String email){var u=learner(email);var set=sets.findById(id).filter(s->s.getOwner()!=null&&s.getOwner().getId().equals(u.getId())&&!"ARCHIVED".equals(s.getStatus())).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bộ thẻ cá nhân"));set.setStatus("ARCHIVED");sets.save(set);}
 public List<CardView> study(Integer id,String email){var u=learner(email);requireForStudent(id,email);return cards.findBySetIdAndStatusOrderByDisplayOrderAsc(id,"ACTIVE").stream().map(c->new CardView(c.getId(),c.getFrontContent(),c.getBackContent(),progress.findByUserIdAndFlashcardId(u.getId(),c.getId()).map(UserFlashcardProgress::getStatus).orElse("LEARNING"))).toList();}
 public void mark(Integer setId,Integer cardId,String state,String email){if(!List.of("LEARNING","MASTERED").contains(state))throw new BusinessRuleException("Trạng thái không hợp lệ");var u=learner(email);em.lock(u,LockModeType.PESSIMISTIC_WRITE);requireForStudent(setId,email);var c=cards.findById(cardId).filter(x->x.getSet()!=null&&x.getSet().getId().equals(setId)&&"ACTIVE".equals(x.getStatus())).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy thẻ"));var p=progress.findByUserIdAndFlashcardId(u.getId(),cardId).orElseGet(()->{var n=new UserFlashcardProgress();n.setUser(u);n.setFlashcard(c);n.setReviewCount(0);return n;});if(!state.equals(p.getStatus())){p.setReviewCount(p.getReviewCount()+1);p.setLastReviewedAt(LocalDateTime.now());}p.setStatus(state);p.setMasteryLevel("MASTERED".equals(state)?5:1);progress.save(p);}
}
