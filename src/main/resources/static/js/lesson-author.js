(() => {
 const skill=document.getElementById('skill');
 const profiles={
  Reading:['Dạy chiến lược đọc, tìm bằng chứng và nhận diện bẫy đáp án.','Đoạn đọc và câu hỏi mẫu','Phân tích từ khóa, liên kết câu và bằng chứng','Đoạn đọc mới và yêu cầu áp dụng'],
  Listening:['Dạy nghe theo mục tiêu; video hướng dẫn và bài nghe cần được phân biệt rõ.','Tình huống nghe hoặc đoạn lời thoại minh họa','Phân tích thông tin cần nghe, từ đồng nghĩa và điểm gây nhiễu','Video nghe và nhiệm vụ trước / trong / sau khi nghe'],
  Writing:['Dạy phân tích đề, lập ý và viết từng phần theo đúng dạng bài.','Đề viết và dàn ý minh họa','Phân tích từng câu / đoạn và giọng văn','Đề viết để học viên tự thực hành'],
  Speaking:['Dạy lập ý và phát triển câu trả lời; không khẳng định chấm phát âm khi chưa có chấm audio.','Câu hỏi nói và khung triển khai ý','Phân tích câu trả lời, lý do và ví dụ','Chủ đề nói và hướng dẫn tự luyện'],
  'Grammar & Vocabulary':['Dạy cách dùng từ / cấu trúc trong ngữ cảnh và các lỗi thường gặp.','Câu mẫu và ngữ cảnh sử dụng','Phân tích cấu trúc, nghĩa và cách dùng','Bài tập vận dụng cấu trúc / từ vựng']
 };
 function update(){const p=profiles[skill.value];if(!p)return;document.getElementById('skill-guidance').textContent=p[0];for(const [id,text]of [['exampleText',p[1]],['analysis',p[2]],['practicePrompt',p[3]]]){const el=document.getElementById(id);el.placeholder=text;}const audio=document.getElementById('listening-audio'),writing=document.getElementById('writing-settings');if(audio)audio.hidden=skill.value!=='Listening';if(writing)writing.hidden=skill.value!=='Writing';}
 skill.addEventListener('change',update);update();

 const list=document.getElementById('question-list'),add=document.getElementById('add-question'),template=document.getElementById('question-template'),empty=document.getElementById('no-question');
 function reindex(){const rows=[...list.querySelectorAll('.lesson-question-editor')];rows.forEach((row,index)=>{row.dataset.questionIndex=index;row.querySelector('legend').textContent=`Câu ${index+1}`;row.querySelectorAll('[name]').forEach(el=>el.name=el.name.replace(/checks\[\d+\]/,`checks[${index}]`));row.querySelectorAll('[id]').forEach(el=>el.id=el.id.replace(/checks\d+/,`checks${index}`));});empty.hidden=rows.length>0;add.disabled=rows.length>=20;}
 function wire(row){row.querySelector('.remove-question').addEventListener('click',()=>{row.remove();reindex();});}
 list.querySelectorAll('.lesson-question-editor').forEach(wire);
 add.addEventListener('click',()=>{if(list.children.length>=20)return;const row=template.content.firstElementChild.cloneNode(true),index=list.children.length;row.querySelectorAll('[data-field]').forEach(el=>{const field=el.dataset.field;el.name=`checks[${index}].${field}`;el.id=`checks${index}.${field}`;});list.appendChild(row);wire(row);reindex();row.querySelector('textarea').focus();});
 reindex();
})();
