const test = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');

const template = readFileSync('src/main/resources/templates/student/attempt.html', 'utf8');
const runner = readFileSync('src/main/resources/static/js/exam-runner.js', 'utf8');

test('every multi-part exam uses the shared page navigation', () => {
  assert.match(template, /attempt\.questions\.size\(\) > 1/);
  assert.match(template, /class="exam-pagination"/);
  assert.match(template, /class="dashboard-panel dashboard-section exam-question-page"/);
  assert.doesNotMatch(template, /examType == 'FULL_MOCK'.*full-mock-exam/);
  assert.match(runner, /classList\.contains\('paginated-exam'\)/);
  assert.doesNotMatch(runner, /full-mock-exam/);
  assert.match(runner, /`Phần \$\{currentPage \+ 1\} \/ \$\{pages\.length\}`/);
});
