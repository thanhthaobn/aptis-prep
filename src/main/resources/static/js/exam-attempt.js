(() => {
  const csrf = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
  const headers = { Accept: 'application/json' };
  if (csrf && csrfHeader) headers[csrfHeader] = csrf;

  async function post(url, body) {
    const response = await fetch(url, { method: 'POST', headers, body });
    const result = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(result.message || 'Yêu cầu chưa hoàn tất. Hãy thử lại.');
    return result;
  }

  const items = [...document.querySelectorAll('.question-item[data-item-id]')];
  const links = new Map(
    [...document.querySelectorAll('.exam-question-link')].map((link) => [link.dataset.itemId, link]),
  );
  const progress = document.querySelector('#exam-progress-count strong');
  const completionStatus = document.getElementById('attempt-completion-status');
  const actionButtons = document.querySelectorAll('#submit-attempt, #next-section');
  const pending = new Set();
  const failed = new Set();

  function updateProgress() {
    const answered = items.filter((item) => item.dataset.answered === 'true').length;
    progress.textContent = answered;
    const incomplete = items.length - answered;
    actionButtons.forEach((button) => {
      button.disabled = !items.length || incomplete > 0 || pending.size > 0 || failed.size > 0;
    });
    if (failed.size) completionStatus.textContent = 'Hãy lưu lại các câu đang báo lỗi.';
    else if (pending.size) completionStatus.textContent = 'Đang xử lý đáp án.';
    else if (incomplete) completionStatus.textContent = `Còn ${incomplete} câu chưa trả lời.`;
    else completionStatus.textContent = '';
  }

  function setSaveState(form, state, answered) {
    pending.delete(form);
    failed.delete(form);
    if (state === 'pending') pending.add(form);
    if (state === 'failed') failed.add(form);
    if (answered !== undefined) {
      const item = form.closest('.question-item');
      item.dataset.answered = String(answered);
      links.get(item.dataset.itemId)?.classList.toggle('is-answered', answered);
    }
    updateProgress();
  }

  for (const button of actionButtons) {
    button.form.addEventListener('submit', (event) => {
      if (button.disabled) event.preventDefault();
    });
  }
  document.getElementById('exit-attempt-form').addEventListener('submit', (event) => {
    if (pending.size) event.preventDefault();
  });
  updateProgress();

  for (const form of document.querySelectorAll('.answer-form')) {
    const status = form.querySelector('.answer-status');
    const retry = form.querySelector('.save-retry');
    let queued;
    let saving = false;
    let timer;

    async function save() {
      window.clearTimeout(timer);
      if (saving) return;
      saving = true;
      while (queued) {
        const { body, valid } = queued;
        queued = null;
        try {
          if (!valid) throw new Error('Hãy nhập giá trị hợp lệ.');
          const result = await post(form.action, body);
          if (!result.saved) throw new Error('Đáp án chưa được lưu. Hãy thử lại.');
          const answered = ['selectedOptionId', 'userPosition', 'textAnswer'].some(
            (name) => body.get(name)?.trim(),
          );
          setSaveState(form, queued ? 'pending' : 'saved', answered);
          status.textContent = queued ? 'Đang lưu…' : 'Đã lưu.';
        } catch (error) {
          if (queued) continue;
          status.textContent = error.message;
          retry.hidden = !valid;
          setSaveState(form, 'failed');
        }
      }
      saving = false;
    }

    function queueSave(delay) {
      queued = { body: new URLSearchParams(new FormData(form)), valid: form.checkValidity() };
      status.textContent = 'Đang lưu…';
      retry.hidden = true;
      setSaveState(form, 'pending');
      window.clearTimeout(timer);
      timer = window.setTimeout(save, delay);
    }

    form.addEventListener('input', (event) => {
      queueSave(event.target.matches('textarea, input[type="number"]') ? 350 : 0);
    });
    form.addEventListener('submit', (event) => {
      event.preventDefault();
      queueSave(0);
    });
    retry.addEventListener('click', () => queueSave(0));
  }

  for (const box of document.querySelectorAll('.media-player')) {
    const player = box.querySelector('.media-audio');
    const play = box.querySelector('.media-play-trigger');
    const status = box.querySelector('.media-status');
    let remaining;
    let position = 0;

    function resetPlayer() {
      player.hidden = true;
      player.removeAttribute('src');
      player.load();
      position = 0;
      play.hidden = remaining === 0;
      play.disabled = remaining === 0;
      play.textContent = 'Nghe lại';
    }

    play.addEventListener('click', async () => {
      play.disabled = true;
      status.textContent = 'Đang tải bài nghe…';
      try {
        const result = await post(player.dataset.play);
        if (!result.contentToken) throw new Error('Máy chủ chưa cấp quyền phát audio.');
        remaining = result.maxPlayCount - result.playCount;
        const url = new URL(player.dataset.content, window.location.href);
        url.searchParams.set('token', result.contentToken);
        player.src = url.href;
        player.hidden = false;
        play.hidden = true;
        status.textContent = `Đã dùng ${result.playCount}/${result.maxPlayCount} lượt nghe.`;
        try {
          await player.play();
        } catch (error) {
          if (error.name === 'NotAllowedError') {
            status.textContent = 'Bấm nút phát trên trình nghe để bắt đầu.';
            player.focus();
          } else if (!player.hidden) {
            resetPlayer();
            status.textContent = 'Không phát được tệp âm thanh.';
          }
        }
      } catch (error) {
        status.textContent = error.message;
        play.disabled = false;
      }
    });
    player.addEventListener('timeupdate', () => {
      if (!player.seeking) position = player.currentTime;
    });
    player.addEventListener('seeking', () => {
      if (Math.abs(player.currentTime - position) > 0.25) player.currentTime = position;
    });
    player.addEventListener('ended', () => {
      resetPlayer();
      status.textContent = remaining ? `Còn ${remaining} lượt nghe.` : 'Đã dùng hết lượt nghe.';
    });
    player.addEventListener('error', () => {
      if (player.hidden) return;
      resetPlayer();
      status.textContent = 'Không tải được tệp âm thanh.';
    });
  }

  let activeCapture;
  for (const form of document.querySelectorAll('.speaking-recorder')) {
    const microphone = form.querySelector('.microphone-recorder');
    const start = form.querySelector('.recording-start');
    const stop = form.querySelector('.recording-stop');
    const retryRecording = form.querySelector('.recording-retry');
    const saveRecording = form.querySelector('.recording-save');
    const preview = form.querySelector('.recording-preview');
    const input = form.querySelector('input[type="file"]');
    const status = form.querySelector('.recording-status');
    const retry = form.querySelector('.save-retry');
    const maxSeconds = Number(form.dataset.responseSeconds) || 45;
    let stream;
    let recorder;
    let timer;
    let draft;
    let previewUrl;

    async function save() {
      if (!draft || input.disabled) return;
      const body = new FormData(form);
      body.set('audioFile', draft);
      start.disabled = true;
      input.disabled = true;
      retryRecording.disabled = true;
      saveRecording.disabled = true;
      retry.hidden = true;
      status.textContent = 'Đang lưu bản ghi…';
      setSaveState(form, 'pending');
      try {
        const result = await post(form.action, body);
        if (!result.saved) throw new Error('Bản ghi chưa được lưu. Hãy thử lại.');
        draft = null;
        input.value = '';
        status.textContent = 'Đã lưu bản ghi.';
        setSaveState(form, 'saved', true);
      } catch (error) {
        status.textContent = error.message;
        retry.hidden = false;
        setSaveState(form, 'failed');
      } finally {
        start.disabled = false;
        input.disabled = false;
        retryRecording.disabled = false;
        saveRecording.disabled = !draft;
      }
    }

    function clearDraft() {
      draft = null;
      input.value = '';
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      previewUrl = null;
      preview.hidden = true;
      preview.removeAttribute('src');
      preview.load();
      retryRecording.hidden = true;
      saveRecording.disabled = true;
    }

    function releaseMicrophone() {
      window.clearTimeout(timer);
      stream?.getTracks().forEach((track) => track.stop());
      stream = null;
      recorder = null;
      activeCapture = null;
      start.disabled = false;
      stop.disabled = true;
      input.disabled = false;
      retryRecording.disabled = false;
    }

    function failRecording(message) {
      releaseMicrophone();
      status.textContent = message;
      retry.hidden = !draft;
      setSaveState(form, draft ? 'failed' : 'idle');
    }

    microphone.hidden = !navigator.mediaDevices?.getUserMedia || !window.MediaRecorder;
    if (microphone.hidden && !status.textContent.trim()) {
      status.textContent = 'Ghi trực tiếp cần HTTPS hoặc localhost và trình duyệt hỗ trợ mic.';
    }
    start.addEventListener('click', async () => {
      if (activeCapture) {
        status.textContent = 'Hãy dừng ghi âm ở câu đang mở mic trước.';
        return;
      }
      activeCapture = form;
      start.disabled = true;
      input.disabled = true;
      retry.hidden = true;
      status.textContent = 'Đang chờ quyền microphone…';
      setSaveState(form, 'pending');
      try {
        stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const mimeType = ['audio/webm;codecs=opus', 'audio/mp4', 'audio/ogg;codecs=opus'].find(
          (type) => MediaRecorder.isTypeSupported?.(type),
        );
        const capture = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream);
        recorder = capture;
        const chunks = [];
        capture.addEventListener('dataavailable', (event) => {
          if (event.data.size) chunks.push(event.data);
        });
        capture.addEventListener('error', () => {
          if (recorder === capture) failRecording('Ghi âm bị gián đoạn. Hãy thử lại.');
        });
        capture.addEventListener('stop', () => {
          if (recorder !== capture) return;
          if (!chunks.length) {
            failRecording('Mic chưa thu được âm thanh. Hãy thử lại.');
            return;
          }
          releaseMicrophone();
          const type = capture.mimeType || chunks[0].type || 'audio/webm';
          const extension = type.includes('mp4') ? 'm4a' : type.includes('ogg') ? 'ogg' : 'webm';
          draft = new File(chunks, `speaking.${extension}`, { type });
          if (previewUrl) URL.revokeObjectURL(previewUrl);
          previewUrl = URL.createObjectURL(draft);
          preview.src = previewUrl;
          preview.hidden = false;
          retryRecording.hidden = false;
          saveRecording.disabled = false;
          status.textContent = 'Bản ghi mới đã sẵn sàng. Hãy nghe lại và lưu.';
          setSaveState(form, 'idle');
        });
        capture.start();
        stop.disabled = false;
        status.textContent = `Đang ghi âm (tối đa ${maxSeconds} giây).`;
        timer = window.setTimeout(() => stop.click(), maxSeconds * 1000);
      } catch (error) {
        failRecording(
          error.name === 'NotAllowedError'
            ? 'Hãy cấp quyền microphone cho trang hoặc tải tệp ghi âm.'
            : 'Không mở được microphone. Hãy kiểm tra thiết bị hoặc tải tệp ghi âm.',
        );
      }
    });
    stop.addEventListener('click', () => {
      if (recorder?.state !== 'recording') return;
      stop.disabled = true;
      recorder.stop();
    });
    retryRecording.addEventListener('click', () => {
      if (recorder?.state === 'recording') return;
      clearDraft();
      status.textContent = 'Sẵn sàng ghi bản mới.';
      start.click();
    });
    saveRecording.addEventListener('click', save);
    input.addEventListener('change', () => {
      if (!input.files.length) return;
      draft = input.files[0];
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      previewUrl = null;
      preview.hidden = true;
      preview.removeAttribute('src');
      preview.load();
      preview.src = URL.createObjectURL(draft);
      previewUrl = preview.src;
      preview.hidden = false;
      retryRecording.hidden = microphone.hidden;
      saveRecording.disabled = false;
      status.textContent = 'Tệp mới đã sẵn sàng. Hãy lưu ghi âm.';
      setSaveState(form, 'idle');
    });
    retry.addEventListener('click', save);
    form.addEventListener('submit', (event) => event.preventDefault());
  }
})();
