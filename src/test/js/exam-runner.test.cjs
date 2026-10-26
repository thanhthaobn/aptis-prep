const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { runInNewContext } = require('node:vm');
const source = readFileSync(
  resolve(__dirname, '../../main/resources/static/js/exam-runner.js'),
  'utf8',
);
const tick = () => new Promise((resolve) => setImmediate(resolve));

function exam() {
  const events = {},
    requests = [],
    confirmations = [],
    timers = new Map();
  let interval,
    clock = 0,
    submissions = 0,
    dismissed = 0,
    nextTimer = 0;
  const status = { textContent: '' };
  const error = {
    hidden: true,
    focus() {
      this.focused = true;
    },
  };
  const button = { disabled: false, textContent: 'Nộp bài' };
  const answer = {
    dataset: { autosave: '/answers/autosave' },
    value: 'first answer',
    querySelector: () => status,
    addEventListener: (name, handler) => {
      events[name] = handler;
    },
  };
  const submit = {
    addEventListener: (name, handler) => {
      events.submitAttempt = handler;
    },
    setAttribute() {},
    removeAttribute() {},
    submit() {
      submissions++;
    },
  };
  const ids = {
    'submit-attempt-form': submit,
    'submit-attempt': button,
    'attempt-submit-error': error,
    'exam-timer': { dataset: { deadline: new Date(60000).toISOString() } },
    'timer-display': {},
  };
  runInNewContext(source, {
    URLSearchParams,
    FormData: class {
      constructor(form) {
        return [['textAnswer', form.value]];
      }
    },
    Date: { parse: Date.parse, now: () => clock },
    setTimeout(handler) {
      timers.set(++nextTimer, handler);
      return nextTimer;
    },
    clearTimeout(id) {
      timers.delete(id);
    },
    setInterval(handler) {
      interval = handler;
      return 1;
    },
    clearInterval() {},
    fetch: (url, request) =>
      new Promise((resolve) => requests.push({ url, body: request.body, resolve })),
    document: {
      querySelector: () => ({ content: 'csrf-token' }),
      querySelectorAll: (selector) => (selector === '.answer-form' ? [answer] : []),
      getElementById: (id) => ids[id] || null,
      body: { classList: { contains: () => false } },
    },
    window: {
      AppDialog: {
        confirm: (options) => new Promise((resolve) => confirmations.push({ options, resolve })),
        dismiss() {
          dismissed++;
          confirmations.at(-1)?.resolve(false);
        },
      },
    },
  });
  return {
    answer,
    status,
    error,
    button,
    requests,
    confirmations,
    submit: () => events.submitAttempt({ preventDefault() {} }),
    input(value) {
      answer.value = value;
      events.input();
    },
    expire() {
      clock = 60000;
      interval();
    },
    async confirm(value) {
      confirmations.at(-1).resolve(value);
      await tick();
    },
    async respond(ok) {
      requests.at(-1).resolve({ ok, json: async () => ({ saved: ok }) });
      await tick();
    },
    submissions: () => submissions,
    dismissed: () => dismissed,
  };
}

test('cancelling the in-page submit confirmation keeps the attempt editable', async () => {
  const page = exam();
  page.input('latest answer');
  page.submit();
  page.submit();
  assert.equal(page.confirmations.length, 1);
  await page.confirm(false);
  assert.equal(page.submissions(), 0);
  assert.equal(page.requests.length, 0);
  assert.equal(page.button.disabled, false);
});

test('confirmed submission saves the last answer before submitting exactly once', async () => {
  const page = exam();
  page.input('latest answer');
  page.submit();
  await page.confirm(true);
  assert.equal(page.submissions(), 0);
  assert.equal(page.requests[0].body.get('textAnswer'), 'latest answer');
  assert.equal(page.button.disabled, true);
  await page.respond(true);
  assert.equal(page.submissions(), 1);
  page.submit();
  assert.equal(page.submissions(), 1);
});

test('failed saves show an inline error and a subsequent submit retries the retained answer', async () => {
  const page = exam();
  page.input('do not lose this text');
  page.submit();
  await page.confirm(true);
  await page.respond(false);
  await page.respond(false);
  assert.equal(page.submissions(), 0);
  assert.equal(page.error.hidden, false);
  assert.match(page.error.textContent, /Bài chưa được nộp/);
  assert.equal(page.error.focused, true);
  assert.equal(page.button.disabled, false);
  assert.equal(page.answer.inert, false);
  assert.equal(page.answer.value, 'do not lose this text');
  page.submit();
  await page.confirm(true);
  assert.equal(page.requests.at(-1).body.get('textAnswer'), 'do not lose this text');
  await page.respond(true);
  assert.equal(page.submissions(), 1);
  assert.equal(page.error.hidden, true);
});

test('deadline dismisses an open confirmation and automatically submits once', async () => {
  const page = exam();
  page.input('answer at deadline');
  page.submit();
  page.expire();
  assert.equal(page.dismissed(), 1);
  await page.respond(true);
  assert.equal(page.submissions(), 1);
});

test('deadline during final saving still submits even if the connection fails', async () => {
  const page = exam();
  page.input('answer at deadline');
  page.submit();
  await page.confirm(true);
  page.expire();
  await page.respond(false);
  assert.equal(page.submissions(), 1);
  assert.equal(page.error.hidden, true);
});
