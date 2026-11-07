const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { runInNewContext } = require('node:vm');
const source = readFileSync(
  resolve(__dirname, '../../main/resources/static/js/exam-runner.js'),
  'utf8',
);

function recorderPage() {
  const requests = [],
    recorders = [];
  let now = 0,
    interval,
    navigated = false,
    stoppedTracks = 0;
  function element() {
    const events = {},
      classes = new Set();
    return {
      disabled: false,
      textContent: '',
      value: 0,
      classList: {
        add: (c) => classes.add(c),
        remove: (c) => classes.delete(c),
        contains: (c) => classes.has(c),
      },
      addEventListener: (name, fn) => {
        events[name] = fn;
      },
      click() {
        return events.click?.();
      },
      setAttribute(name, value) {
        this[name] = value;
      },
      removeAttribute(name) {
        delete this[name];
      },
    };
  }
  const fields = Object.fromEntries(
    [
      'start',
      'stop',
      'reset',
      'save',
      'preview',
      'status',
      'meter',
      'progress',
      'time',
      'saved',
    ].map((k) => [k, element()]),
  );
  const selectors = {
    '.recording-start': fields.start,
    '.recording-stop': fields.stop,
    '.recording-reset': fields.reset,
    '.recording-save': fields.save,
    '.recording-preview': fields.preview,
    '.recording-status': fields.status,
    '.recording-meter': fields.meter,
    '.recording-progress': fields.progress,
    '.recording-time': fields.time,
    '.recording-saved': fields.saved,
  };
  const item = { dataset: {}, dispatchEvent() {} };
  const box = {
    dataset: { responseSeconds: '2', itemId: '8', questionId: '5', audioAction: '/audio/autosave' },
    querySelector: (s) => selectors[s],
    closest: () => item,
  };
  class Recorder {
    static isTypeSupported() {
      return true;
    }
    constructor() {
      this.state = 'inactive';
      this.mimeType = 'audio/webm';
      recorders.push(this);
    }
    start() {
      this.state = 'recording';
    }
    stop() {
      this.state = 'inactive';
      this.ondataavailable({ data: new Blob(['audio']) });
      this.onstop();
    }
  }
  runInNewContext(source, {
    Blob,
    Event,
    MediaRecorder: Recorder,
    TextEncoder,
    FormData: class {
      append() {}
    },
    URL: { createObjectURL: () => 'blob:preview', revokeObjectURL() {} },
    navigator: {
      mediaDevices: {
        getUserMedia: async () => ({
          getTracks: () => [
            {
              stop() {
                stoppedTracks++;
              },
            },
          ],
        }),
      },
    },
    performance: { now: () => now },
    setInterval: (fn) => {
      interval = fn;
      return 1;
    },
    clearInterval() {},
    setTimeout() {},
    clearTimeout() {},
    fetch: (url, request) => new Promise((resolve) => requests.push({ url, request, resolve })),
    document: {
      querySelector: () => ({ content: 'csrf' }),
      querySelectorAll: (s) => (s === '.speaking-recorder' ? [box] : []),
      getElementById: () => null,
      body: { classList: { contains: () => false } },
    },
    window: {
      MediaRecorder: Recorder,
      location: {
        set href(v) {
          navigated = true;
        },
      },
    },
  });
  return {
    ...fields,
    box,
    item,
    requests,
    recorders,
    advance(ms) {
      now += ms;
      interval();
    },
    get navigated() {
      return navigated;
    },
    get stoppedTracks() {
      return stoppedTracks;
    },
  };
}

test('Speaking keeps timer and playback slots mounted through recording, preview and rerecord', async () => {
  const p = recorderPage();
  await p.start.click();
  assert.equal(p.box.dataset.state, 'recording');
  assert.equal(p.box.dataset.dirty, 'true');
  await p.stop.click();
  assert.equal(p.box.dataset.state, 'preview');
  assert.equal(p.preview.classList.contains('is-empty'), false);
  await p.reset.click();
  await new Promise(setImmediate);
  assert.equal(p.recorders.length, 2, 'rerecord starts a new recording directly');
  assert.equal(p.box.dataset.state, 'recording');
  assert.equal(p.preview.classList.contains('is-empty'), true);
  assert.equal(p.meter.hidden, undefined, 'timer never leaves the layout');
  assert.equal(p.preview.hidden, undefined, 'audio slot never leaves the layout');
});

test('Speaking stops once at the time limit and releases microphone tracks', async () => {
  const p = recorderPage();
  await p.start.click();
  p.advance(2000);
  assert.equal(p.recorders[0].state, 'inactive');
  assert.equal(p.box.dataset.state, 'preview');
  assert.equal(p.stoppedTracks, 1);
  p.advance(1000);
  assert.equal(p.stoppedTracks, 1);
});

test('Speaking failed upload retains the recording; retry saves without navigation', async () => {
  const p = recorderPage();
  await p.start.click();
  await p.stop.click();
  const first = p.save.click();
  p.requests[0].resolve({
    ok: false,
    json: async () => ({ saved: false, message: 'Mất kết nối' }),
  });
  await first;
  assert.equal(p.box.dataset.dirty, 'true');
  assert.equal(p.save.disabled, false);
  assert.equal(p.preview.src, 'blob:preview');
  const second = p.save.click();
  p.requests[1].resolve({ ok: true, json: async () => ({ saved: true }) });
  await second;
  assert.equal(p.item.dataset.saved, 'true');
  assert.equal(p.box.dataset.dirty, 'false');
  assert.equal(p.navigated, false);
});
