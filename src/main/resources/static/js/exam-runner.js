/* ==========================================================================
   AptisPrep Exam Runner (exam-runner.js)
   - 1 câu / màn hình với bảng Palette câu hỏi & Drawer trên mobile
   - Server-rendered Countdown Timer (remainingSeconds, mốc performance.now, không trôi giờ)
   - Autosave qua fetch POST /student/attempts/{id}/answers (debounce 800ms)
   - CSRF qua header X-CSRF-TOKEN và param _csrf
   - Header Accept: application/json, X-Requested-With; từ chối redirect/HTML
   - Submit: flush pending saves; nộp thủ công chặn khi lỗi; hết giờ retry 3 lần rồi nộp
   - ORDERING: gửi tuần tự (await sequential) từng item trong câu
   - LISTENING: không đếm client, bắt buộc server playsRemaining, khóa nút khi lỗi
   - SPEAKING: hiển thị thông báo chờ backend, không gửi placeholder giả
   - Phím tắt an toàn: Alt + ArrowLeft / Alt + ArrowRight (bỏ phím chữ đơn)
   - A11y: aria-label động cho palette, toast aria-live="polite", focus trap, Esc
   - Lưu cờ "xem lại" vào sessionStorage theo attemptId
   ========================================================================== */

(() => {
    'use strict';

    const container = document.querySelector('.exam-runner-container');
    if (!container) return;

    document.body.classList.add('js-enabled');

    const attemptId = container.dataset.attemptId;
    const submitForm = document.getElementById('exam-submit-form');
    const autoSaveStatus = document.getElementById('autosave-indicator');
    const timerElement = document.getElementById('exam-timer');
    const timerDisplay = document.getElementById('timer-display');

    // Lấy CSRF token từ meta hoặc input
    const getCsrfToken = () => {
        return document.querySelector('meta[name="_csrf"]')?.content 
            || document.querySelector('input[name="_csrf"]')?.value 
            || '';
    };

    // --------------------------------------------------------------------------
    // 1. Quản lý đồng hồ đếm ngược (Dùng remainingSeconds từ server render)
    // --------------------------------------------------------------------------
    const rawRemaining = container.dataset.remainingSeconds;
    let timerInterval = null;
    let isSubmitting = false;

    // Phân biệt: data-remaining-seconds rỗng / null => ẩn đồng hồ; = 0 => hết giờ, nộp ngay
    if (rawRemaining === undefined || rawRemaining === null || rawRemaining.trim() === '') {
        if (timerElement) timerElement.style.display = 'none';
    } else {
        const remainingSeconds = parseInt(rawRemaining, 10);
        if (isNaN(remainingSeconds)) {
            if (timerElement) timerElement.style.display = 'none';
        } else if (remainingSeconds <= 0) {
            // Đã hết giờ ngay từ lúc tải trang => thông báo và nộp ngay
            if (timerElement) {
                timerElement.style.display = 'inline-flex';
                if (timerDisplay) timerDisplay.textContent = '00:00';
                timerElement.classList.add('timer-danger');
            }
            autoSubmitOnTimeUp();
        } else {
            // remainingSeconds > 0: Dùng mốc kết thúc performance.now() để không bị trôi
            if (timerElement) timerElement.style.display = 'inline-flex';
            const endMark = performance.now() + remainingSeconds * 1000;
            let alert5mShown = false;
            let alert1mShown = false;

            const formatTime = (secs) => {
                const m = Math.floor(secs / 60);
                const s = secs % 60;
                return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
            };

            const updateTimer = () => {
                const diffMs = endMark - performance.now();
                const currentRemaining = Math.max(0, Math.ceil(diffMs / 1000));

                if (currentRemaining <= 0) {
                    if (timerDisplay) timerDisplay.textContent = '00:00';
                    timerElement.classList.add('timer-danger');
                    clearInterval(timerInterval);
                    autoSubmitOnTimeUp();
                    return;
                }

                if (timerDisplay) timerDisplay.textContent = formatTime(currentRemaining);

                if (currentRemaining <= 300 && !alert5mShown) {
                    alert5mShown = true;
                    timerElement.classList.add('timer-warning');
                    showNotificationToast('Thời gian làm bài còn lại 5 phút!');
                }
                if (currentRemaining <= 60 && !alert1mShown) {
                    alert1mShown = true;
                    timerElement.classList.remove('timer-warning');
                    timerElement.classList.add('timer-danger');
                    showNotificationToast('Thời gian làm bài chỉ còn 1 phút! Hãy chuẩn bị nộp bài.');
                }
            };

            updateTimer();
            timerInterval = setInterval(updateTimer, 500);
        }
    }

    // Luồng hết giờ: thử lại flush tối đa 3 lần cách nhau 1s, sau đó VẪN submit
    async function autoSubmitOnTimeUp() {
        if (isSubmitting) return;
        isSubmitting = true;

        showNotificationToast('Hết thời gian làm bài! Đang tự động lưu bài và nộp...');
        updateAutoSaveStatus('saving');

        let saved = false;
        for (let attempt = 1; attempt <= 3; attempt++) {
            saved = await flushPendingSaves();
            if (saved) break;
            if (attempt < 3) {
                await new Promise(resolve => setTimeout(resolve, 1000));
            }
        }

        if (!saved) {
            showNotificationToast('Một số câu có thể chưa lưu được do lỗi kết nối. Đang tiến hành nộp bài...');
        }

        if (submitForm) {
            submitForm.submit();
        }
    }

    // --------------------------------------------------------------------------
    // 2. Autosave từng câu bằng fetch (Debounced + Flush Queue)
    // --------------------------------------------------------------------------
    const pendingSaves = new Map(); // key -> { questionId, itemId, payload, timerId }
    const inFlightSaves = new Set();

    function trackSave(promise) {
        inFlightSaves.add(promise);
        promise.finally(() => inFlightSaves.delete(promise)).catch(() => {});
        return promise;
    }

    function triggerAutoSave(questionId, itemId, payload) {
        updateAutoSaveStatus('saving');
        const key = `${questionId}_${itemId || 0}`;

        const existing = pendingSaves.get(key);
        if (existing && existing.timerId) {
            clearTimeout(existing.timerId);
        }

        const timerId = setTimeout(() => {
            pendingSaves.delete(key);
            trackSave(executeSave(questionId, itemId, payload)).catch(() => {});
        }, 800);

        pendingSaves.set(key, { questionId, itemId, payload, timerId });
    }

    async function executeSave(questionId, itemId, payload) {
        const token = getCsrfToken();
        const formData = new URLSearchParams();
        formData.append('_csrf', token);
        formData.append('questionId', questionId);
        if (itemId) formData.append('itemId', itemId);

        if (payload.selectedOptionId !== undefined) formData.append('selectedOptionId', payload.selectedOptionId);
        if (payload.userPosition !== undefined) formData.append('userPosition', payload.userPosition);
        if (payload.textAnswer !== undefined) formData.append('textAnswer', payload.textAnswer);
        if (payload.audioUrl !== undefined) formData.append('audioUrl', payload.audioUrl);

        try {
            const res = await fetch(`/student/attempts/${attemptId}/answers`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Accept': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest',
                    'X-CSRF-TOKEN': token
                },
                body: formData.toString()
            });

            if (!res.ok || res.redirected) {
                throw new Error('Save error');
            }

            const contentType = res.headers.get('content-type') || '';
            if (contentType.includes('text/html')) {
                throw new Error('Unexpected HTML response');
            }

            updateAutoSaveStatus('saved');
            markQuestionAnswered(questionId, true);
            return true;
        } catch (err) {
            updateAutoSaveStatus('error');
            throw err;
        }
    }

    // Hủy debounce và gửi toàn bộ các thay đổi còn chờ
    async function flushPendingSaves() {
        if (pendingSaves.size === 0 && inFlightSaves.size === 0) return true;

        const entries = Array.from(pendingSaves.values());
        entries.forEach(entry => {
            if (entry.timerId) clearTimeout(entry.timerId);
        });
        pendingSaves.clear();

        try {
            entries.forEach(entry => {
                trackSave(executeSave(entry.questionId, entry.itemId, entry.payload));
            });
            await Promise.all(Array.from(inFlightSaves));
            return true;
        } catch (err) {
            return false;
        }
    }

    function updateAutoSaveStatus(state) {
        if (!autoSaveStatus) return;
        autoSaveStatus.className = 'autosave-indicator ' + state;
        if (state === 'saving') {
            autoSaveStatus.innerHTML = '<span class="status-spinner"></span> Đang lưu...';
        } else if (state === 'saved') {
            autoSaveStatus.innerHTML = '✓ Đã lưu bài';
        } else if (state === 'error') {
            autoSaveStatus.innerHTML = '⚠ Chưa thể lưu (Sẽ tự động thử lại)';
        }
    }

    // --------------------------------------------------------------------------
    // 3. Quản lý trạng thái câu hỏi & Cờ "xem lại" (sessionStorage)
    // --------------------------------------------------------------------------
    const questionCards = Array.from(document.querySelectorAll('.exam-question-card'));
    const totalQuestions = questionCards.length;
    let currentIndex = 0;

    const storageKey = `aptis_review_flags_${attemptId}`;
    const reviewFlags = new Set();
    try {
        const saved = JSON.parse(sessionStorage.getItem(storageKey) || '[]');
        if (Array.isArray(saved)) {
            saved.forEach(id => reviewFlags.add(String(id)));
        }
    } catch (e) {}

    function showQuestion(index) {
        if (index < 0 || index >= totalQuestions) return;
        currentIndex = index;

        questionCards.forEach((card, idx) => {
            const isCurrent = idx === index;
            card.classList.toggle('active', isCurrent);
            card.hidden = !isCurrent;
        });

        // Nút Trước / Sau
        const prevBtn = document.getElementById('exam-btn-prev');
        const nextBtn = document.getElementById('exam-btn-next');
        if (prevBtn) prevBtn.disabled = (index === 0);
        if (nextBtn) {
            nextBtn.textContent = (index === totalQuestions - 1) ? 'Xem lại & Nộp bài' : 'Câu tiếp theo →';
        }

        // Nút Đánh dấu xem lại
        const currentCard = questionCards[index];
        const qId = currentCard?.dataset.questionId;
        const reviewBtn = document.getElementById('exam-btn-review');
        if (reviewBtn && qId) {
            const isFlagged = reviewFlags.has(String(qId));
            reviewBtn.classList.toggle('flagged', isFlagged);
            reviewBtn.setAttribute('aria-pressed', String(isFlagged));
            reviewBtn.textContent = isFlagged ? '★ Bỏ đánh dấu xem lại' : '☆ Đánh dấu xem lại';
        }

        // Screen reader accessibility focus
        const currentHeading = currentCard?.querySelector('h2, .question-prompt');
        if (currentHeading) {
            currentHeading.setAttribute('tabindex', '-1');
            currentHeading.focus();
        }

        updateAllPaletteAria();
    }

    function updatePaletteItemAria(btn, index) {
        if (!btn) return;
        const qPos = btn.textContent.trim();
        const isCurrent = (index === currentIndex);
        const isAnswered = btn.classList.contains('answered');
        const qId = btn.dataset.questionId;
        const isFlagged = reviewFlags.has(String(qId));

        let statusText = isAnswered ? 'Đã làm' : 'Chưa làm';
        if (isFlagged) statusText += ', Đánh dấu xem lại';
        if (isCurrent) statusText += ' (Câu hiện tại)';

        btn.setAttribute('aria-label', `Câu ${qPos}: ${statusText}`);
        btn.setAttribute('aria-current', isCurrent ? 'true' : 'false');
        btn.classList.toggle('current', isCurrent);
        btn.classList.toggle('flagged', isFlagged);
    }

    function updateAllPaletteAria() {
        const paletteBtns = document.querySelectorAll('.palette-btn');
        paletteBtns.forEach((btn, idx) => {
            updatePaletteItemAria(btn, idx);
        });
    }

    function markQuestionAnswered(questionId, isAnswered) {
        const paletteBtn = document.querySelector(`.palette-btn[data-question-id="${questionId}"]`);
        if (paletteBtn) {
            paletteBtn.classList.toggle('answered', isAnswered);
            const idx = Array.from(document.querySelectorAll('.palette-btn')).indexOf(paletteBtn);
            updatePaletteItemAria(paletteBtn, idx);
        }
    }

    function toggleReviewFlag(questionId) {
        const qStr = String(questionId);
        if (reviewFlags.has(qStr)) {
            reviewFlags.delete(qStr);
        } else {
            reviewFlags.add(qStr);
        }

        try {
            sessionStorage.setItem(storageKey, JSON.stringify(Array.from(reviewFlags)));
        } catch (e) {}

        const currentCard = questionCards[currentIndex];
        const currentQId = currentCard?.dataset.questionId;
        const reviewBtn = document.getElementById('exam-btn-review');
        if (reviewBtn && currentQId === qStr) {
            const isFlagged = reviewFlags.has(qStr);
            reviewBtn.classList.toggle('flagged', isFlagged);
            reviewBtn.setAttribute('aria-pressed', String(isFlagged));
            reviewBtn.textContent = isFlagged ? '★ Bỏ đánh dấu xem lại' : '☆ Đánh dấu xem lại';
        }

        updateAllPaletteAria();
    }

    // Gắn sự kiện nút Trước / Sau / Đánh dấu
    document.getElementById('exam-btn-prev')?.addEventListener('click', () => showQuestion(currentIndex - 1));
    document.getElementById('exam-btn-next')?.addEventListener('click', () => {
        if (currentIndex === totalQuestions - 1) {
            openSubmitModal();
        } else {
            showQuestion(currentIndex + 1);
        }
    });

    document.getElementById('exam-btn-review')?.addEventListener('click', () => {
        const qId = questionCards[currentIndex]?.dataset.questionId;
        if (qId) toggleReviewFlag(qId);
    });

    // Click ô trong Palette
    document.querySelectorAll('.palette-btn').forEach((btn, idx) => {
        btn.addEventListener('click', () => {
            showQuestion(idx);
            closeMobilePaletteDrawer();
        });
    });

    // --------------------------------------------------------------------------
    // 4. Xử lý các dạng câu hỏi
    // --------------------------------------------------------------------------

    // A. SINGLE_CHOICE: Radio selection & Card highlight
    document.querySelectorAll('.choice-card-input').forEach(radio => {
        radio.addEventListener('change', (e) => {
            const questionId = e.target.closest('.exam-question-card').dataset.questionId;
            const itemId = e.target.dataset.itemId;
            const selectedOptionId = e.target.value;

            const group = e.target.closest('.choice-group') || e.target.closest('.question-item-body');
            group.querySelectorAll('.choice-card').forEach(c => c.classList.remove('selected'));
            e.target.closest('.choice-card')?.classList.add('selected');

            triggerAutoSave(questionId, itemId, { selectedOptionId });
        });
    });

    // B. ORDERING: Gửi tuần tự (await sequential) từng item trong câu mỗi lần đổi thứ tự
    document.querySelectorAll('.ordering-list').forEach(list => {
        const questionId = list.closest('.exam-question-card').dataset.questionId;

        const updateAllOrderingPositions = async () => {
            const items = Array.from(list.querySelectorAll('.ordering-item'));
            // Cập nhật giao diện vị trí ngay lập tức
            items.forEach((item, index) => {
                const pos = index + 1;
                const posInput = item.querySelector('input[data-field="userPosition"]');
                const posDisplay = item.querySelector('.ordering-position-number');
                if (posInput) posInput.value = pos;
                if (posDisplay) posDisplay.textContent = pos;
            });

            // Gửi tuần tự từng item tới server để tránh race condition
            updateAutoSaveStatus('saving');
            try {
                for (let i = 0; i < items.length; i++) {
                    const item = items[i];
                    const pos = i + 1;
                    const itemId = item.dataset.itemId;
                    await trackSave(executeSave(questionId, itemId, { userPosition: pos }));
                }
                updateAutoSaveStatus('saved');
                markQuestionAnswered(questionId, true);
            } catch (err) {
                updateAutoSaveStatus('error');
            }
        };

        list.addEventListener('click', (e) => {
            const target = e.target.closest('button');
            if (!target) return;
            const item = target.closest('.ordering-item');
            if (!item) return;

            if (target.classList.contains('btn-order-up')) {
                const prev = item.previousElementSibling;
                if (prev) {
                    list.insertBefore(item, prev);
                    updateAllOrderingPositions();
                }
            } else if (target.classList.contains('btn-order-down')) {
                const next = item.nextElementSibling;
                if (next) {
                    list.insertBefore(next, item);
                    updateAllOrderingPositions();
                }
            }
        });

        // HTML5 Drag & Drop
        let draggedItem = null;
        list.querySelectorAll('.ordering-item').forEach(item => {
            item.setAttribute('draggable', 'true');
            item.addEventListener('dragstart', () => {
                draggedItem = item;
                item.classList.add('dragging');
            });
            item.addEventListener('dragend', () => {
                item.classList.remove('dragging');
                draggedItem = null;
                updateAllOrderingPositions();
            });
        });

        list.addEventListener('dragover', (e) => {
            e.preventDefault();
            const afterElement = getDragAfterElement(list, e.clientY);
            if (!draggedItem) return;
            if (afterElement == null) {
                list.appendChild(draggedItem);
            } else {
                list.insertBefore(draggedItem, afterElement);
            }
        });
    });

    function getDragAfterElement(container, y) {
        const draggableElements = [...container.querySelectorAll('.ordering-item:not(.dragging)')];
        return draggableElements.reduce((closest, child) => {
            const box = child.getBoundingClientRect();
            const offset = y - box.top - box.height / 2;
            if (offset < 0 && offset > closest.offset) {
                return { offset: offset, element: child };
            } else {
                return closest;
            }
        }, { offset: Number.NEGATIVE_INFINITY }).element;
    }

    // C. MATCHING: Ghép cặp dropdown
    document.querySelectorAll('.matching-select').forEach(select => {
        select.addEventListener('change', (e) => {
            const questionId = e.target.closest('.exam-question-card').dataset.questionId;
            const itemId = e.target.dataset.itemId;
            const selectedOptionId = e.target.value;
            triggerAutoSave(questionId, itemId, { selectedOptionId });
        });
    });

    // D. WRITING_TEXT: Textarea đếm từ trực tiếp
    document.querySelectorAll('.writing-textarea').forEach(area => {
        const counter = area.parentElement.querySelector('.writing-word-counter');
        const questionId = area.closest('.exam-question-card').dataset.questionId;
        const itemId = area.dataset.itemId;

        const countWords = () => {
            const text = area.value.trim();
            const count = text ? text.split(/\s+/).length : 0;
            if (counter) counter.textContent = `${count} từ`;
            return count;
        };

        area.addEventListener('input', () => {
            countWords();
            triggerAutoSave(questionId, itemId, { textAnswer: area.value });
        });
        countWords();
    });

    // E. LISTENING: Bỏ mặc định '2' và playsRemaining-- client; chỉ nhận server
    document.querySelectorAll('.exam-audio-player').forEach(player => {
        const audio = player.querySelector('audio');
        const playBtn = player.querySelector('.btn-audio-play');
        const playCountBadge = player.querySelector('.audio-play-count');
        const questionId = player.dataset.questionId;

        const rawRemaining = player.dataset.playsRemaining;
        const rawMaxPlays = player.dataset.maxPlays;

        // Bỏ mặc định '2', không tự gán số
        let playsRemaining = (rawRemaining !== undefined && rawRemaining !== '') ? parseInt(rawRemaining, 10) : null;
        const maxPlays = (rawMaxPlays !== undefined && rawMaxPlays !== '') ? parseInt(rawMaxPlays, 10) : null;

        const updateDisplay = () => {
            if (playsRemaining === null || isNaN(playsRemaining)) {
                if (playCountBadge) playCountBadge.textContent = 'Chưa có thông tin lượt nghe';
                if (playBtn) {
                    playBtn.disabled = true;
                    playBtn.classList.add('disabled');
                    playBtn.textContent = 'Không khả dụng';
                }
                return;
            }

            if (playCountBadge) {
                playCountBadge.textContent = maxPlays ? `Lượt nghe còn lại: ${playsRemaining}/${maxPlays}` : `Lượt nghe còn lại: ${playsRemaining}`;
            }

            if (playsRemaining <= 0 && playBtn) {
                playBtn.disabled = true;
                playBtn.classList.add('disabled');
                playBtn.textContent = 'Đã hết lượt nghe';
            }
        };

        // Nếu thiếu data-plays-remaining -> Khóa nút phát và hiện thông báo
        if (playsRemaining === null || isNaN(playsRemaining)) {
            updateDisplay();
            return;
        }

        updateDisplay();

        if (playBtn && audio && questionId) {
            playBtn.addEventListener('click', async () => {
                if (playsRemaining === null || playsRemaining <= 0) return;

                // Tạm dừng (không tính lượt)
                if (!audio.paused) {
                    audio.pause();
                    playBtn.textContent = '► Tiếp tục nghe';
                    return;
                }

                // Tiếp tục phát sau khi tạm dừng (không tính lượt mới)
                if (audio.currentTime > 0) {
                    try {
                        await audio.play();
                        playBtn.textContent = '❚❚ Tạm dừng';
                    } catch (e) {
                        showNotificationToast('Không thể phát âm thanh: ' + e.message);
                    }
                    return;
                }

                // CHỈ gọi server tính lượt khi bắt đầu phát mới (audio.currentTime === 0)
                try {
                    playBtn.disabled = true;
                    playBtn.textContent = 'Đang xác thực lượt nghe...';

                    const token = getCsrfToken();
                    const res = await fetch(`/student/attempts/${attemptId}/audio-played`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded',
                            'Accept': 'application/json',
                            'X-Requested-With': 'XMLHttpRequest',
                            'X-CSRF-TOKEN': token
                        },
                        body: `_csrf=${encodeURIComponent(token)}&questionId=${encodeURIComponent(questionId)}`
                    });

                    if (!res.ok || res.redirected) {
                        throw new Error('Máy chủ từ chối hoặc chưa có endpoint ghi nhận lượt nghe');
                    }

                    const contentType = res.headers.get('content-type') || '';
                    if (contentType.includes('text/html')) {
                        throw new Error('Máy chủ phản hồi HTML không hợp lệ');
                    }

                    const data = await res.json().catch(() => null);
                    if (!data || data.playsRemaining === undefined) {
                        throw new Error('Phản hồi từ máy chủ không chứa dữ liệu playsRemaining');
                    }

                    // Cập nhật số lượt nghe duy nhất từ server, KHÔNG tự giảm bằng playsRemaining--
                    playsRemaining = parseInt(data.playsRemaining, 10);
                    player.dataset.playsRemaining = playsRemaining;
                    updateDisplay();

                    if (playsRemaining >= 0) {
                        playBtn.disabled = false;
                        await audio.play();
                        playBtn.textContent = '❚❚ Tạm dừng';
                    }
                } catch (err) {
                    // Nếu lỗi endpoint hoặc dữ liệu sai -> Khóa nút phát và hiện thông báo, KHÔNG tự nới
                    playBtn.disabled = true;
                    playBtn.classList.add('disabled');
                    playBtn.textContent = 'Lỗi lượt nghe';
                    showNotificationToast('Không thể phát âm thanh: ' + err.message);
                }
            });

            audio.addEventListener('ended', () => {
                audio.currentTime = 0;
                if (playsRemaining !== null && playsRemaining > 0) {
                    playBtn.textContent = `► Nghe lượt tiếp theo (Còn ${playsRemaining} lượt)`;
                } else {
                    updateDisplay();
                }
            });
        }
    });

    // --------------------------------------------------------------------------
    // 5. Phím tắt điều hướng: Alt + ArrowLeft / Alt + ArrowRight (Bỏ phím chữ đơn)
    // --------------------------------------------------------------------------
    document.addEventListener('keydown', (e) => {
        // Phím tắt tổ hợp Alt + Mũi tên an toàn, không xung đột khi focus ở form control
        if (e.altKey && e.key === 'ArrowRight') {
            e.preventDefault();
            if (currentIndex < totalQuestions - 1) showQuestion(currentIndex + 1);
        } else if (e.altKey && e.key === 'ArrowLeft') {
            e.preventDefault();
            if (currentIndex > 0) showQuestion(currentIndex - 1);
        }
    });

    // --------------------------------------------------------------------------
    // 6. Modal xác nhận nộp bài (Focus trap, Esc, Chờ lưu trước khi submit)
    // --------------------------------------------------------------------------
    const submitModal = document.getElementById('exam-submit-modal');
    const openModalBtn = document.getElementById('btn-open-submit-modal');
    const cancelModalBtn = document.getElementById('btn-modal-cancel');
    const confirmModalBtn = document.getElementById('btn-modal-confirm');
    let lastActiveElementBeforeModal = null;

    function openSubmitModal() {
        if (!submitModal) return;
        lastActiveElementBeforeModal = document.activeElement;

        let answeredCount = 0;
        questionCards.forEach(card => {
            const qId = card.dataset.questionId;
            const paletteBtn = document.querySelector(`.palette-btn[data-question-id="${qId}"]`);
            if (paletteBtn && paletteBtn.classList.contains('answered')) {
                answeredCount++;
            }
        });

        const unAnsweredCount = totalQuestions - answeredCount;
        const flaggedCount = reviewFlags.size;

        const unansweredStat = document.getElementById('modal-unanswered-count');
        const flaggedStat = document.getElementById('modal-flagged-count');
        if (unansweredStat) unansweredStat.textContent = String(unAnsweredCount);
        if (flaggedStat) flaggedStat.textContent = String(flaggedCount);

        submitModal.classList.add('is-open');
        confirmModalBtn?.focus();
    }

    function closeSubmitModal() {
        if (!submitModal) return;
        submitModal.classList.remove('is-open');
        if (lastActiveElementBeforeModal && typeof lastActiveElementBeforeModal.focus === 'function') {
            lastActiveElementBeforeModal.focus();
        }
    }

    // Nộp bài thủ công (bấm nút nộp): Chỉ submit khi lưu thành công, chặn lại nếu lỗi
    async function handleManualSubmit() {
        if (isSubmitting) return;
        updateAutoSaveStatus('saving');
        if (confirmModalBtn) confirmModalBtn.disabled = true;

        try {
            const saveSuccess = await flushPendingSaves();
            if (!saveSuccess) {
                throw new Error('Chưa thể lưu tất cả các câu trả lời do sự cố kết nối');
            }

            isSubmitting = true;
            if (submitForm) {
                submitForm.submit();
            }
        } catch (err) {
            updateAutoSaveStatus('error');
            if (confirmModalBtn) confirmModalBtn.disabled = false;
            showNotificationToast('Không thể nộp bài: Chưa thể lưu câu trả lời mới nhất. Vui lòng kiểm tra kết nối mạng và thử lại.');
            closeSubmitModal();
        }
    }

    openModalBtn?.addEventListener('click', openSubmitModal);
    cancelModalBtn?.addEventListener('click', closeSubmitModal);
    confirmModalBtn?.addEventListener('click', handleManualSubmit);

    submitModal?.addEventListener('click', (e) => {
        if (e.target === submitModal) closeSubmitModal();
    });

    // Focus trap cho modal
    submitModal?.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            e.preventDefault();
            closeSubmitModal();
            return;
        }

        if (e.key === 'Tab') {
            const focusables = submitModal.querySelectorAll('button:not([disabled]), [tabindex]:not([tabindex="-1"])');
            if (focusables.length === 0) return;
            const first = focusables[0];
            const last = focusables[focusables.length - 1];

            if (e.shiftKey && document.activeElement === first) {
                e.preventDefault();
                last.focus();
            } else if (!e.shiftKey && document.activeElement === last) {
                e.preventDefault();
                first.focus();
            }
        }
    });

    // --------------------------------------------------------------------------
    // 7. Palette Drawer trên Mobile (Focus trap, Esc, Trả focus)
    // --------------------------------------------------------------------------
    const paletteToggleBtn = document.getElementById('btn-toggle-palette-drawer');
    const paletteDrawer = document.getElementById('exam-palette-drawer');
    const closeDrawerBtn = document.getElementById('btn-close-palette-drawer');

    function openMobilePaletteDrawer() {
        if (paletteDrawer) {
            paletteDrawer.classList.add('is-open');
            paletteToggleBtn?.setAttribute('aria-expanded', 'true');
            closeDrawerBtn?.focus();
        }
    }

    function closeMobilePaletteDrawer() {
        if (paletteDrawer && paletteDrawer.classList.contains('is-open')) {
            paletteDrawer.classList.remove('is-open');
            paletteToggleBtn?.setAttribute('aria-expanded', 'false');
            paletteToggleBtn?.focus();
        }
    }

    paletteToggleBtn?.addEventListener('click', () => {
        if (paletteDrawer?.classList.contains('is-open')) {
            closeMobilePaletteDrawer();
        } else {
            openMobilePaletteDrawer();
        }
    });

    closeDrawerBtn?.addEventListener('click', closeMobilePaletteDrawer);

    paletteDrawer?.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            e.preventDefault();
            closeMobilePaletteDrawer();
            return;
        }

        if (e.key === 'Tab') {
            const focusables = paletteDrawer.querySelectorAll('button:not([disabled]), [tabindex]:not([tabindex="-1"])');
            if (focusables.length === 0) return;
            const first = focusables[0];
            const last = focusables[focusables.length - 1];

            if (e.shiftKey && document.activeElement === first) {
                e.preventDefault();
                last.focus();
            } else if (!e.shiftKey && document.activeElement === last) {
                e.preventDefault();
                first.focus();
            }
        }
    });

    // --------------------------------------------------------------------------
    // 8. Thông báo Toast nổi hỗ trợ A11Y
    // --------------------------------------------------------------------------
    function showNotificationToast(msg) {
        const toast = document.createElement('div');
        toast.className = 'exam-toast-notification';
        toast.setAttribute('role', 'status');
        toast.setAttribute('aria-live', 'polite');
        toast.textContent = msg;
        document.body.appendChild(toast);
        setTimeout(() => toast.classList.add('visible'), 50);
        setTimeout(() => {
            toast.classList.remove('visible');
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    }

    // Khởi tạo câu đầu tiên và đồng bộ A11Y
    showQuestion(0);

})();
