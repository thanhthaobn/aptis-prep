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

function exam({ answered = true } = {}) {
  const events = {},
    requests = [],
    confirmations = [],
    timers = new Map();
  let interval,
    clock = 0,
    submissions = 0,
    sectionAdvances = 0,
    dismissed = 0,
    exits = 0,
    nextTimer = 0;
  const status = { textContent: '' };
  const error = {
    hidden: true,
    focus() {
      this.focused = true;
    },
  };
  const button = { disabled: false, textContent: 'Nộp bài' };
  const automaticInput = { value: 'false' };
  const answer = {
    dataset: { autosave: '/answers/autosave' },
    value: 'first answer',
    querySelector: () => status,
    addEventListener: (name, handler) => {
      events[name] = handler;
    },
  };
  const item = {
    dataset: { itemId: '101', saved: String(answered) },
    addEventListener() {},
    contains: () => false,
    focus() {},
    querySelector: () => null,
    querySelectorAll: () => [],
  };
  const questionPage = {
    querySelectorAll: (selector) => (selector === '.question-item' ? [item] : []),
  };
  const submit = {
    addEventListener: (name, handler) => {
      events.submitAttempt = handler;
    },
    setAttribute() {},
    removeAttribute() {},
    querySelector: (selector) => (selector === 'input[name="automatic"]' ? automaticInput : null),
    submit() {
      submissions++;
    },
  };
  const exit = {
    addEventListener: (name, handler) => {
      if (name === 'submit') events.exitAttempt = handler;
    },
    submit() {
      exits++;
    },
  };
  const advance = {
    ...submit,
    addEventListener: (name, handler) => {
      events.advanceSection = handler;
    },
    submit() {
      sectionAdvances++;
    },
  };
  const ids = {
    'submit-attempt-form': submit,
    'submit-attempt': button,
    'attempt-submit-error': error,
    'exam-timer': { dataset: { deadline: new Date(60000).toISOString() } },
    'timer-display': {},
    'exit-attempt-form': exit,
    'next-section-form': advance,
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
      querySelectorAll: (selector) => {
        if (selector === '.answer-form') return [answer];
        if (selector === '.exam-question-page') return [questionPage];
        return [];
      },
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
    advance: () => events.advanceSection({ preventDefault() {} }),
    exit: () => events.exitAttempt({ preventDefault() {}, currentTarget: exit }),
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
    sectionAdvances: () => sectionAdvances,
    dismissed: () => dismissed,
    exits: () => exits,
  };
}

test('advancing to the next section waits for pending answers without submitting the exam', async () => {
  const page = exam();
  page.input('last answer in section');
  page.advance();
  await page.confirm(true);
  assert.equal(page.sectionAdvances(), 0);
  assert.equal(page.requests.at(-1).body.get('textAnswer'), 'last answer in section');
  await page.respond(true);
  assert.equal(page.sectionAdvances(), 1);
  assert.equal(page.submissions(), 0);
});

test('leaving an in-progress exam requires explicit confirmation', async () => {
  const page = exam();
  page.exit();
  assert.match(page.confirmations.at(-1).options.title, /Thoát bài thi/);
  await page.confirm(false);
  assert.equal(page.exits(), 0);

  page.exit();
  await page.confirm(true);
  assert.equal(page.exits(), 1);
  assert.equal(page.submissions(), 0);
});

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

test('manual submission is blocked while an answer is still blank', () => {
  const page = exam({ answered: false });
  page.submit();
  assert.equal(page.confirmations.length, 0);
  assert.equal(page.submissions(), 0);
  assert.equal(page.error.hidden, false);
  assert.match(page.error.textContent, /1 câu chưa hoàn thành/);
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
