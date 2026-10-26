const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { runInNewContext } = require('node:vm');
const source = readFileSync(
  resolve(__dirname, '../../main/resources/static/js/app-dialog.js'),
  'utf8',
);

function page() {
  const elements = [],
    events = {},
    navigations = [];
  const trigger = {
    isConnected: true,
    focus() {
      this.focused = true;
    },
  };
  function element(tag) {
    const node = {
      tag,
      children: [],
      events: {},
      attributes: {},
      classList: { toggle() {} },
      setAttribute(name, value) {
        this.attributes[name] = value;
      },
      addEventListener(name, handler) {
        this.events[name] = handler;
      },
      append(...children) {
        this.children.push(...children);
      },
      showModal() {
        this.open = true;
      },
      close() {
        this.open = false;
      },
      focus() {
        this.focused = true;
      },
    };
    elements.push(node);
    return node;
  }
  const document = {
    activeElement: trigger,
    createElement: element,
    body: element('body'),
    addEventListener(name, handler, capture = false) {
      (events[name] ||= []).push({ handler, capture });
    },
  };
  const window = {
    location: {
      href: 'http://localhost/editor',
      assign(url) {
        navigations.push(url);
      },
    },
  };
  runInNewContext(source, { document, window, URL });
  function emit(name, event) {
    event.defaultPrevented = false;
    event.preventDefault = () => {
      event.defaultPrevented = true;
    };
    return Promise.all(
      [...(events[name] || [])]
        .sort((a, b) => Number(b.capture) - Number(a.capture))
        .map(({ handler }) => handler(event)),
    );
  }
  function form(dataset = {}) {
    return {
      dataset,
      submissions: 0,
      matches: () => !!dataset.confirmMessage,
      requestSubmit(submitter) {
        this.submitter = submitter;
        const event = { target: this, submitter };
        emit('submit', event);
        if (!event.defaultPrevented) this.submissions++;
      },
    };
  }
  return {
    api: window.AppDialog,
    elements,
    trigger,
    navigations,
    emit,
    form,
    dialog: () => elements.find((node) => node.tag === 'dialog'),
    button: (text) => elements.find((node) => node.tag === 'button' && node.textContent === text),
  };
}

test('website confirmation uses text, accessible labels and explicit Vietnamese buttons', async () => {
  const p = page();
  const result = p.api.confirm({ title: 'Nộp bài?', message: '<img src=x onerror=alert(1)>' });
  assert.equal(p.dialog().open, true);
  assert.equal(p.dialog().attributes['aria-labelledby'], 'app-confirm-title');
  assert.equal(p.dialog().attributes['aria-describedby'], 'app-confirm-message');
  assert.equal(
    p.elements.find((node) => node.tag === 'p').textContent,
    '<img src=x onerror=alert(1)>',
  );
  assert.equal(p.button('Hủy').focused, true);
  p.button('Xác nhận').events.click();
  assert.equal(await result, true);
  assert.equal(p.dialog().open, false);
  assert.equal(p.trigger.focused, true);
});

test('Escape cancels and a duplicate request cannot replace the open confirmation', async () => {
  const p = page();
  const result = p.api.confirm({ message: 'Original question' });
  assert.equal(await p.api.confirm({ message: 'Another question' }), false);
  let prevented = false;
  p.dialog().events.cancel({
    preventDefault() {
      prevented = true;
    },
  });
  assert.equal(await result, false);
  assert.equal(prevented, true);
});

test('archive confirmation retains the clicked submitter and submits once after approval', async () => {
  const p = page();
  const form = p.form({
    confirmTitle: 'Lưu trữ?',
    confirmMessage: 'Giữ nguyên tiến độ',
    confirmButton: 'Lưu trữ',
  });
  const submitter = { name: 'action', value: 'archive' };
  const done = p.emit('submit', { target: form, submitter });
  assert.equal(form.submissions, 0);
  const duplicate = p.emit('submit', { target: form, submitter });
  p.button('Lưu trữ').events.click();
  await Promise.all([done, duplicate]);
  assert.equal(form.submissions, 1);
  assert.equal(form.submitter, submitter);
});

test('cancelling archive never submits the form', async () => {
  const p = page();
  const form = p.form({ confirmMessage: 'Archive this set?' });
  const done = p.emit('submit', { target: form });
  p.button('Hủy').events.click();
  await done;
  assert.equal(form.submissions, 0);
});

test('unsaved edits block navigation on cancel and clear the guard after confirmed departure', async () => {
  const p = page();
  let dirty = true;
  p.api.guardUnsaved(p.form(), {
    isDirty: () => dirty,
    onDiscard: () => {
      dirty = false;
    },
    message: 'Unsaved edits',
  });
  const link = { href: 'http://localhost/flashcards', target: '', hasAttribute: () => false };
  const event = () => ({ button: 0, target: { closest: () => link } });
  const cancelled = p.emit('click', event());
  p.button('Tiếp tục chỉnh sửa').events.click();
  await cancelled;
  assert.equal(dirty, true);
  assert.deepEqual(p.navigations, []);
  const confirmed = p.emit('click', event());
  p.button('Rời trang').events.click();
  await confirmed;
  assert.equal(dirty, false);
  assert.deepEqual(p.navigations, [link.href]);
});

test('unsaved guard lets the editor save normally and confirms a navigation form such as logout', async () => {
  const p = page();
  const editor = p.form(),
    logout = p.form(),
    submitter = {};
  let dirty = true;
  p.api.guardUnsaved(editor, {
    isDirty: () => dirty,
    onDiscard: () => {
      dirty = false;
    },
    message: 'Unsaved edits',
  });
  await p.emit('submit', { target: editor });
  assert.equal(p.dialog(), undefined);
  const done = p.emit('submit', { target: logout, submitter });
  p.button('Rời trang').events.click();
  await done;
  assert.equal(logout.submissions, 1);
  assert.equal(logout.submitter, submitter);
});
