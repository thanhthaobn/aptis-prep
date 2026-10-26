// Page behavior; form values and URLs come from the rendered HTML.
(() => {
  const csrf =
    document.querySelector('meta[name="_csrf"]')?.content ||
    document.querySelector('input[name="_csrf"]')?.value ||
    '';
  const forms = [...document.querySelectorAll('.answer-form')];
  const timers = new Map();
  const inFlight = new Set();
  const failedForms = new Set();
  const submitForm = document.getElementById('submit-attempt-form');
  const submitButton = document.getElementById('submit-attempt');
  const submitError = document.getElementById('attempt-submit-error');
  let submitting = false,
    confirming = false,
    deadlineReached = false;
  const save = (form) => {
    const status = form.querySelector('.autosave-status');
    if (!csrf || !status) return Promise.resolve(true);
    status.textContent = 'Đang lưu…';
    const request = fetch(form.dataset.autosave, {
      method: 'POST',
      headers: {
        'X-CSRF-TOKEN': csrf,
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
        Accept: 'application/json',
      },
      body: new URLSearchParams(new FormData(form)),
    })
      .then((response) =>
        response.ok ? response.json() : Promise.reject(new Error('Không thể lưu câu trả lời')),
      )
      .then((result) => {
        if (!result.saved) throw new Error(result.message || 'Chưa lưu được');
        failedForms.delete(form);
        status.textContent = 'Đã tự động lưu';
        return true;
      })
      .catch((error) => {
        failedForms.add(form);
        status.textContent = error.message || 'Mất kết nối, câu trả lời chưa được lưu';
        throw error;
      });
    inFlight.add(request);
    request.then(
      () => inFlight.delete(request),
      () => inFlight.delete(request),
    );
    return request;
  };
  const schedule = (form) => {
    clearTimeout(timers.get(form));
    timers.set(
      form,
      setTimeout(() => {
        timers.delete(form);
        save(form).catch(() => {});
      }, 700),
    );
  };
  forms.forEach((form) => {
    form.addEventListener('input', () => schedule(form));
    form.addEventListener('change', () => schedule(form));
    form.addEventListener('submit', (event) => {
      event.preventDefault();
      save(form).catch(() => {});
    });
  });
  const submit = async (automatic) => {
    if (automatic) {
      deadlineReached = true;
      window.AppDialog.dismiss();
    }
    if (submitting || (!automatic && confirming)) return;
    if (!automatic) {
      confirming = true;
      const confirmed = await window.AppDialog.confirm({
        title: 'Nộp bài thi?',
        message:
          'Hệ thống sẽ lưu các câu trả lời cuối cùng trước khi nộp. Sau khi nộp, bạn không thể chỉnh sửa bài làm.',
        confirmText: 'Nộp bài',
        cancelText: 'Tiếp tục làm bài',
      });
      confirming = false;
      if (!confirmed || submitting || deadlineReached) return;
    }
    submitting = true;
    if (submitButton) {
      submitButton.disabled = true;
      submitButton.textContent = 'Đang nộp bài…';
    }
    if (submitError) submitError.hidden = true;
    submitForm?.setAttribute('aria-busy', 'true');
    forms.forEach((form) => {
      form.inert = true;
    });
    timers.forEach((timer, form) => {
      clearTimeout(timer);
      save(form).catch(() => {});
    });
    timers.clear();
    await Promise.allSettled([...inFlight]);
    // A previously failed autosave must be retried before a manual submission can lock the attempt.
    if (!automatic && !deadlineReached) await Promise.allSettled([...failedForms].map(save));
    if (!automatic && !deadlineReached && failedForms.size) {
      submitting = false;
      if (submitButton) {
        submitButton.disabled = false;
        submitButton.textContent = 'Nộp bài';
      }
      forms.forEach((form) => {
        form.inert = false;
      });
      submitForm?.removeAttribute('aria-busy');
      if (submitError) {
        submitError.textContent =
          'Chưa thể lưu đầy đủ đáp án. Bài chưa được nộp. Vui lòng kiểm tra kết nối và thử lại.';
        submitError.hidden = false;
        submitError.focus();
      }
      return;
    }
    submitForm?.submit();
  };
  submitForm?.addEventListener('submit', (event) => {
    event.preventDefault();
    submit(false);
  });
  const timer = document.getElementById('exam-timer');
  const display = document.getElementById('timer-display');
  if (timer && display) {
    const deadline = Date.parse(timer.dataset.deadline);
    let interval;
    const tick = () => {
      const seconds = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
      display.textContent =
        String(Math.floor(seconds / 60)).padStart(2, '0') +
        ':' +
        String(seconds % 60).padStart(2, '0');
      if (!seconds) {
        if (interval) clearInterval(interval);
        submit(true);
      }
    };
    tick();
    interval = setInterval(tick, 500);
  }
  const fullMock = document.body.classList.contains('full-mock-exam');
  if (fullMock) {
    const pages = [...document.querySelectorAll('.full-mock-question')];
    const pageInfo = document.getElementById('full-mock-page-info');
    const previous = document.getElementById('full-mock-prev');
    const next = document.getElementById('full-mock-next');
    const progressCount = document.getElementById('full-mock-progress-count');
    const progressBar = document.getElementById('full-mock-progress-bar');
    const progressTrack = progressBar?.parentElement;
    let currentPage = 0;
    const isAnswered = (item) =>
      item.dataset.saved === 'true' ||
      !!item.querySelector('input[type="radio"]:checked') ||
      [...item.querySelectorAll('input[type="number"], textarea')].some(
        (input) => input.value.trim() !== '',
      );
    const updateProgress = () => {
      const items = pages.flatMap((page) => [...page.querySelectorAll('.question-item')]);
      const answered = items.filter(isAnswered).length;
      const total = items.length;
      const percent = total ? Math.round((answered * 100) / total) : 0;
      if (progressCount) progressCount.textContent = `${answered} / ${total} câu đã trả lời`;
      if (progressBar) progressBar.style.width = `${percent}%`;
      if (progressTrack) {
        progressTrack.setAttribute('aria-valuemax', total);
        progressTrack.setAttribute('aria-valuenow', answered);
      }
    };
    const showPage = (index) => {
      if (!pages.length) return;
      currentPage = Math.max(0, Math.min(index, pages.length - 1));
      pages.forEach((page, pageIndex) => {
        const active = pageIndex === currentPage;
        page.classList.toggle('is-active', active);
        page.setAttribute('aria-hidden', String(!active));
      });
      if (pageInfo) pageInfo.textContent = `Câu ${currentPage + 1} / ${pages.length}`;
      if (previous) previous.disabled = currentPage === 0;
      if (next) next.disabled = currentPage === pages.length - 1;
      updateProgress();
      pages[currentPage].querySelector('h2')?.focus({ preventScroll: true });
      window.scrollTo({ top: 0, behavior: 'smooth' });
    };
    document.body.classList.add('pagination-ready');
    previous?.addEventListener('click', () => showPage(currentPage - 1));
    next?.addEventListener('click', () => showPage(currentPage + 1));
    document.querySelectorAll('.answer-form').forEach((form) => {
      form.addEventListener('input', updateProgress);
      form.addEventListener('change', updateProgress);
    });
    showPage(0);
  }
  document.querySelectorAll('.media-audio').forEach((player) => {
    const status = player.parentElement.querySelector('.media-status');
    let tokenReady = false;
    let loading = false;
    let exhausted = false;
    const requestPlayback = async () => {
      if (tokenReady || loading) return;
      if (exhausted) {
        player.pause();
        status.textContent = 'Đã dùng hết số lượt nghe cho audio này.';
        return;
      }
      player.pause();
      loading = true;
      status.textContent = 'Đang kiểm tra lượt nghe…';
      try {
        const response = await fetch(player.dataset.play, {
          method: 'POST',
          headers: {
            'X-CSRF-TOKEN': csrf,
            'X-Requested-With': 'XMLHttpRequest',
            Accept: 'application/json',
          },
        });
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || 'Không thể phát audio');
        if (!result.contentToken) throw new Error('Máy chủ chưa cấp quyền phát audio');
        player.src = player.dataset.content + '?token=' + encodeURIComponent(result.contentToken);
        player.load();
        tokenReady = true;
        status.textContent = `Đã dùng ${result.playCount}/${result.maxPlayCount} lượt nghe`;
        await player.play();
        player.dataset.playCount = result.playCount;
        player.dataset.maxPlayCount = result.maxPlayCount;
      } catch (error) {
        tokenReady = false;
        status.textContent = error.message || 'Không thể phát audio';
      } finally {
        loading = false;
      }
    };
    player.addEventListener('click', (event) => {
      if (!tokenReady) {
        event.preventDefault();
        requestPlayback();
      }
    });
    player.addEventListener('play', () => {
      if (!tokenReady) {
        player.pause();
        requestPlayback();
      }
    });
    player.addEventListener('error', () => {
      if (loading) return;
      tokenReady = false;
      status.textContent = 'Không tải được audio. Hãy bấm phát lại.';
    });
    player.addEventListener('ended', () => {
      tokenReady = false;
      const playCount = Number(player.dataset.playCount || 0);
      const maxPlayCount = Number(player.dataset.maxPlayCount || 0);
      exhausted = maxPlayCount > 0 && playCount >= maxPlayCount;
      if (exhausted) status.textContent = `Đã dùng hết ${maxPlayCount} lượt nghe.`;
      else status.textContent = `Bạn còn ${Math.max(0, maxPlayCount - playCount)} lượt nghe.`;
    });
  });
  document.querySelectorAll('.speaking-recorder').forEach((box) => {
    const start = box.querySelector('.recording-start');
    const stop = box.querySelector('.recording-stop');
    const reset = box.querySelector('.recording-reset');
    const saveRecording = box.querySelector('.recording-save');
    const preview = box.querySelector('.recording-preview');
    const status = box.querySelector('.recording-status');
    let recorder;
    let stream;
    let chunks = [];
    let recordingBlob;
    let previewUrl;
    const supportedMime = [
      'audio/webm;codecs=opus',
      'audio/webm',
      'audio/mp4',
      'audio/ogg;codecs=opus',
    ].find((type) => window.MediaRecorder?.isTypeSupported?.(type));
    const extension = () =>
      recorder?.mimeType.includes('mp4')
        ? 'm4a'
        : recorder?.mimeType.includes('ogg')
          ? 'ogg'
          : 'webm';
    const setStatus = (message) => {
      status.textContent = message;
    };
    const clearPreview = () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      previewUrl = null;
      preview.removeAttribute('src');
      preview.hidden = true;
    };
    start.addEventListener('click', async () => {
      if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
        setStatus('Trình duyệt không hỗ trợ ghi âm trực tiếp.');
        return;
      }
      start.disabled = true;
      reset.disabled = true;
      saveRecording.disabled = true;
      setStatus('Đang chờ quyền microphone…');
      try {
        stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        chunks = [];
        recorder = supportedMime
          ? new MediaRecorder(stream, { mimeType: supportedMime })
          : new MediaRecorder(stream);
        recorder.ondataavailable = (event) => {
          if (event.data.size) chunks.push(event.data);
        };
        recorder.onstop = () => {
          recordingBlob = new Blob(chunks, { type: recorder.mimeType || 'audio/webm' });
          clearPreview();
          previewUrl = URL.createObjectURL(recordingBlob);
          preview.src = previewUrl;
          preview.hidden = false;
          reset.disabled = false;
          saveRecording.disabled = false;
          stream?.getTracks().forEach((track) => track.stop());
          stream = null;
          setStatus('Đã ghi xong. Hãy nghe lại trước khi lưu.');
        };
        recorder.onerror = () => {
          stream?.getTracks().forEach((track) => track.stop());
          stream = null;
          start.disabled = false;
          stop.disabled = true;
          setStatus('Không thể hoàn tất bản ghi. Vui lòng thử lại.');
        };
        recorder.start();
        stop.disabled = false;
        setStatus('Đang ghi âm…');
      } catch (error) {
        stream?.getTracks().forEach((track) => track.stop());
        stream = null;
        start.disabled = false;
        setStatus('Không thể dùng microphone. Hãy kiểm tra quyền của trình duyệt.');
      }
    });
    stop.addEventListener('click', () => {
      if (recorder?.state === 'recording') {
        recorder.stop();
        stop.disabled = true;
        start.disabled = false;
      }
    });
    reset.addEventListener('click', () => {
      recordingBlob = null;
      clearPreview();
      reset.disabled = true;
      saveRecording.disabled = true;
      start.disabled = false;
      setStatus('Bản ghi đã được xóa. Bạn có thể ghi lại.');
    });
    saveRecording.addEventListener('click', async () => {
      if (!recordingBlob) return;
      saveRecording.disabled = true;
      reset.disabled = true;
      setStatus('Đang lưu bản ghi…');
      const data = new FormData();
      data.append('questionId', box.dataset.questionId);
      data.append('itemId', box.dataset.itemId);
      data.append('audioFile', recordingBlob, `speaking-${box.dataset.itemId}.${extension()}`);
      try {
        const response = await fetch(box.dataset.audioAction, {
          method: 'POST',
          headers: { 'X-CSRF-TOKEN': csrf },
          body: data,
        });
        if (!response.ok) throw new Error();
        window.location.href = response.url;
      } catch (error) {
        saveRecording.disabled = false;
        reset.disabled = false;
        setStatus('Chưa thể lưu bản ghi. Vui lòng thử lại.');
      }
    });
  });
})();
