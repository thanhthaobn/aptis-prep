const test = require('node:test');
const assert = require('node:assert/strict');
const { readAuthorProfiles } = require('../../main/resources/static/js/lesson-author.js');

function metadata(skill, prefix) {
  return {
    dataset: { skill, hint: `${prefix} hint` },
    querySelector(selector) {
      return { textContent: `${prefix} ${selector}` };
    },
    querySelectorAll() {
      return [{ textContent: `${prefix} task 1` }, { textContent: `${prefix} task 2` }];
    },
  };
}

test('author labels, tasks and hints are read from backend-rendered metadata', () => {
  const reading = metadata('Reading', 'server reading');
  const speaking = metadata('Speaking', 'server speaking');
  const profiles = readAuthorProfiles({ querySelectorAll: () => [reading, speaking] });
  assert.deepEqual(Object.keys(profiles), ['Reading', 'Speaking']);
  assert.equal(profiles.Reading.labels.length, 6);
  assert.equal(profiles.Reading.labels[0], 'server reading [data-field="exampleText"]');
  assert.deepEqual(profiles.Speaking.tasks, ['server speaking task 1', 'server speaking task 2']);
  assert.equal(profiles.Speaking.hint, 'server speaking hint');
  speaking.dataset.hint = 'Updated backend hint';
  assert.equal(
    readAuthorProfiles({ querySelectorAll: () => [speaking] }).Speaking.hint,
    'Updated backend hint',
  );
});

test('missing metadata does not invent sample lesson content', () => {
  assert.deepEqual(readAuthorProfiles({ querySelectorAll: () => [] }), {});
});
