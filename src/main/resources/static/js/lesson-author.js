(() => {
 const skill=document.getElementById('skill');if(!skill)return;
 const profiles={
  Reading:{labels:['Bài đọc chính','Yêu cầu đọc hiểu','Chiến lược đọc (không bắt buộc)','Gợi ý phân tích bài đọc','Tiêu chí tự kiểm tra','Gợi ý đọc hiểu'],tasks:['Đọc hiểu','Đọc tìm thông tin','Nhận diện ý chính'],hint:'Ảnh đặt cạnh bài đọc. Video hướng dẫn ở trước phần đọc; câu hỏi ở ngay sau bài đọc.'},
  Listening:{labels:['Trước khi nghe — bối cảnh (không nhập transcript ở đây)','Nhiệm vụ nghe','Chiến lược nghe (không bắt buộc)','Phân tích tình huống','Tiêu chí nghe hiểu','Gợi ý làm bài nghe'],tasks:['Hội thoại','Thông báo','Phỏng vấn'],hint:'Audio đặt ngay trước câu hỏi nghe. Ảnh chỉ minh họa bối cảnh, không chứa đáp án. Video hướng dẫn không thay thế audio thực hành.'},
  Writing:{labels:['Dàn ý / ví dụ hướng dẫn (không bắt buộc)','Đề viết học viên cần nộp','Cách triển khai bài viết (không bắt buộc)','Phân tích dàn ý','Tiêu chí đánh giá bài viết','Bài viết tham khảo'],tasks:['Viết ngắn','Email','Bài luận'],hint:'Ảnh đề bài đặt trên ô viết. Video hướng dẫn đặt trước phần viết. Bài mẫu mở theo thời điểm đã chọn.'},
  Speaking:{labels:['Tình huống / khung triển khai (không bắt buộc)','Câu hỏi / đề nói học viên cần ghi âm','Cách triển khai câu trả lời (không bắt buộc)','Phân tích cách diễn đạt','Tiêu chí đánh giá bài nói','Gợi ý bài nói'],tasks:['Trả lời câu hỏi','Mô tả ảnh','So sánh trong một ảnh ghép'],hint:'Ảnh mô tả đặt ngay trên bộ ghi âm. Video hướng dẫn đặt trước phần nói. Học viên phải lưu bản ghi âm.'},
  'Grammar & Vocabulary':{labels:['Cấu trúc / từ vựng và ví dụ','Yêu cầu bài tập vận dụng','Cách dùng (không bắt buộc)','Giải thích ví dụ','Tiêu chí tự kiểm tra','Gợi ý vận dụng'],tasks:['Ngữ pháp trong ngữ cảnh','Từ vựng theo chủ đề'],hint:'Ảnh minh họa đặt cạnh ví dụ. Video hướng dẫn ở trước ví dụ; trắc nghiệm ở phần luyện tập.'}
 };
 function update(){
  const p=profiles[skill.value];if(!p)return;
  document.querySelector('.lesson-core-fields').dataset.authorSkill=skill.value;
  document.getElementById('skill-guidance').textContent=p.hint;document.querySelectorAll('.lesson-media-placement').forEach(el=>el.textContent=p.hint);
  ['exampleText','practicePrompt','strategy','analysis','checklist','sampleResponse'].forEach((id,i)=>{document.querySelector('label[for="'+id+'"]').textContent=p.labels[i];document.getElementById(id).placeholder=p.labels[i];});
  document.getElementById('task-suggestions').replaceChildren(...p.tasks.map(t=>new Option(t,t)));
  document.getElementById('listening-audio').hidden=skill.value!=='Listening';document.getElementById('writing-settings').hidden=skill.value!=='Writing';
  const sample=document.getElementById('sampleVisibility'),listenOption=sample.querySelector('[value="AFTER_LISTEN"]');
  if(skill.value==='Listening'&&!listenOption)sample.append(new Option('Sau khi nghe ít nhất 90%','AFTER_LISTEN'));
  if(skill.value!=='Listening'&&listenOption){if(sample.value==='AFTER_LISTEN')sample.value='AFTER_SUBMIT';listenOption.remove();}
  const publishing=document.getElementById('status').value==='PUBLISHED';
  document.getElementById('practicePrompt').required=publishing&&['Writing','Speaking'].includes(skill.value);
 }
 skill.addEventListener('change',update);document.getElementById('status').addEventListener('change',update);update();
 for(const [id,max] of [['audio',20],['image',8],['video',50]]){
  const input=document.getElementById(id);input.addEventListener('change',()=>{input.setCustomValidity(input.files[0]?.size>max*1024*1024?'Tệp vượt quá '+max+' MB.':'');input.reportValidity();});
 }

 const list=document.getElementById('question-list'),add=document.getElementById('add-question'),template=document.getElementById('question-template'),empty=document.getElementById('no-question');
 function reindex(){const rows=[...list.querySelectorAll('.lesson-question-editor')];rows.forEach((row,index)=>{row.dataset.questionIndex=index;row.querySelector('legend').textContent=`Câu ${index+1}`;row.querySelectorAll('[name]').forEach(el=>el.name=el.name.replace(/checks\[\d+\]/,`checks[${index}]`));row.querySelectorAll('[id]').forEach(el=>el.id=el.id.replace(/checks\d+/,`checks${index}`));});empty.hidden=rows.length>0;add.disabled=rows.length>=20;}
 function wire(row){row.querySelector('.remove-question').addEventListener('click',()=>{row.remove();reindex();});}
 list.querySelectorAll('.lesson-question-editor').forEach(wire);
 add.addEventListener('click',()=>{if(list.children.length>=20)return;const row=template.content.firstElementChild.cloneNode(true),index=list.children.length;row.querySelectorAll('[data-field]').forEach(el=>{const field=el.dataset.field;el.name=`checks[${index}].${field}`;el.id=`checks${index}.${field}`;});list.appendChild(row);wire(row);reindex();row.querySelector('textarea').focus();});
 reindex();
})();
