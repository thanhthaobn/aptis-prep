(() => {
  const common = new Set([
    'password',
    'password1',
    'password123',
    '12345678',
    '123456789',
    '1234567890',
    'qwerty123',
    'qwertyuiop',
    'abcdefgh',
    '87654321',
    '11111111',
    'admin123',
    'aptis123',
    'letmein123',
  ]);
  document.querySelectorAll('[data-toggle-password]').forEach((button) => {
    button.addEventListener('click', () => {
      const field = document.getElementById(button.dataset.togglePassword);
      const showing = field.type === 'password';
      field.type = showing ? 'text' : 'password';
      button.setAttribute('aria-pressed', String(showing));
      button.textContent = showing ? 'Ẩn' : 'Hiện';
      button.setAttribute('aria-label', showing ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
    });
  });
  document.querySelectorAll('[data-account-form]').forEach((form) => {
    const password = form.querySelector('[data-password-policy]');
    const confirm = form.querySelector('[data-password-confirm]');
    const validate = (field, show) => {
      let message = '';
      if (field === password && field.value) {
        const v = field.value;
        if (
          v.length < 8 ||
          v.length > 72 ||
          new TextEncoder().encode(v).length > 72 ||
          common.has(v.toLowerCase()) ||
          new Set(v).size < 2 ||
          /[\u0000-\u001f\u007f-\u009f]/u.test(v)
        )
          message =
            v.length < 8
              ? 'Mật khẩu cần ít nhất 8 ký tự.'
              : new TextEncoder().encode(v).length > 72
                ? 'Mật khẩu quá dài. Hãy rút ngắn một chút.'
                : 'Mật khẩu quá dễ đoán. Hãy dùng cụm từ khác.';
      }
      if (field === confirm && field.value && field.value !== password?.value)
        message = 'Mật khẩu xác nhận không khớp';
      if (
        field.name === 'fullName' &&
        field.value &&
        !/^(?=.*\p{L})[\p{L}\p{M} .’'\-]+$/u.test(field.value)
      )
        message = 'Họ tên không hợp lệ. Vui lòng dùng chữ.';
      field.setCustomValidity(message);
      if (show) {
        const error = document.getElementById(`${field.id}-error`);
        if (error)
          error.textContent = message || (field.validity.valid ? '' : field.validationMessage);
        field.setAttribute('aria-invalid', String(!field.validity.valid));
      }
    };
    form.querySelectorAll('input:not([type="hidden"])').forEach((field) => {
      field.addEventListener('blur', () => {
        if (field.name === 'email') field.value = field.value.trim().toLowerCase();
        if (field.name === 'fullName' && !/[\u0000-\u001f\u007f-\u009f]/u.test(field.value))
          field.value = field.value
            .normalize('NFC')
            .replace(/\p{Zs}+/gu, ' ')
            .trim();
        validate(field, true);
      });
      field.addEventListener('input', () => {
        validate(field, field.getAttribute('aria-invalid') === 'true');
        if (field === password && confirm?.value) validate(confirm, true);
      });
    });
    form.addEventListener('submit', (event) => {
      form.querySelectorAll('input:not([type="hidden"])').forEach((field) => validate(field, true));
      if (!form.checkValidity()) {
        event.preventDefault();
        form.querySelector(':invalid')?.focus();
      }
    });
    const invalid = form.querySelector('[aria-invalid="true"]');
    if (invalid) invalid.focus();
    else form.querySelector('[data-form-errors]')?.focus();
  });
})();
