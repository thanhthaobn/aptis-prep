(() => {
 const page=document.querySelector('.lesson-classroom'),form=document.getElementById('lesson-response-form');
 if(!page)return;const id=page.dataset.lessonId;
 const csrf=()=>document.querySelector('input[name="_csrf"]')?.value;
 const post=(url,body='')=>fetch(url,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded','X-CSRF-TOKEN':csrf()||''},body});
 const audio=document.getElementById('lesson-audio');let marked=false,playbackSave=Promise.resolve();
 if(audio){
  const message=document.getElementById('listening-status');
  let session=null,pending=null,timer=null,seeking=false,starting=false,resuming=false;
  const status=text=>{if(message)message.textContent=text;};
  const response=async request=>{const result=await request;if(!result.ok||result.redirected){const error=Error();error.status=result.status;throw error;}return result.json();};
  function send(discontinuity=false){
   const position=audio.currentTime,playing=!audio.paused&&!audio.ended;
   playbackSave=playbackSave.catch(()=>{}).then(async()=>{
    try{
     if(!session){session=await response(post(`/student/lessons/${id}/listening/start`,new URLSearchParams({position})));marked=session.verified;}
     else{
      // Keep the exact request/sequence after a lost response so retries cannot add time twice.
      if(!pending)pending={token:session.token,sequence:session.sequence+1,position,playing,discontinuity};
      const result=await response(post(`/student/lessons/${id}/listening/heartbeat`,new URLSearchParams(pending)));
      session={...result,token:session.token};pending=null;marked=result.verified;
     }
     status(marked?'Đã xác nhận nghe đủ 90% bài.':`Đã xác nhận ${Math.min(100,Math.floor(session.listened/session.duration*100))}% bài nghe.`);
    }catch(error){if(error.status===400){session=null;pending=null;}status('Chưa xác nhận được tiến trình nghe. Hãy kiểm tra kết nối rồi phát lại audio.');}
   });
   return playbackSave;
  }
  audio.addEventListener('play',()=>{
   if(!session){
    audio.pause();
    if(starting)return;
    starting=true;
    send(true).then(()=>{starting=false;if(session){resuming=true;audio.play().catch(()=>status('Phiên nghe đã sẵn sàng. Bấm phát để tiếp tục.'));}});
    return;
   }
   if(!resuming)send(true);
   resuming=false;clearInterval(timer);timer=setInterval(()=>{if(!audio.paused&&!seeking)send();},4000);
  });
  audio.addEventListener('pause',()=>{clearInterval(timer);if(session)send();});
  audio.addEventListener('seeking',()=>{seeking=true;});
  audio.addEventListener('seeked',()=>{seeking=false;if(session)send(true);});
  audio.addEventListener('ratechange',()=>{if(audio.playbackRate!==1){audio.playbackRate=1;status('Bài nghe yêu cầu tốc độ phát 1x để xác nhận tiến trình.');}});
  // Do not discard an unfinished quiz when newly verified audio unlocks its transcript.
  audio.addEventListener('ended',async()=>{clearInterval(timer);await playbackSave;if(session)await send();if(marked&&!document.querySelector('.lesson-knowledge-check input[type="radio"]:checked'))window.location.reload();});
  window.addEventListener('pagehide',()=>clearInterval(timer));
 }
 if(form){const area=document.getElementById('response'),status=document.getElementById('response-status');let timer,dirty=false;
  const words=()=>area.value.trim()?area.value.trim().split(/\s+/).length:0;
  const label=()=>{const count=words(),min=Number(area.dataset.minWords||0),max=Number(area.dataset.maxWords||0);status.textContent=`${count} từ${min||max?` · yêu cầu ${min||0}${max?`–${max}`:'+'} từ`:''}${dirty?' · chưa lưu':''}`;};
  area.addEventListener('input',()=>{dirty=true;label();clearTimeout(timer);timer=setTimeout(()=>{post(`/student/lessons/${id}/response/autosave`,`response=${encodeURIComponent(area.value)}`).then(r=>{if(!r.ok)throw Error();dirty=false;status.textContent=`${words()} từ · đã tự động lưu`;}).catch(()=>status.textContent=`${words()} từ · chưa thể tự động lưu`);},1200);});
  window.addEventListener('beforeunload',e=>{if(dirty){e.preventDefault();e.returnValue='';}});form.addEventListener('submit',()=>{clearTimeout(timer);dirty=false;});label();
 }
 let lastSent='';const observer=new IntersectionObserver(entries=>{const visible=entries.filter(e=>e.isIntersecting).sort((a,b)=>b.intersectionRatio-a.intersectionRatio)[0];if(!visible||visible.target.id===lastSent)return;lastSent=visible.target.id;post(`/student/lessons/${id}/section`,`section=${encodeURIComponent(lastSent)}`).catch(()=>{});},{threshold:[.35,.65]});document.querySelectorAll('#overview,#media,#method,#example,#practice').forEach(section=>observer.observe(section));
 const recorderBox=document.getElementById('speaking-recorder');if(recorderBox){const start=document.getElementById('record-start'),stop=document.getElementById('record-stop'),save=document.getElementById('record-save'),preview=document.getElementById('record-preview'),status=document.getElementById('record-status');let recorder,chunks=[],blob;
  start.addEventListener('click',async()=>{let stream;start.disabled=true;status.textContent='Đang chờ quyền micro. Hãy cho phép trong trình duyệt.';try{stream=await navigator.mediaDevices.getUserMedia({audio:true});chunks=[];recorder=new MediaRecorder(stream);recorder.ondataavailable=e=>{if(e.data.size)chunks.push(e.data);};recorder.onstop=()=>{if(preview.src.startsWith('blob:'))URL.revokeObjectURL(preview.src);blob=new Blob(chunks,{type:recorder.mimeType||'audio/webm'});preview.src=URL.createObjectURL(blob);preview.hidden=false;save.disabled=false;stream.getTracks().forEach(t=>t.stop());status.textContent='Đã ghi xong. Hãy nghe lại trước khi lưu.';};recorder.start();stop.disabled=false;status.textContent='Đang ghi âm…';}catch(e){stream?.getTracks().forEach(t=>t.stop());start.disabled=false;status.textContent='Không thể dùng micro. Hãy kiểm tra quyền của trình duyệt.';}});
  stop.addEventListener('click',()=>{if(recorder?.state==='recording')recorder.stop();start.disabled=false;stop.disabled=true;});save.addEventListener('click',async()=>{if(!blob)return;const data=new FormData();data.append('recording',blob,'speaking.webm');try{const response=await fetch(`/student/lessons/${id}/recording`,{method:'POST',headers:{'X-CSRF-TOKEN':csrf()||''},body:data});if(!response.ok)throw Error();save.disabled=true;window.location.reload();}catch(e){status.textContent='Chưa thể lưu bản ghi. Vui lòng thử lại.';}});
 }
})();
