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
  const failedRecordings = new Set();
  const submitForm = document.getElementById('submit-attempt-form');
  const submitButton = document.getElementById('submit-attempt');
  const nextSectionForm = document.getElementById('next-section-form');
  const nextSectionButton = document.getElementById('next-section');
  const submitError = document.getElementById('attempt-submit-error');
  const questionSections = [...document.querySelectorAll('.exam-question-page')];
  const questionLinks = [...document.querySelectorAll('.exam-question-link')];
  const answerItems = new Map();
  questionSections.forEach((section, index) => {
    section.querySelectorAll('.question-item').forEach((item) => {
      answerItems.set(item.dataset.itemId, { item, index });
    });
  });
  const examProgressCount = document.getElementById('exam-progress-count');
  let submitting = false,
    confirming = false,
    deadlineReached = false;
  const itemIsAnswered = (item) =>
    item.dataset.saved === 'true' ||
    !!item.querySelector('input[type="radio"]:checked') ||
    [...item.querySelectorAll('input[type="number"], textarea')].some(
      (input) => input.value.trim() !== '',
    );
  const wordCount = (value) => (value.trim() ? value.trim().split(/\s+/).length : 0);
  const writingLimitErrors = () => {
    const errors = [];
    for (const textarea of document.querySelectorAll(
      'textarea[data-min-words], textarea[data-max-words]',
    )) {
      const words = wordCount(textarea.value);
      const min = Number(textarea.dataset.minWords || 0);
      const max = Number(textarea.dataset.maxWords || 0);
      if (min > 0 && words < min)
        errors.push({
          textarea,
          message: `Câu trả lời cần ít nhất ${min} từ (hiện có ${words} từ).`,
        });
      else if (max > 0 && words > max)
        errors.push({
          textarea,
          message: `Câu trả lời không được vượt quá ${max} từ (hiện có ${words} từ).`,
        });
    }
    return errors;
  };
  const clearWritingLimitErrors = () =>
    document.querySelectorAll('.writing-limit-error').forEach((error) => {
      error.hidden = true;
      error.textContent = '';
    });
  document
    .querySelectorAll('textarea[data-min-words], textarea[data-max-words]')
    .forEach((textarea) => {
      textarea.addEventListener('input', () => {
        textarea
          .closest('.field')
          ?.querySelector('.writing-limit-error')
          ?.setAttribute('hidden', '');
      });
    });
  const currentQuestion = document.getElementById('exam-current-question');
  const markCurrentQuestion = (item) => {
    if (!item) return;
    document.querySelectorAll('.media-audio').forEach((player) => {
      if (!item.contains(player)) {
        player.pause();
        player.currentTime = 0;
      }
    });
    questionLinks.forEach((link) => {
      const active = link.dataset.itemId === item.dataset.itemId;
      link.classList.toggle('is-current', active);
      link.setAttribute('aria-current', active ? 'step' : 'false');
      if (active && currentQuestion) currentQuestion.textContent = 'Câu ' + link.textContent.trim();
    });
  };
  const updateExamNavigation = () => {
    let answeredItems = 0;
    questionLinks.forEach((link) => {
      const target = answerItems.get(link.dataset.itemId);
      if (target) link.classList.toggle('is-answered', itemIsAnswered(target.item));
      if (link.classList.contains('is-answered')) answeredItems++;
    });
    if (examProgressCount)
      examProgressCount.innerHTML = `<strong>${answeredItems}</strong>/${questionLinks.length} câu đã làm`;
  };
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
    form.addEventListener('input', () => {
      schedule(form);
      updateExamNavigation();
    });
    form.addEventListener('change', () => {
      schedule(form);
      updateExamNavigation();
    });
    form.addEventListener('submit', (event) => {
      event.preventDefault();
      save(form).catch(() => {});
    });
  });
  updateExamNavigation();
  answerItems.forEach(({ item }) => {
    item.addEventListener('focusin', () => markCurrentQuestion(item));
    item.addEventListener('recording-saved', updateExamNavigation);
  });
  const goToItem = (item) => {
    markCurrentQuestion(item);
    item.focus({ preventScroll: true });
    item.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };
  document.getElementById('exit-attempt-form')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const exitForm = event.currentTarget;
    const confirmed = await window.AppDialog.confirm({
      title: 'Thoát bài thi?',
      message:
        'Các câu trả lời đã lưu vẫn được giữ lại. Bạn có chắc muốn rời bài thi và tiếp tục sau không?',
      confirmText: 'Thoát bài thi',
      cancelText: 'Ở lại làm bài',
    });
    if (confirmed && !submitting && !deadlineReached) exitForm.submit();
  });
  if (!document.body.classList.contains('paginated-exam')) {
    questionLinks.forEach((link) => {
      link.addEventListener('click', () => {
        const target = answerItems.get(link.dataset.itemId);
        if (target) goToItem(target.item);
      });
    });
    markCurrentQuestion(answerItems.values().next().value?.item);
  }
  const submit = async (automatic, nextSection = false) => {
    const formToSubmit = nextSection ? nextSectionForm : submitForm;
    const button = nextSection ? nextSectionButton : submitButton;
    const buttonLabel = nextSection ? 'Sang phần tiếp theo' : 'Nộp bài';
    if (!automatic) {
      const incomplete = [...answerItems.values()]
        .map(({ item }) => item)
        .filter((item) => !itemIsAnswered(item));
      if (incomplete.length) {
        if (submitError) {
          submitError.textContent = `Bạn còn ${incomplete.length} câu chưa hoàn thành. Hãy trả lời đầy đủ trước khi ${nextSection ? 'sang phần tiếp theo' : 'nộp bài'}.`;
          submitError.hidden = false;
          submitError.focus();
        }
        const firstIncomplete = incomplete[0];
        questionLinks
          .find((link) => link.dataset.itemId === firstIncomplete.dataset.itemId)
          ?.click();
        return;
      }
      clearWritingLimitErrors();
      const invalid = writingLimitErrors();
      invalid.forEach(({ textarea, message }) => {
        const error = textarea.closest('.field')?.querySelector('.writing-limit-error');
        if (error) {
          error.textContent = message;
          error.hidden = false;
        }
      });
      if (invalid.length) {
        invalid[0].textarea.focus();
        return;
      }
    }
    if (automatic) {
      deadlineReached = true;
      window.AppDialog.dismiss();
    }
    if (submitting || (!automatic && confirming)) return;
    if (
      !automatic &&
      [...document.querySelectorAll('.speaking-recorder')].some(
        (box) =>
          box.dataset.state === 'recording' ||
          (box.dataset.dirty === 'true' && box.dataset.state !== 'preview'),
      )
    ) {
      if (submitError) {
        submitError.textContent = 'Hãy dừng ghi và lưu bản ghi Speaking trước khi tiếp tục.';
        submitError.hidden = false;
        submitError.focus();
      }
      return;
    }
    if (!automatic) {
      confirming = true;
      const confirmed = await window.AppDialog.confirm({
        title: nextSection ? 'Sang phần tiếp theo?' : 'Nộp bài thi?',
        message: nextSection
          ? 'Bạn sẽ không thể quay lại phần này. Phần tiếp theo có thời gian riêng.'
          : 'Hệ thống sẽ lưu các câu trả lời cuối cùng trước khi nộp. Sau khi nộp, bạn không thể chỉnh sửa bài làm.',
        confirmText: buttonLabel,
        cancelText: 'Tiếp tục làm bài',
      });
      confirming = false;
      if (!confirmed || submitting || deadlineReached) return;
    }
    submitting = true;
    if (button) {
      button.disabled = true;
      button.textContent = nextSection ? 'Đang chuyển phần…' : 'Đang nộp bài…';
    }
    if (submitError) submitError.hidden = true;
    formToSubmit?.setAttribute('aria-busy', 'true');
    // A stopped recorder keeps its blob in the preview until it is uploaded.
    // Persist those previews before navigating to another section or submitting
    // the attempt, otherwise a page reload would lose the recording.
    document.querySelectorAll('.speaking-recorder').forEach((box) => {
      if (box.dataset.state === 'preview' && box.dataset.dirty === 'true')
        box.querySelector('.recording-save')?.click();
    });
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
    if (!automatic && !deadlineReached && (failedForms.size || failedRecordings.size)) {
      submitting = false;
      if (button) {
        button.disabled = false;
        button.textContent = buttonLabel;
      }
      forms.forEach((form) => {
        form.inert = false;
      });
      formToSubmit?.removeAttribute('aria-busy');
      if (submitError) {
        submitError.textContent = nextSection
          ? 'Chưa lưu được tất cả câu trả lời hoặc bản ghi. Hãy kiểm tra kết nối và thử lại.'
          : 'Chưa thể lưu đầy đủ đáp án hoặc bản ghi. Bài chưa được nộp. Vui lòng kiểm tra kết nối và thử lại.';
        submitError.hidden = false;
        submitError.focus();
      }
      return;
    }
    const automaticInput = formToSubmit?.querySelector('input[name="automatic"]');
    if (automaticInput) automaticInput.value = String(automatic);
    formToSubmit?.submit();
  };
  submitForm?.addEventListener('submit', (event) => {
    event.preventDefault();
    submit(false);
  });
  nextSectionForm?.addEventListener('submit', (event) => {
    event.preventDefault();
    submit(false, true);
  });
  const timer = document.getElementById('exam-timer');
  const display = document.getElementById('timer-display');
  if (timer && display) {
    const deadline = timer.dataset.sectionSeconds
      ? Date.now() + Number(timer.dataset.sectionSeconds) * 1000
      : Date.parse(timer.dataset.deadline);
    let interval;
    const tick = () => {
      const seconds = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
      display.textContent =
        String(Math.floor(seconds / 60)).padStart(2, '0') +
        ':' +
        String(seconds % 60).padStart(2, '0');
      if (!seconds) {
        if (interval) clearInterval(interval);
        if (timer.dataset.nextSection === 'true') {
          // Advance the server-side section clock instead of reloading the same
          // expired section indefinitely.
          submit(true, true);
        } else {
          submit(true);
        }
      }
    };
    tick();
    interval = setInterval(tick, 500);
  }
  const paginatedExam = document.body.classList.contains('paginated-exam');
  if (paginatedExam) {
    const pages = [...document.querySelectorAll('.exam-question-page')];
    const pageInfo = document.getElementById('exam-page-info');
    const previous = document.getElementById('exam-page-prev');
    const next = document.getElementById('exam-page-next');
    let currentPage = 0;
    const showPage = (index, item) => {
      if (!pages.length) return;
      currentPage = Math.max(0, Math.min(index, pages.length - 1));
      pages.forEach((page, pageIndex) => {
        const active = pageIndex === currentPage;
        page.classList.toggle('is-active', active);
        page.setAttribute('aria-hidden', String(!active));
      });
      markCurrentQuestion(item || pages[currentPage].querySelector('.question-item'));
      if (pageInfo) pageInfo.textContent = `Phần ${currentPage + 1} / ${pages.length}`;
      if (previous) previous.disabled = currentPage === 0;
      if (next) next.disabled = currentPage === pages.length - 1;
      if (item) {
        goToItem(item);
      } else {
        pages[currentPage].querySelector('h2')?.focus({ preventScroll: true });
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    };
    document.body.classList.add('pagination-ready');
    previous?.addEventListener('click', () => showPage(currentPage - 1));
    next?.addEventListener('click', () => showPage(currentPage + 1));
    questionLinks.forEach((link) =>
      link.addEventListener('click', () => {
        const target = answerItems.get(link.dataset.itemId);
        if (target) showPage(target.index, target.item);
      }),
    );
    showPage(0);
  }
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
  document.querySelectorAll('.media-audio').forEach((player) => {
    const container = player.closest('.media-player');
    const status = container.querySelector('.media-status');
    const playTrigger = container.querySelector('.media-play-trigger');
    let tokenReady = false;
    let loading = false;
    let exhausted = false;
    let playbackPosition = 0;
    let restoringPosition = false;
    let browserSpeechActive = false;
    const speechProgress = container.querySelector('.browser-speech-progress');
    const speechProgressBar = speechProgress?.querySelector('[role="progressbar"]');
    const speechProgressFill = speechProgress?.querySelector('.browser-speech-progress-fill');
    const speechElapsed = speechProgress?.querySelector('.browser-speech-elapsed');
    const speechDuration = speechProgress?.querySelector('.browser-speech-duration');
    let speechProgressTimer = null;
    let speechStartedAt = 0;
    let speechDurationSeconds = 0;
    let speechProgressSeconds = 0;
    const formatSpeechTime = (seconds) => {
      const safeSeconds = Math.max(0, Math.floor(Number(seconds) || 0));
      return `${Math.floor(safeSeconds / 60)}:${String(safeSeconds % 60).padStart(2, '0')}`;
    };
    const updateSpeechProgress = (seconds) => {
      if (!speechProgress || !speechProgressFill || !speechElapsed || !speechDuration) return;
      const boundedSeconds = Math.max(0, Math.min(speechDurationSeconds, Number(seconds) || 0));
      speechProgressSeconds = Math.max(speechProgressSeconds, boundedSeconds);
      const percent = speechDurationSeconds
        ? Math.round((speechProgressSeconds / speechDurationSeconds) * 100)
        : 0;
      speechProgressFill.style.width = `${percent}%`;
      speechProgressBar?.setAttribute('aria-valuenow', String(percent));
      speechElapsed.textContent = formatSpeechTime(speechProgressSeconds);
      speechDuration.textContent = formatSpeechTime(speechDurationSeconds);
    };
    const resetSpeechProgress = () => {
      if (speechProgressTimer !== null) window.clearInterval(speechProgressTimer);
      speechProgressTimer = null;
      speechProgressSeconds = 0;
      speechDurationSeconds = 0;
      speechStartedAt = 0;
      if (!speechProgress || !speechProgressFill || !speechElapsed || !speechDuration) return;
      speechProgress.hidden = true;
      speechProgressFill.style.width = '0%';
      speechProgressBar?.setAttribute('aria-valuenow', '0');
      speechElapsed.textContent = '0:00';
      speechDuration.textContent = '0:00';
    };
    const startSpeechProgress = (text) => {
      if (!speechProgress || !speechProgressFill || !speechElapsed || !speechDuration) return;
      const wordCount = text.trim().split(/\s+/).filter(Boolean).length;
      // SpeechSynthesis does not expose the actual duration, so estimate it
      // from the transcript and keep it in sync with boundary events when the
      // browser provides them.
      speechDurationSeconds = Math.max(2, Math.ceil(((wordCount / 150) * 60) / 0.9));
      speechStartedAt = Date.now();
      speechProgressSeconds = 0;
      speechProgress.hidden = false;
      updateSpeechProgress(0);
      speechProgressTimer = window.setInterval(() => {
        updateSpeechProgress((Date.now() - speechStartedAt) / 1000);
      }, 250);
    };
    const finishSpeechProgress = () => {
      updateSpeechProgress(speechDurationSeconds);
      if (speechProgressTimer !== null) window.clearInterval(speechProgressTimer);
      speechProgressTimer = null;
    };
    const showNextPlayback = () => {
      resetSpeechProgress();
      tokenReady = false;
      browserSpeechActive = false;
      const playCount = Number(player.dataset.playCount || 0);
      const maxPlayCount = Number(player.dataset.maxPlayCount || 0);
      exhausted = maxPlayCount > 0 && playCount >= maxPlayCount;
      playTrigger.hidden = exhausted;
      playTrigger.disabled = exhausted;
      if (exhausted) status.textContent = `Đã dùng hết ${maxPlayCount} lượt nghe.`;
      else status.textContent = `Bạn còn ${Math.max(0, maxPlayCount - playCount)} lượt nghe.`;
    };
    const speakWithBrowserVoice = (result) => {
      if (!window.speechSynthesis || typeof window.SpeechSynthesisUtterance !== 'function') {
        throw new Error('Trình duyệt không hỗ trợ giọng đọc Listening.');
      }
      const utterance = new window.SpeechSynthesisUtterance(result.speechText);
      utterance.lang = 'en-US';
      utterance.rate = 0.9;
      utterance.onboundary = (event) => {
        if (typeof event.charIndex !== 'number' || !result.speechText.length) return;
        updateSpeechProgress((event.charIndex / result.speechText.length) * speechDurationSeconds);
      };
      utterance.onend = () => {
        finishSpeechProgress();
        showNextPlayback();
      };
      utterance.onerror = () => {
        resetSpeechProgress();
        browserSpeechActive = false;
        tokenReady = false;
        playTrigger.hidden = false;
        status.textContent = 'Không thể phát giọng đọc. Hãy bấm phát lại.';
      };
      browserSpeechActive = true;
      tokenReady = true;
      player.controls = false;
      player.classList.add('browser-speech-placeholder');
      playTrigger.classList.add('browser-speech');
      playTrigger.setAttribute('aria-label', 'Nghe lại bài nghe');
      playTrigger.hidden = true;
      status.textContent = `Đã dùng ${result.playCount}/${result.maxPlayCount} lượt nghe`;
      startSpeechProgress(result.speechText);
      window.speechSynthesis.cancel();
      window.speechSynthesis.speak(utterance);
    };
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
            [csrfHeader]: csrf,
            'X-Requested-With': 'XMLHttpRequest',
            Accept: 'application/json',
          },
        });
        const contentType = response.headers.get('content-type') || '';
        const result = contentType.includes('application/json') ? await response.json() : {};
        if (!response.ok) {
          const message = result.message || '';
          if (message.toLowerCase().includes('hết số lượt nghe')) {
            exhausted = true;
            const maxPlayCount = Number(player.dataset.maxPlayCount || 2);
            playTrigger.hidden = true;
            playTrigger.disabled = true;
            status.textContent = maxPlayCount
              ? `Đã dùng hết ${maxPlayCount} lượt nghe.`
              : 'Đã dùng hết lượt nghe.';
            return;
          }
          throw new Error(message || 'Không thể phát audio');
        }
        player.dataset.playCount = result.playCount;
        player.dataset.maxPlayCount = result.maxPlayCount;
        if (result.browserSpeech) {
          if (!result.speechText?.trim()) throw new Error('Audio chưa có transcript để đọc.');
          speakWithBrowserVoice(result);
          return;
        }
        if (!result.contentToken) throw new Error('Máy chủ chưa cấp quyền phát audio');
        player.src = player.dataset.content + '?token=' + encodeURIComponent(result.contentToken);
        player.load();
        playbackPosition = 0;
        tokenReady = true;
        playTrigger.hidden = true;
        status.textContent = `Đã dùng ${result.playCount}/${result.maxPlayCount} lượt nghe`;
        try {
          await player.play();
        } catch (error) {
          // Browsers can block playback after the asynchronous token request.
          // The source is still loaded, so the native play control remains usable.
          status.textContent = 'Audio đã sẵn sàng. Bấm nút phát để nghe.';
        }
      } catch (error) {
        resetSpeechProgress();
        tokenReady = false;
        browserSpeechActive = false;
        player.controls = true;
        player.classList.remove('browser-speech-placeholder');
        playTrigger.hidden = false;
        status.textContent = error.message || 'Không thể phát audio';
      } finally {
        loading = false;
      }
    };
    playTrigger.addEventListener('click', requestPlayback);
    player.addEventListener('play', () => {
      if (!tokenReady && !browserSpeechActive) {
        player.pause();
        requestPlayback();
      }
    });
    player.addEventListener('timeupdate', () => {
      if (!player.seeking && !restoringPosition) playbackPosition = player.currentTime;
    });
    player.addEventListener('seeking', () => {
      if (restoringPosition || Math.abs(player.currentTime - playbackPosition) < 0.25) return;
      restoringPosition = true;
      player.currentTime = playbackPosition;
      status.textContent = 'Bài nghe không cho phép tua. Audio sẽ tiếp tục từ vị trí hiện tại.';
      window.setTimeout(() => {
        restoringPosition = false;
      }, 0);
    });
    player.addEventListener('error', () => {
      if (loading || browserSpeechActive) return;
      tokenReady = false;
      playTrigger.hidden = false;
      status.textContent = 'Không tải được audio. Hãy bấm phát lại.';
    });
    player.addEventListener('ended', showNextPlayback);
  });
  document.querySelectorAll('.speaking-recorder').forEach((box) => {
    const start = box.querySelector('.recording-start');
    const stop = box.querySelector('.recording-stop');
    const reset = box.querySelector('.recording-reset');
    const saveRecording = box.querySelector('.recording-save');
    const preview = box.querySelector('.recording-preview');
    const status = box.querySelector('.recording-status');
    const meter = box.querySelector('.recording-meter');
    const progress = box.querySelector('.recording-progress');
    const timeLabel = box.querySelector('.recording-time');
    const maxSeconds = Math.max(1, Number(box.dataset.responseSeconds || 45));
    let recorder;
    let stream;
    let chunks = [];
    let recordingBlob;
    let previewUrl;
    let timerId;
    let recordingStartedAt;
    let elapsedSeconds = 0;
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
    const formatTime = (seconds) => {
      const safeSeconds = Math.max(0, Math.floor(seconds));
      return `${String(Math.floor(safeSeconds / 60)).padStart(2, '0')}:${String(
        safeSeconds % 60,
      ).padStart(2, '0')}`;
    };
    const updateTimer = () => {
      if (recordingStartedAt == null) return;
      elapsedSeconds = Math.min(maxSeconds, (performance.now() - recordingStartedAt) / 1000);
      progress.value = elapsedSeconds;
      timeLabel.textContent = `${formatTime(elapsedSeconds)} / ${formatTime(maxSeconds)}`;
      if (elapsedSeconds >= maxSeconds && recorder?.state === 'recording') {
        stopRecording();
      }
    };
    const startTimer = () => {
      box.dataset.state = 'recording';
      box.dataset.dirty = 'true';
      progress.max = maxSeconds;
      progress.value = 0;
      elapsedSeconds = 0;
      recordingStartedAt = performance.now();
      timeLabel.textContent = `00:00 / ${formatTime(maxSeconds)}`;
      clearInterval(timerId);
      timerId = setInterval(updateTimer, 200);
    };
    const stopTimer = () => {
      clearInterval(timerId);
      timerId = null;
      recordingStartedAt = null;
    };
    const stopRecording = () => {
      if (recorder?.state !== 'recording') return;
      stopTimer();
      recorder.stop();
      stop.disabled = true;
      start.disabled = false;
    };
    const clearPreview = () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      previewUrl = null;
      preview.removeAttribute('src');
      preview.classList.add('is-empty');
      preview.setAttribute('aria-hidden', 'true');
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
        clearPreview();
        recordingBlob = null;
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
          preview.classList.remove('is-empty');
          preview.setAttribute('aria-hidden', 'false');
          box.dataset.state = 'preview';
          reset.disabled = false;
          saveRecording.disabled = false;
          stream?.getTracks().forEach((track) => track.stop());
          stream = null;
          setStatus('Đã ghi xong. Nghe lại và lưu.');
        };
        recorder.onerror = () => {
          stopTimer();
          stream?.getTracks().forEach((track) => track.stop());
          stream = null;
          start.disabled = false;
          stop.disabled = true;
          setStatus('Không thể hoàn tất bản ghi. Vui lòng thử lại.');
        };
        recorder.start();
        stop.disabled = false;
        startTimer();
        setStatus('Đang ghi âm…');
      } catch (error) {
        stream?.getTracks().forEach((track) => track.stop());
        stream = null;
        start.disabled = false;
        reset.disabled = !recordingBlob;
        saveRecording.disabled = !recordingBlob;
        setStatus('Không thể dùng microphone. Hãy kiểm tra quyền của trình duyệt.');
      }
    });
    stop.addEventListener('click', () => {
      stopRecording();
    });
    reset.addEventListener('click', () => {
      recordingBlob = null;
      clearPreview();
      stopTimer();
      box.dataset.state = 'idle';
      box.dataset.dirty = 'false';
      progress.value = 0;
      timeLabel.textContent = `00:00 / ${formatTime(maxSeconds)}`;
      reset.disabled = true;
      saveRecording.disabled = true;
      start.disabled = false;
      setStatus('Đang bắt đầu bản ghi mới…');
      start.click();
    });
    saveRecording.addEventListener('click', async () => {
      if (!recordingBlob) return;
      saveRecording.disabled = true;
      reset.disabled = true;
      start.disabled = true;
      setStatus('Đang lưu bản ghi…');
      const data = new FormData();
      data.append('questionId', box.dataset.questionId);
      data.append('itemId', box.dataset.itemId);
      data.append('audioFile', recordingBlob, `speaking-${box.dataset.itemId}.${extension()}`);
      let upload;
      try {
        upload = fetch(box.dataset.audioAction, {
          method: 'POST',
          headers: { 'X-CSRF-TOKEN': csrf },
          body: data,
        });
        inFlight.add(upload);
        const response = await upload;
        const result = await response.json();
        if (!response.ok || !result.saved)
          throw new Error(result.message || 'Chưa lưu được bản ghi');
        box.dataset.dirty = 'false';
        box.dataset.state = 'saved';
        failedRecordings.delete(box);
        const item = box.closest('.question-item');
        item.dataset.saved = 'true';
        item.dispatchEvent(new Event('recording-saved'));
        box.querySelector('.recording-saved').textContent = 'Đã lưu bản ghi.';
        setStatus('Đã lưu bản ghi.');
        start.disabled = false;
        reset.disabled = false;
        updateExamNavigation();
      } catch (error) {
        failedRecordings.add(box);
        saveRecording.disabled = false;
        reset.disabled = false;
        start.disabled = false;
        setStatus(error.message || 'Chưa thể lưu bản ghi. Vui lòng thử lại.');
      } finally {
        inFlight.delete(upload);
      }
    });
  });
})();
