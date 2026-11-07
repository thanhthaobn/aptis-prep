const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { runInNewContext } = require('node:vm');
const source = readFileSync(
  resolve(__dirname, '../../main/resources/static/js/account-forms.js'),
  'utf8',
);

function registration(invalidField) {
  const fields = {};
  for (const name of ['fullName', 'email', 'password', 'confirmPassword']) {
    const events = {},
      attributes = {};
    const field = (fields[name] = {
      id: name,
      name,
      value: '',
      type: name.includes('assword') ? 'password' : 'text',
      message: '',
      addEventListener: (name, fn) => {
        events[name] = fn;
      },
      setCustomValidity(message) {
        this.message = message;
      },
      setAttribute: (name, v) => {
        attributes[name] = v;
      },
      getAttribute: (name) => attributes[name],
      focus() {
        this.focused = true;
      },
      get validity() {
        return { valid: !this.message };
      },
      get validationMessage() {
        return this.message;
      },
      input(v) {
        this.value = v;
        events.input();
      },
      blur() {
        events.blur();
      },
    });
    if (name === invalidField) attributes['aria-invalid'] = 'true';
  }
  const errors = Object.fromEntries(
    Object.keys(fields).map((k) => [`${k}-error`, { textContent: '' }]),
  );
  let toggle;
  const button = {
    dataset: { togglePassword: 'password' },
    textContent: '',
    addEventListener: (name, fn) => {
      toggle = fn;
    },
    setAttribute(name, v) {
      this[name] = v;
    },
  };
  const form = {
    querySelectorAll: () => Object.values(fields),
    addEventListener() {},
    querySelector: (s) =>
      s === '[data-password-policy]'
        ? fields.password
        : s === '[data-password-confirm]'
          ? fields.confirmPassword
          : s === '[aria-invalid="true"]'
            ? Object.values(fields).find((f) => f.getAttribute('aria-invalid') === 'true')
            : null,
  };
  runInNewContext(source, {
    TextEncoder,
    document: {
      querySelectorAll: (s) => (s === '[data-toggle-password]' ? [button] : [form]),
      getElementById: (id) => fields[id] || errors[id],
    },
  });
  return { ...fields, errors, button, toggle: () => toggle() };
}

test('registration normalizes Unicode names and email on blur without changing passwords', () => {
  const r = registration();
  r.fullName.input('  Thảo\u00a0 Nguyễn  ');
  r.fullName.blur();
  r.email.input('  THAO@EXAMPLE.COM  ');
  r.email.blur();
  r.password.input(' spaced-passphrase ');
  r.password.blur();
  assert.equal(r.fullName.value, 'Thảo Nguyễn');
  assert.equal(r.email.value, 'thao@example.com');
  assert.equal(r.password.value, ' spaced-passphrase ');
});

test('confirmation mismatch updates immediately when the password changes and can be corrected', () => {
  const r = registration();
  r.password.input('Learning-English-2026');
  r.confirmPassword.input('Learning-English-2026');
  r.confirmPassword.blur();
  assert.equal(r.confirmPassword.message, '');
  r.password.input('Learning-English-2027');
  assert.match(r.errors['confirmPassword-error'].textContent, /không khớp/);
  r.confirmPassword.input('Learning-English-2027');
  assert.equal(r.confirmPassword.message, '');
});

test('registration rejects common passwords and UTF-8 values beyond BCrypt limit', () => {
  const r = registration();
  r.password.input('password123');
  r.password.blur();
  assert.notEqual(r.password.message, '');
  r.password.input('đ'.repeat(40));
  assert.notEqual(r.password.message, '');
  r.password.input('A long learning passphrase');
  assert.equal(r.password.message, '');
});

test('password visibility toggles accessibly and server field errors receive focus', () => {
  const r = registration('email');
  assert.equal(r.email.focused, true);
  r.toggle();
  assert.equal(r.password.type, 'text');
  assert.equal(r.button['aria-pressed'], 'true');
  r.toggle();
  assert.equal(r.password.type, 'password');
});
