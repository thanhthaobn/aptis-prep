const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { runInNewContext } = require('node:vm');
const source = readFileSync(resolve(__dirname, '../../main/resources/static/js/flashcard-study.js'), 'utf8');

function study(responses) {
  const elements = new Map(), requests = [];
  function element(id) {
    if (!elements.has(id)) elements.set(id, {
      textContent: '', checked: false, events: {},
      classList: { toggle() {}, add() {}, remove() {} },
      setAttribute() {}, replaceChildren() {}, append() {},
      querySelector: () => null,
      addEventListener(name, callback) { this.events[name] = callback; }
    });
    return elements.get(id);
  }
  let nextKey = 0;
  runInNewContext(source, {
    URLSearchParams,
    crypto: { randomUUID: () => `review-action-${++nextKey}` },
    document: {
      querySelector: () => ({ dataset: { setId: '1' } }),
      querySelectorAll: () => [{ dataset: { id: '7', state: 'LEARNING' },
        querySelector: selector => ({ textContent: selector === '.term' ? 'hello' : 'xin chào' }) }],
      getElementById: element, createElement: () => element(Symbol()), addEventListener() {}
    },
    fetch: async (url, request) => {
      requests.push({ key: request.body.get('reviewKey'), state: request.body.get('state') });
      const result = responses.shift();
      if (result instanceof Error) throw result;
      return { ok: result, redirected: false };
    }
  });
  return { requests, click: id => element(id).events.click() };
}

test('network retry keeps its review key and a new round uses a new key', async () => {
  const page = study([new Error('connection lost'), true, true]);
  await page.click('mastered');
  await page.click('mastered');
  assert.equal(page.requests[0].key, page.requests[1].key);
  page.click('restart-all');
  await page.click('mastered');
  assert.notEqual(page.requests[1].key, page.requests[2].key);
});

test('changing the choice after a failed request creates a different action', async () => {
  const page = study([false, true]);
  await page.click('mastered');
  await page.click('learning');
  assert.notEqual(page.requests[0].key, page.requests[1].key);
  assert.equal(page.requests[1].state, 'LEARNING');
});
