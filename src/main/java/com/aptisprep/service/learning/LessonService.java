package com.aptisprep.service.learning;

import com.aptisprep.dto.form.LessonForm;
import com.aptisprep.entity.*;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.exception.*;
import com.aptisprep.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.aptisprep.service.storage.StorageService;

@Service
public class LessonService {
    public static final List<String> SKILLS = List.of("Grammar & Vocabulary", "Reading", "Listening", "Writing", "Speaking");
    public static final List<String> LEVELS = List.of("A1", "A2", "B1", "B2", "C1");
    public static final List<String> STATUSES = List.of("DRAFT", "PUBLISHED", "ARCHIVED");
    private final LessonRepository lessons;
    private final LessonProgressRepository progress;
    private final UserRepository users; private final LearningPathService learningPaths; private final LessonTeachingService teaching;
    private final StorageService storage;
    public LessonService(LessonRepository l, LessonProgressRepository p,
                         UserRepository u,LearningPathService lp,LessonTeachingService t,StorageService s) {
        lessons=l; progress=p; users=u; learningPaths=lp; teaching=t; storage=s;
    }
    public List<Lesson> adminLessons(){ return lessons.findAllByOrderBySkillAscCefrLevelAscDisplayOrderAsc(); }
    public record LevelBranch(String level, List<Lesson> lessons) {
        public Map<String,List<Lesson>> topics(){
            var result=new LinkedHashMap<String,List<Lesson>>();
            for(var lesson:lessons){String topic=lesson.getMaterial()==null?null:lesson.getMaterial().getTopic();if(topic==null||topic.isBlank())topic="Bài học độc lập";result.computeIfAbsent(topic,key->new ArrayList<>()).add(lesson);}
            return result;
        }
    }
    public record SkillBranch(String skill, List<LevelBranch> levels, int count) {}
    public List<SkillBranch> adminTree() {
        var all = adminLessons();
        return SKILLS.stream().map(skill -> new SkillBranch(skill, LEVELS.stream()
                .map(level -> new LevelBranch(level, all.stream()
                        .filter(l -> skill.equals(l.getSkill()) && level.equals(l.getCefrLevel())).toList()))
                .toList(), (int) all.stream().filter(l -> skill.equals(l.getSkill())).count())).toList();
    }
    public LessonForm newForm(String skill, String level) {
        if (!SKILLS.contains(skill) || !LEVELS.contains(level)) throw new BusinessRuleException("Kỹ năng hoặc cấp độ không hợp lệ");
        int max = adminLessons().stream().filter(l -> skill.equals(l.getSkill()) && level.equals(l.getCefrLevel()))
                .mapToInt(Lesson::getDisplayOrder).max().orElse(0);
        if (max == Integer.MAX_VALUE) throw new BusinessRuleException("Không thể gợi ý thêm thứ tự cho nhánh này");
        var form = new LessonForm(); form.setSkill(skill); form.setCefrLevel(level);
        form.setDisplayOrder(max + 1); form.setStatus("DRAFT"); form.setTranscriptVisibility("AFTER_LISTEN");form.setSampleVisibility("AFTER_SUBMIT");return form;
    }
    public List<Lesson> publishedLessons(){ return lessons.findByStatusOrderBySkillAscCefrLevelAscDisplayOrderAsc("PUBLISHED"); }
    public Lesson require(Integer id){ return lessons.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học")); }
    public Lesson requirePublished(Integer id){ var l=require(id); if(!"PUBLISHED".equals(l.getStatus())) throw new ResourceNotFoundException("Bài học chưa được công bố"); return l; }
    public List<LessonTeachingService.CheckView> questions(Integer lessonId){return teaching.questions(lessonId);}
    public LessonTeachingService.LessonStats stats(Integer lessonId){return teaching.stats(lessonId);}
    public LessonForm form(Integer id){ var l=require(id); var f=new LessonForm(); if(l.getMaterial()!=null)org.springframework.beans.BeanUtils.copyProperties(l.getMaterial(),f); f.setChecks(new java.util.ArrayList<>(teaching.editorQuestions(id))); f.setTitle(l.getTitle()); f.setDescription(l.getDescription()); f.setSkill(l.getSkill()); f.setCefrLevel(l.getCefrLevel()); f.setDisplayOrder(l.getDisplayOrder()); f.setStatus(l.getStatus());f.setTranscript(l.getTranscript());f.setMinWordCount(l.getMinWordCount());f.setMaxWordCount(l.getMaxWordCount());if(f.getTranscriptVisibility()==null)f.setTranscriptVisibility("AFTER_LISTEN");if(f.getSampleVisibility()==null)f.setSampleVisibility("AFTER_SUBMIT");return f; }
    @Transactional public Lesson save(Integer id, LessonForm f){
        return save(id,f,null,false);
    }
    @Transactional public Lesson save(Integer id, LessonForm f, MultipartFile audio, boolean removeAudio){
        return save(id,f,audio,removeAudio,null,false);
    }
    @Transactional public Lesson save(Integer id, LessonForm f, MultipartFile audio, boolean removeAudio, MultipartFile image, boolean removeImage){
        return save(id,f,audio,removeAudio,image,removeImage,null,false);
    }
    @Transactional public Lesson save(Integer id, LessonForm f, MultipartFile audio, boolean removeAudio, MultipartFile image, boolean removeImage, MultipartFile video, boolean removeVideo){
        if(video!=null&&!video.isEmpty())validateVideo(video);
        if("PUBLISHED".equals(f.getStatus())&&"Listening".equals(f.getSkill())){
            boolean available=audio!=null&&!audio.isEmpty()||id!=null&&!removeAudio&&has(require(id).getAudioPath());
            boolean transcriptAfterListen=has(f.getTranscript())&&(!has(f.getTranscriptVisibility())||"AFTER_LISTEN".equals(f.getTranscriptVisibility()));
            boolean sampleAfterListen=has(f.getSampleResponse())&&"AFTER_LISTEN".equals(f.getSampleVisibility());
            if(!available&&(transcriptAfterListen||sampleAfterListen))throw new BusinessRuleException("Cần tải audio để mở transcript / bài mẫu sau khi nghe, hoặc chọn thời điểm mở khác");
        }
        validate(f, id); teaching.validate(f); var l=id==null?new Lesson():require(id); l.setTitle(f.getTitle().trim()); l.setDescription(trim(f.getDescription())); l.setSkill(f.getSkill()); l.setCefrLevel(f.getCefrLevel()); l.setDisplayOrder(f.getDisplayOrder()); l.setStatus(f.getStatus()); l.setMaterial(f.copy());l.setTranscript(trim(f.getTranscript()));l.setMinWordCount(f.getMinWordCount());l.setMaxWordCount(f.getMaxWordCount()); l=lessons.save(l); teaching.saveChecks(l.getId(),f.getChecks());
        if(removeAudio&&l.getAudioPath()!=null){teaching.resetAudioEngagement(l.getId());l.setAudioPath(null);l.setAudioName(null);}
        if(audio!=null&&!audio.isEmpty()){validateAudio(f.getSkill(),audio);teaching.resetAudioEngagement(l.getId());String path=storage.store(audio,"lesson-audio/"+l.getId());l.setAudioPath(path);l.setAudioName(cleanName(audio.getOriginalFilename()));}
        if(removeImage&&l.getImagePath()!=null){l.setImagePath(null);l.setImageName(null);}
        if(image!=null&&!image.isEmpty()){validateImage(image);String path=storage.store(image,"lesson-images/"+l.getId());l.setImagePath(path);l.setImageName(cleanName(image.getOriginalFilename()));}
        if(removeVideo)l.setVideoPath(null);
        if(video!=null&&!video.isEmpty())l.setVideoPath(storage.store(video,"lesson-video/"+l.getId()));
        return lessons.save(l);
    }
    @Transactional public void archive(Integer id){ var l=require(id); l.setStatus("ARCHIVED"); lessons.save(l); }
    @Transactional public Lesson duplicate(Integer id){var source=require(id);var f=form(id);f.setTitle(source.getTitle()+" — Bản sao");f.setStatus("DRAFT");f.setDisplayOrder(newForm(source.getSkill(),source.getCefrLevel()).getDisplayOrder());return save(null,f);}
    @Transactional public void move(Integer id,int direction){if(direction!= -1&&direction!=1)throw new BusinessRuleException("Hướng di chuyển không hợp lệ");var current=require(id);var branch=adminLessons().stream().filter(x->x.getSkill().equals(current.getSkill())&&x.getCefrLevel().equals(current.getCefrLevel())).toList();int index=java.util.stream.IntStream.range(0,branch.size()).filter(i->branch.get(i).getId().equals(id)).findFirst().orElseThrow();int target=index+direction;if(target<0||target>=branch.size())return;var other=branch.get(target);int a=current.getDisplayOrder(),b=other.getDisplayOrder(),temp=branch.stream().mapToInt(Lesson::getDisplayOrder).max().orElse(0)+1000;current.setDisplayOrder(temp);lessons.saveAndFlush(current);other.setDisplayOrder(a);lessons.saveAndFlush(other);current.setDisplayOrder(b);lessons.save(current);}
    @Transactional public LessonProgress recordOpen(String email, Integer lessonId){ var u=active(email); var l=requirePublished(lessonId); var p=progress.findByUserIdAndLessonId(u.getId(), l.getId()).orElseGet(() -> {var x=new LessonProgress();x.setUser(u);x.setLesson(l);x.setStatus("IN_PROGRESS");return x;}); p.setLastAccessedAt(LocalDateTime.now()); return progress.save(p); }
    @Transactional public LessonProgress complete(String email, Integer lessonId){ var u=active(email);var lesson=requirePublished(lessonId);var state=teaching.completion(lessonId,email);if(!state.eligible())throw new BusinessRuleException(state.message(lesson.getSkill())); learningPaths.completeLesson(u.getId(),lessonId); return progress.findByUserIdAndLessonId(u.getId(),lessonId).orElseThrow(); }
    public Optional<LessonProgress> progress(String email,Integer lessonId){var u=active(email);return progress.findByUserIdAndLessonId(u.getId(),lessonId);}
    @Transactional(readOnly = true)
    public Map<Integer, String> lessonStatuses(String email, Collection<Integer> lessonIds) {
        var user = active(email);
        if (lessonIds.isEmpty()) return Map.of();
        var statuses = new HashMap<Integer, String>();
        for (var value : progress.findByUserIdAndLessonIdIn(user.getId(), lessonIds)) {
            boolean completed = "COMPLETED".equals(value.getStatus()) && value.getCompletedAt() != null;
            statuses.put(value.getLesson().getId(), completed ? "COMPLETED" : "IN_PROGRESS");
        }
        return Map.copyOf(statuses);
    }
    private User active(String email){var u=users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));if(u.getStatus()!=UserStatus.ACTIVE)throw new BusinessRuleException("Tài khoản không hoạt động");return u;}
    public record Neighbors(Lesson previous,Lesson next){}
    public Neighbors neighbors(Lesson lesson){var all=publishedLessons().stream().filter(x->x.getSkill().equals(lesson.getSkill())&&x.getCefrLevel().equals(lesson.getCefrLevel())).toList();int i=java.util.stream.IntStream.range(0,all.size()).filter(x->all.get(x).getId().equals(lesson.getId())).findFirst().orElse(-1);return new Neighbors(i>0?all.get(i-1):null,i>=0&&i+1<all.size()?all.get(i+1):null);}
    private void validate(LessonForm f,Integer id){if(!SKILLS.contains(f.getSkill())||!LEVELS.contains(f.getCefrLevel())||!STATUSES.contains(f.getStatus()))throw new BusinessRuleException("Kỹ năng, CEFR hoặc trạng thái không hợp lệ");if(f.getMinWordCount()!=null&&f.getMaxWordCount()!=null&&f.getMinWordCount()>f.getMaxWordCount())throw new BusinessRuleException("Số từ tối thiểu không được lớn hơn số từ tối đa");if(lessons.existsBySkillAndCefrLevelAndDisplayOrderAndIdNot(f.getSkill(),f.getCefrLevel(),f.getDisplayOrder(),id==null?-1:id))throw new BusinessRuleException("Thứ tự bài học đã tồn tại trong kỹ năng và cấp độ này");}
    private static void validateAudio(String skill,MultipartFile file){String name=cleanName(file.getOriginalFilename()).toLowerCase(Locale.ROOT);String type=file.getContentType()==null?"":file.getContentType().toLowerCase(Locale.ROOT);if(!"Listening".equals(skill))throw new BusinessRuleException("Tệp âm thanh chỉ dùng cho bài Listening");if(file.getSize()>20L*1024*1024)throw new BusinessRuleException("Tệp âm thanh tối đa 20 MB");if(!(name.endsWith(".mp3")||name.endsWith(".wav")||name.endsWith(".m4a")||name.endsWith(".ogg")||name.endsWith(".webm"))||!type.startsWith("audio/"))throw new BusinessRuleException("Chỉ chấp nhận tệp âm thanh MP3, WAV, M4A, OGG hoặc WEBM");}
    private static void validateImage(MultipartFile file){String name=cleanName(file.getOriginalFilename()).toLowerCase(Locale.ROOT);String type=file.getContentType()==null?"":file.getContentType().toLowerCase(Locale.ROOT);if(file.getSize()>8L*1024*1024)throw new BusinessRuleException("Hình ảnh tối đa 8 MB");if(!(name.endsWith(".jpg")||name.endsWith(".jpeg")||name.endsWith(".png")||name.endsWith(".webp")||name.endsWith(".gif"))||!type.startsWith("image/"))throw new BusinessRuleException("Chỉ chấp nhận hình JPG, PNG, WEBP hoặc GIF");}
    private static void validateVideo(MultipartFile file){
        String name=cleanName(file.getOriginalFilename()).toLowerCase(Locale.ROOT),type=Objects.toString(file.getContentType(),"");
        if(file.getSize()>50L*1024*1024||!(name.endsWith(".mp4")&&type.equals("video/mp4")||name.endsWith(".webm")&&type.equals("video/webm")))throw new BusinessRuleException("Video chỉ nhận MP4 hoặc WEBM, tối đa 50 MB");
        try(var input=file.getInputStream()){
            byte[] head=input.readNBytes(12);
            boolean mp4=head.length>=12&&head[4]=='f'&&head[5]=='t'&&head[6]=='y'&&head[7]=='p';
            boolean webm=head.length>=4&&(head[0]&255)==0x1a&&(head[1]&255)==0x45&&(head[2]&255)==0xdf&&(head[3]&255)==0xa3;
            if(name.endsWith(".mp4")?!mp4:!webm)throw new BusinessRuleException("Nội dung tệp video không đúng định dạng");
        }catch(java.io.IOException ex){throw new BusinessRuleException("Không đọc được video tải lên");}
    }
    private static String cleanName(String value){if(value==null||value.isBlank())return "audio";String normalized=value.replace('\\','/');String name=normalized.substring(normalized.lastIndexOf('/')+1);return name.length()>255?name.substring(name.length()-255):name;}
    private static boolean has(String s){return s!=null&&!s.isBlank();} private static String trim(String s){return s==null?null:s.trim();}
}
