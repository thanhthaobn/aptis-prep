// Shared confirmations stay inside the website and never insert message text as HTML.
(() => {
  let dialog, heading, description, accept, cancel, resolveConfirmation, previousFocus;

  function finish(confirmed) {
    if (!resolveConfirmation) return;
    const resolve = resolveConfirmation;
    resolveConfirmation = null;
    dialog.close();
    if (previousFocus?.isConnected) previousFocus.focus();
    resolve(confirmed);
  }

  function createDialog() {
    dialog = document.createElement('dialog');
    dialog.className = 'app-confirm-dialog';
    dialog.setAttribute('aria-labelledby', 'app-confirm-title');
    dialog.setAttribute('aria-describedby', 'app-confirm-message');
    heading = document.createElement('h2');
    heading.id = 'app-confirm-title';
    description = document.createElement('p');
    description.id = 'app-confirm-message';
    const actions = document.createElement('div');
    actions.className = 'modal-actions';
    cancel = document.createElement('button');
    cancel.type = 'button';
    cancel.className = 'button button-secondary';
    cancel.autofocus = true;
    accept = document.createElement('button');
    accept.type = 'button';
    accept.className = 'button';
    actions.append(cancel, accept);
    dialog.append(heading, description, actions);
    document.body.append(dialog);
    cancel.addEventListener('click', () => finish(false));
    accept.addEventListener('click', () => finish(true));
    dialog.addEventListener('cancel', (event) => {
      event.preventDefault();
      finish(false);
    });
  }

  function confirm({
    title = 'Xác nhận thao tác',
    message,
    confirmText = 'Xác nhận',
    cancelText = 'Hủy',
    danger = false,
  }) {
    if (resolveConfirmation) return Promise.resolve(false);
    if (!dialog) createDialog();
    previousFocus = document.activeElement;
    heading.textContent = title;
    description.textContent = message;
    accept.textContent = confirmText;
    cancel.textContent = cancelText;
    accept.classList.toggle('is-destructive', danger);
    return new Promise((resolve) => {
      resolveConfirmation = resolve;
      dialog.showModal();
      cancel.focus();
    });
  }

  // Covers links and navigation forms (including logout) without changing normal save forms.
  function guardUnsaved(form, { isDirty, onDiscard, message }) {
    let asking = false;
    const canLeave = async () => {
      if (asking) return false;
      asking = true;
      try {
        const leave = await confirm({
          title: 'Thay đổi chưa được lưu',
          message,
          confirmText: 'Rời trang',
          cancelText: 'Tiếp tục chỉnh sửa',
          danger: true,
        });
        if (leave) onDiscard();
        return leave;
      } finally {
        asking = false;
      }
    };
    document.addEventListener('click', async (event) => {
      if (
        event.defaultPrevented ||
        !isDirty() ||
        event.button !== 0 ||
        event.ctrlKey ||
        event.metaKey ||
        event.shiftKey ||
        event.altKey
      )
        return;
      const link = event.target.closest('a[href]');
      if (!link || link.hasAttribute('download') || (link.target && link.target !== '_self'))
        return;
      const url = new URL(link.href, window.location.href);
      if (!['http:', 'https:'].includes(url.protocol)) return;
      const current = new URL(window.location.href);
      if (
        url.origin === current.origin &&
        url.pathname === current.pathname &&
        url.search === current.search
      )
        return;
      event.preventDefault();
      if (await canLeave()) window.location.assign(url.href);
    });
    document.addEventListener(
      'submit',
      async (event) => {
        const target = event.target;
        if (target === form || event.defaultPrevented || !isDirty()) return;
        event.preventDefault();
        if (await canLeave()) {
          target.requestSubmit(event.submitter || undefined);
        }
      },
      true,
    );
  }

  window.AppDialog = { confirm, dismiss: () => finish(false), guardUnsaved };
  const approvedForms = new WeakSet(),
    pendingForms = new WeakSet();
  document.addEventListener('submit', async (event) => {
    const form = event.target;
    if (!form.matches('form[data-confirm-message]') || event.defaultPrevented) return;
    if (approvedForms.delete(form)) return;
    event.preventDefault();
    if (pendingForms.has(form)) return;
    pendingForms.add(form);
    try {
      if (
        await confirm({
          title: form.dataset.confirmTitle,
          message: form.dataset.confirmMessage,
          confirmText: form.dataset.confirmButton,
        })
      ) {
        approvedForms.add(form);
        form.requestSubmit(event.submitter || undefined);
        approvedForms.delete(form);
      }
    } finally {
      pendingForms.delete(form);
    }
  });
})();
