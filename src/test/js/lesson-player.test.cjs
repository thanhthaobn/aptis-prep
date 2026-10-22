const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { runInNewContext } = require('node:vm');
const source = readFileSync(resolve(__dirname, '../../main/resources/static/js/lesson-player.js'), 'utf8');

function player({ selected = false } = {}) {
  const events = {};
  let finishSave, reloads = 0;
  const audio = { duration: 10, played: { length: 1, start: () => 0, end: () => 10 },
    addEventListener: (name, callback) => { events[name] = callback; } };
  runInNewContext(source, {
    document: {
      querySelector: selector => selector === '.lesson-classroom' ? { dataset: { lessonId: '37' } }
        : selector.includes(':checked') ? (selected ? {} : null) : null,
      getElementById: id => id === 'lesson-audio' ? audio : null,
      querySelectorAll: () => []
    },
    fetch: () => new Promise(resolve => { finishSave = resolve; }),
    window: { location: { reload: () => { reloads++; } } },
    IntersectionObserver: class { observe() {} }
  });
  return { events, save: ok => finishSave({ ok }), reloads: () => reloads };
}

test('finishing Listening keeps selected quiz answers', async () => {
  const p = player({ selected: true });
  p.events.timeupdate(); p.save(true); await p.events.ended();
  assert.equal(p.reloads(), 0);
});
test('unlock refresh waits for the listening mark to reach the server', async () => {
  const p = player(); p.events.timeupdate();
  const ended = p.events.ended(); assert.equal(p.reloads(), 0);
  p.save(true); await ended; assert.equal(p.reloads(), 1);
});
test('failed listening save does not refresh or claim an unlock', async () => {
  const p = player(); p.events.timeupdate(); p.save(false); await p.events.ended();
  assert.equal(p.reloads(), 0);
});
test('ending without sufficient listening does not refresh', async () => {
  const p = player(); await p.events.ended(); assert.equal(p.reloads(), 0);
});
