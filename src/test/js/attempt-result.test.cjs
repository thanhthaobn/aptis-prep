const test = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');

const template = readFileSync('src/main/resources/templates/student/attempt-result.html', 'utf8');

test('result page offers one dedicated review entry without embedding a second review', () => {
  assert.equal((template.match(/Xem lại bài làm/g) || []).length, 1);
  assert.doesNotMatch(template, /result-review-panel/);
  assert.doesNotMatch(template, /Xem câu hỏi và đáp án/);
  assert.match(template, /\/student\/attempts\/\{id\}\/review/);
});
