package com.aptisprep.service.learning;

import com.aptisprep.dto.form.LessonForm;
import com.aptisprep.entity.*;
import com.aptisprep.enums.*;
import com.aptisprep.exception.*;
import com.aptisprep.repository.*;
import java.net.URI;
import jakarta.persistence.*;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.aptisprep.service.storage.StorageService;
import com.aptisprep.service.contract.AiAssessmentContract;
import java.math.BigDecimal;

@Service @Transactional
public class LessonTeachingService {
 private final LessonCheckRepository checks; private final LessonRepository lessons; private final UserRepository users; private final LessonResponseRepository responses; private final LessonCheckAttemptRepository attempts; private final LessonEngagementRepository engagements; private final StorageService storage; private final LessonProgressRepository progress; private final AiAssessmentContract assessments;
 @PersistenceContext private EntityManager em;
 public LessonTeachingService(LessonCheckRepository c,LessonRepository l,UserRepository u,LessonResponseRepository r,LessonCheckAttemptRepository a,LessonEngagementRepository e,StorageService s,LessonProgressRepository p,AiAssessmentContract ai){checks=c;lessons=l;users=u;responses=r;attempts=a;engagements=e;storage=s;progress=p;assessments=ai;}
 public Optional<LessonResponse> savedWork(Integer lessonId,String email){var u=user(email);return responses.findByUserIdAndLessonId(u.getId(),lessonId);}
 public void saveRecording(Integer lessonId,MultipartFile file,String email){var u=user(email);var lesson=lessons.findById(lessonId).filter(l->"PUBLISHED".equals(l.getStatus())&&"Speaking".equals(l.getSkill())).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bài Speaking"));if(file==null||file.isEmpty()||file.getSize()>20L*1024*1024||file.getContentType()==null||!file.getContentType().startsWith("audio/"))throw new BusinessRuleException("Bản ghi âm không hợp lệ hoặc lớn hơn 20 MB");var r=responses.findByUserIdAndLessonId(u.getId(),lessonId).orElseGet(LessonResponse::new);String old=r.getAudioPath();String path=storage.store(file,"lesson-recordings/"+u.getId()+"/"+lesson.getId());r.setUserId(u.getId());r.setLessonId(lessonId);r.setAudioPath(path);r.setAudioName("Bản ghi luyện nói");r.setResponseVersion((r.getResponseVersion()==null?0:r.getResponseVersion())+1);r.setAssessmentKey(null);r.setUpdatedAt(java.time.LocalDateTime.now());responses.save(r);if(old!=null)storage.delete(old);}
 public record AttemptView(int score,int total,java.time.LocalDateTime submittedAt){}
 public record CompletionState(boolean hasQuestions,boolean checkSubmitted,boolean needsResponse,boolean responseSaved,boolean needsAudio,boolean audioPlayed,boolean eligible){public String message(){if(!checkSubmitted&&hasQuestions)return "Hãy hoàn thành kiểm tra kiến thức";if(needsResponse&&!responseSaved)return "Hãy lưu bài thực hành";if(needsAudio&&!audioPlayed)return "Hãy phát bài nghe";return "Đã đủ điều kiện hoàn thành";}}
 public List<AttemptView> history(Integer lessonId,String email){var u=user(email);return attempts.findByUserIdAndLessonIdOrderBySubmittedAtDesc(u.getId(),lessonId).stream().map(a->new AttemptView(a.getScore(),a.getTotal(),a.getSubmittedAt())).toList();}
 public CompletionState completion(Integer lessonId,String email){var u=user(email);var l=lessons.findById(lessonId).orElseThrow();boolean h=checks.countByLessonId(lessonId)>0,c=attempts.existsByUserIdAndLessonId(u.getId(),lessonId),nr=List.of("Writing","Speaking").contains(l.getSkill())&&has(l.getMaterial().getPracticePrompt()),rs=responses.findByUserIdAndLessonId(u.getId(),lessonId).map(x->has(x.getText())||has(x.getAudioPath())).orElse(false),na="Listening".equals(l.getSkill())&&has(l.getAudioPath()),ap=engagements.findByUserIdAndLessonId(u.getId(),lessonId).map(x->x.getAudioPlayedAt()!=null).orElse(false);return new CompletionState(h,c,nr,rs,na,ap,(!h||c)&&(!nr||rs)&&(!na||ap));}
 public void audioPlayed(Integer lessonId,String email){var u=user(email);lessons.findById(lessonId).filter(l->"PUBLISHED".equals(l.getStatus())&&has(l.getAudioPath())).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bài nghe"));var e=engagements.findByUserIdAndLessonId(u.getId(),lessonId).orElseGet(LessonEngagement::new);e.setUserId(u.getId());e.setLessonId(lessonId);e.setAudioPlayedAt(java.time.LocalDateTime.now());engagements.save(e);}
 public void section(Integer lessonId,String section,String email){if(!Set.of("overview","media","method","example","practice").contains(section))throw new BusinessRuleException("Phần bài học không hợp lệ");var u=user(email);lessons.findById(lessonId).filter(l->"PUBLISHED".equals(l.getStatus())).orElseThrow();var e=engagements.findByUserIdAndLessonId(u.getId(),lessonId).orElseGet(LessonEngagement::new);e.setUserId(u.getId());e.setLessonId(lessonId);e.setLastSection(section);engagements.save(e);}
 public String lastSection(Integer lessonId,String email){var u=user(email);return engagements.findByUserIdAndLessonId(u.getId(),lessonId).map(LessonEngagement::getLastSection).orElse(null);}
 public record LessonStats(long started,long completed,long audioListeners,long attempts,double averagePercent){}
 public LessonStats stats(Integer lessonId){var all=attempts.findByLessonId(lessonId);double avg=all.stream().mapToDouble(a->a.getTotal()==0?0:(a.getScore()*100.0/a.getTotal())).average().orElse(0);return new LessonStats(progress.countByLessonId(lessonId),progress.countByLessonIdAndStatus(lessonId,"COMPLETED"),engagements.countByLessonIdAndAudioPlayedAtIsNotNull(lessonId),all.size(),avg);}
 public String response(Integer lessonId,String email){var u=users.findByEmailIgnoreCase(email).orElseThrow();return responses.findByUserIdAndLessonId(u.getId(),lessonId).map(LessonResponse::getText).orElse("");}
 public void saveResponse(Integer lessonId,String text,String email){
  var u=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản"));em.lock(u,LockModeType.PESSIMISTIC_WRITE);em.refresh(u);
  if(u.getStatus()!=UserStatus.ACTIVE||u.getRole()!=Role.STUDENT)throw new BusinessRuleException("Tài khoản không được phép học");
  lessons.findById(lessonId).filter(l->"PUBLISHED".equals(l.getStatus())).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bài học"));
  if(text==null||text.length()>10000)throw new BusinessRuleException("Bài thực hành tối đa 10000 ký tự");
  var r=responses.findByUserIdAndLessonId(u.getId(),lessonId).orElseGet(LessonResponse::new);if(!java.util.Objects.equals(r.getText(),text)){r.setResponseVersion((r.getResponseVersion()==null?0:r.getResponseVersion())+1);r.setAssessmentKey(null);}r.setUserId(u.getId());r.setLessonId(lessonId);r.setText(text);r.setUpdatedAt(java.time.LocalDateTime.now());responses.save(r);
 }
 @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
 public String assess(Integer lessonId,String email){var u=user(email);if(u.getStatus()!=UserStatus.ACTIVE||u.getRole()!=Role.STUDENT)throw new BusinessRuleException("Tài khoản không được phép học");var lesson=lessons.findById(lessonId).filter(l->"PUBLISHED".equals(l.getStatus())&&List.of("Writing","Speaking").contains(l.getSkill())).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bài tự luận"));var response=responses.findByUserIdAndLessonId(u.getId(),lessonId).orElseThrow(()->new BusinessRuleException("Hãy lưu bài làm trước khi yêu cầu chấm"));if(!has(response.getText()))throw new BusinessRuleException("Writing cần bài viết; Speaking cần transcript trước khi chấm");int version=response.getResponseVersion()==null?1:response.getResponseVersion();String key="lesson-"+lessonId+"-user-"+u.getId()+"-v"+version;String prompt=has(lesson.getMaterial().getPracticePrompt())?lesson.getMaterial().getPracticePrompt():lesson.getTitle();var request=new AiAssessmentContract.AssessmentRequest(key,u.getId(),response.getId(),response.getId(),version,lesson.getSkill().toUpperCase(Locale.ROOT),lesson.getSkill().toUpperCase(Locale.ROOT),prompt,response.getText(),"lesson-v1","learning-feedback-v1",BigDecimal.valueOf(50),"Speaking".equals(lesson.getSkill())?response.getText():null);assessments.request(request);response.setAssessmentKey(key);responses.save(response);assessments.process(key);return key;}
 public record CheckView(Integer id,String prompt,List<String> options){}
 public record AnswerFeedback(String prompt,String selected,String correct,boolean right,String explanation){}
 public record Feedback(int score,int total,List<AnswerFeedback> answers){}
 public List<CheckView> questions(Integer id){return checks.findByLessonIdOrderByPositionAsc(id).stream().map(q->new CheckView(q.getId(),q.getPrompt(),options(q))).toList();}
 public List<LessonForm.CheckForm> editorQuestions(Integer id){return checks.findByLessonIdOrderByPositionAsc(id).stream().map(q->{var f=new LessonForm.CheckForm();f.setPrompt(q.getPrompt());f.setOptionA(q.getOptionA());f.setOptionB(q.getOptionB());f.setOptionC(q.getOptionC());f.setOptionD(q.getOptionD());f.setOptionE(q.getOptionE());f.setOptionF(q.getOptionF());f.setCorrectOption(q.getCorrectOption());f.setExplanation(q.getExplanation());return f;}).toList();}
 public void validate(LessonForm f){
  videoId(f.getVideoUrl());safeLink(f.getSourceUrl());
  if(f.getChecks()==null||f.getChecks().size()>20)throw new BusinessRuleException("Mỗi bài tối đa 20 câu kiểm tra kiến thức");
  for(var q:f.getChecks()){if(empty(q))continue;var opts=options(q);if(!has(q.getPrompt())||opts.size()<2||!has(q.getExplanation())||q.getCorrectOption()==null||q.getCorrectOption()<0||q.getCorrectOption()>=opts.size())throw new BusinessRuleException("Mỗi câu kiểm tra cần câu hỏi, từ 2 đến 6 lựa chọn liên tiếp, đáp án đúng và giải thích");}
  if("PUBLISHED".equals(f.getStatus())&&has(f.getObjectives())&&(!has(f.getStrategy())||!has(f.getExampleText())||!has(f.getAnalysis())||!has(f.getPracticePrompt())))throw new BusinessRuleException("Bài giảng cần cách làm, ví dụ, phân tích và yêu cầu thực hành trước khi công bố");
 }
 public void saveChecks(Integer id,List<LessonForm.CheckForm> forms){checks.deleteByLessonId(id);checks.flush();int position=1;for(var f:forms){if(empty(f))continue;var q=new LessonCheck();q.setLessonId(id);q.setPosition(position++);q.setPrompt(f.getPrompt());q.setOptionA(f.getOptionA());q.setOptionB(f.getOptionB());q.setOptionC(blankToNull(f.getOptionC()));q.setOptionD(blankToNull(f.getOptionD()));q.setOptionE(blankToNull(f.getOptionE()));q.setOptionF(blankToNull(f.getOptionF()));q.setCorrectOption(f.getCorrectOption());q.setExplanation(f.getExplanation());checks.save(q);}}
 public Feedback grade(Integer id,Map<String,String> answers,String email){
  var u=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản"));if(u.getStatus()!=UserStatus.ACTIVE||u.getRole()!=Role.STUDENT)throw new BusinessRuleException("Tài khoản không được phép học");
  lessons.findById(id).filter(l->"PUBLISHED".equals(l.getStatus())).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy bài học"));
  var all=checks.findByLessonIdOrderByPositionAsc(id);if(all.isEmpty())throw new BusinessRuleException("Bài chưa có câu kiểm tra kiến thức");
  var feedback=new ArrayList<AnswerFeedback>();int score=0;
  for(var q:all){var selected=answers.get("q"+q.getId());var options=options(q);int value;try{value=Integer.parseInt(selected);}catch(Exception ex){throw new BusinessRuleException("Vui lòng trả lời đầy đủ các câu hỏi");}if(value<0||value>=options.size())throw new BusinessRuleException("Lựa chọn không hợp lệ");boolean right=value==q.getCorrectOption();if(right)score++;feedback.add(new AnswerFeedback(q.getPrompt(),options.get(value),options.get(q.getCorrectOption()),right,q.getExplanation()));}
  var attempt=new LessonCheckAttempt();attempt.setUserId(u.getId());attempt.setLessonId(id);attempt.setScore(score);attempt.setTotal(all.size());attempt.setAnswersJson(answers.entrySet().stream().filter(e->e.getKey().matches("q\\d+")&&e.getValue().matches("[0-5]")).sorted(Map.Entry.comparingByKey()).map(e->e.getKey()+"="+e.getValue()).collect(java.util.stream.Collectors.joining("&")));attempts.save(attempt);return new Feedback(score,all.size(),feedback);
 }
 public static String videoId(String url){if(!has(url))return null;try{var uri=URI.create(url.trim());String host=uri.getHost();if(!"https".equalsIgnoreCase(uri.getScheme())||host==null||uri.getUserInfo()!=null||uri.getPort()!=-1)throw new IllegalArgumentException();String id=null;if(host.equalsIgnoreCase("youtu.be"))id=uri.getPath().substring(1);else if(List.of("www.youtube.com","youtube.com","www.youtube-nocookie.com").contains(host.toLowerCase())){if(uri.getPath().startsWith("/embed/"))id=uri.getPath().substring(7);else if("/watch".equals(uri.getPath())&&uri.getRawQuery()!=null)for(String part:uri.getRawQuery().split("&"))if(part.startsWith("v="))id=part.substring(2);}if(id==null||!id.matches("[A-Za-z0-9_-]{11}"))throw new IllegalArgumentException();return id;}catch(Exception ex){throw new BusinessRuleException("Nhập liên kết HTTPS YouTube hợp lệ cho video hướng dẫn");}}
 private static void safeLink(String value){if(!has(value))return;try{var u=URI.create(value);if(!"https".equalsIgnoreCase(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)throw new IllegalArgumentException();}catch(Exception e){throw new BusinessRuleException("Nguồn tham khảo cần là liên kết HTTPS hợp lệ");}}
 private static boolean has(String s){return s!=null&&!s.isBlank();}
 private static List<String> options(LessonCheck q){return Stream.of(q.getOptionA(),q.getOptionB(),q.getOptionC(),q.getOptionD(),q.getOptionE(),q.getOptionF()).filter(LessonTeachingService::has).toList();}
 private static List<String> options(LessonForm.CheckForm q){return Stream.of(q.getOptionA(),q.getOptionB(),q.getOptionC(),q.getOptionD(),q.getOptionE(),q.getOptionF()).filter(LessonTeachingService::has).toList();}
 private static String blankToNull(String value){return has(value)?value.trim():null;}
 private static boolean empty(LessonForm.CheckForm q){return !has(q.getPrompt())&&options(q).isEmpty()&&!has(q.getExplanation());}
 private User user(String email){return users.findByEmailIgnoreCase(email).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tài khoản"));}
}
