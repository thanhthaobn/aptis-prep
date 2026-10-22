const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { runInNewContext } = require('node:vm');
const source = readFileSync(resolve(__dirname, '../../main/resources/static/js/lesson-player.js'), 'utf8');
const tick = () => new Promise(resolve => setImmediate(resolve));

function player({ selected = false } = {}) {
  const events = {}, requests = [];
  let interval, reloads = 0;
  const audio = { duration: 100, currentTime: 0, paused: false, ended: false, playbackRate: 1,
    pause: () => { audio.paused = true; events.pause?.(); },
    play: async () => { audio.paused = false; events.play?.(); },
    addEventListener: (name, callback) => { events[name] = callback; } };
  runInNewContext(source, {
    URLSearchParams,
    setInterval: callback => { interval = callback; return 1; }, clearInterval() {},
    document: {
      querySelector: selector => selector === '.lesson-classroom' ? { dataset: { lessonId: '37' } }
        : selector.includes(':checked') ? (selected ? {} : null) : null,
      getElementById: id => id === 'lesson-audio' ? audio : null,
      querySelectorAll: () => []
    },
    fetch: (url, request) => new Promise(resolve => { requests.push({ url, body: request.body, resolve }); }),
    window: { addEventListener() {}, location: { reload: () => { reloads++; } } },
    IntersectionObserver: class { observe() {} }
  });
  async function respond({ ok = true, sequence = 0, listened = 0, verified = false, token = 'session-1' } = {}) {
    requests.at(-1).resolve({ ok, status: ok ? 200 : 500, redirected: false,
      json: async () => ({ token, sequence, duration: 100, listened, verified }) });
    await tick();
  }
  async function start() { events.play(); await tick(); await respond(); }
  async function end() { audio.currentTime = 100; audio.ended = true; const done = events.ended(); await tick(); return { done }; }
  return { events, audio, requests, respond, start, end, pulse: async () => { interval(); await tick(); }, reloads: () => reloads };
}

test('start sends a position, not a trusted client duration or listened claim', async () => {
  const p = player(); await p.start();
  assert.match(p.requests[0].url, /listening\/start$/);
  assert.equal(p.requests[0].body.get('position'), '0');
  assert.equal(p.requests[0].body.has('duration'), false);
  assert.equal(p.requests[0].body.has('listened'), false);
});

test('server verification keeps selected quiz answers instead of reloading', async () => {
  const p = player({ selected: true }); await p.start(); const { done } = await p.end();
  await p.respond({ sequence: 1, listened: 90, verified: true }); await done;
  assert.equal(p.reloads(), 0);
});

test('unlock refresh waits for the server acknowledgement', async () => {
  const p = player(); await p.start(); const { done } = await p.end();
  assert.equal(p.reloads(), 0);
  await p.respond({ sequence: 1, listened: 90, verified: true }); await done;
  assert.equal(p.reloads(), 1);
});

test('failed or insufficient server verification does not claim an unlock', async () => {
  for (const ok of [true, false]) {
    const p = player(); await p.start(); const { done } = await p.end();
    await p.respond({ ok, sequence: 1, listened: 89, verified: false }); await done;
    assert.equal(p.reloads(), 0);
  }
});

test('a lost response retries the same position and sequence', async () => {
  const p = player(); await p.start(); p.audio.currentTime = 4; await p.pulse();
  await p.respond({ ok: false }); p.audio.currentTime = 8; await p.pulse();
  assert.equal(p.requests[1].body.toString(), p.requests[2].body.toString());
  await p.respond({ sequence: 1, listened: 4 });
});

test('a seek is sent as a discontinuity, never as a listened percentage', async () => {
  const p = player(); await p.start(); p.events.seeking(); p.audio.currentTime = 90; p.events.seeked(); await tick();
  assert.equal(p.requests.at(-1).body.get('discontinuity'), 'true');
  await p.respond({ sequence: 1, verified: false });
});

test('ending without a started session cannot unlock a lesson', async () => {
  const p = player(); await p.events.ended(); assert.equal(p.reloads(), 0); assert.equal(p.requests.length, 0);
});
