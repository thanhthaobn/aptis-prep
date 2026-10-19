(() => {
 const page=document.querySelector('.lesson-classroom'),form=document.getElementById('lesson-response-form');
 if(!page)return;const id=page.dataset.lessonId;
 const csrf=()=>document.querySelector('input[name="_csrf"]')?.value;
 const post=(url,body='')=>fetch(url,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded','X-CSRF-TOKEN':csrf()||''},body});
 const audio=document.getElementById('lesson-audio');let marked=false;
 if(audio)audio.addEventListener('play',()=>{if(marked)return;marked=true;post(`/student/lessons/${id}/audio-played`).catch(()=>{marked=false;});},{once:false});
 if(form){const area=document.getElementById('response'),status=document.getElementById('response-status');let timer,dirty=false;
  const words=()=>area.value.trim()?area.value.trim().split(/\s+/).length:0;
  const label=()=>{const count=words(),min=Number(area.dataset.minWords||0),max=Number(area.dataset.maxWords||0);status.textContent=`${count} từ${min||max?` · yêu cầu ${min||0}${max?`–${max}`:'+'} từ`:''}${dirty?' · chưa lưu':''}`;};
  area.addEventListener('input',()=>{dirty=true;label();clearTimeout(timer);timer=setTimeout(()=>{post(`/student/lessons/${id}/response/autosave`,`response=${encodeURIComponent(area.value)}`).then(r=>{if(!r.ok)throw Error();dirty=false;status.textContent=`${words()} từ · đã tự động lưu`;}).catch(()=>status.textContent=`${words()} từ · chưa thể tự động lưu`);},1200);});
  window.addEventListener('beforeunload',e=>{if(dirty){e.preventDefault();e.returnValue='';}});form.addEventListener('submit',()=>{dirty=false;});label();
 }
 let lastSent='';const observer=new IntersectionObserver(entries=>{const visible=entries.filter(e=>e.isIntersecting).sort((a,b)=>b.intersectionRatio-a.intersectionRatio)[0];if(!visible||visible.target.id===lastSent)return;lastSent=visible.target.id;post(`/student/lessons/${id}/section`,`section=${encodeURIComponent(lastSent)}`).catch(()=>{});},{threshold:[.35,.65]});document.querySelectorAll('#overview,#media,#method,#example,#practice').forEach(section=>observer.observe(section));
 const recorderBox=document.getElementById('speaking-recorder');if(recorderBox){const start=document.getElementById('record-start'),stop=document.getElementById('record-stop'),save=document.getElementById('record-save'),preview=document.getElementById('record-preview'),status=document.getElementById('record-status');let recorder,chunks=[],blob;
  start.addEventListener('click',async()=>{try{const stream=await navigator.mediaDevices.getUserMedia({audio:true});chunks=[];recorder=new MediaRecorder(stream);recorder.ondataavailable=e=>{if(e.data.size)chunks.push(e.data);};recorder.onstop=()=>{blob=new Blob(chunks,{type:recorder.mimeType||'audio/webm'});preview.src=URL.createObjectURL(blob);save.disabled=false;stream.getTracks().forEach(t=>t.stop());status.textContent='Đã ghi xong. Hãy nghe lại trước khi lưu.';};recorder.start();start.disabled=true;stop.disabled=false;status.textContent='Đang ghi âm…';}catch(e){status.textContent='Không thể dùng micro. Hãy kiểm tra quyền của trình duyệt.';}});
  stop.addEventListener('click',()=>{if(recorder?.state==='recording')recorder.stop();start.disabled=false;stop.disabled=true;});save.addEventListener('click',async()=>{if(!blob)return;const data=new FormData();data.append('recording',blob,'speaking.webm');try{const response=await fetch(`/student/lessons/${id}/recording`,{method:'POST',headers:{'X-CSRF-TOKEN':csrf()||''},body:data});if(!response.ok)throw Error();status.textContent='Đã lưu bản ghi luyện nói.';save.disabled=true;}catch(e){status.textContent='Chưa thể lưu bản ghi. Vui lòng thử lại.';}});
 }
})();
