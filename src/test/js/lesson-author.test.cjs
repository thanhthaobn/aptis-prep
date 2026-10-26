const test = require('node:test');
const assert = require('node:assert/strict');
const {
  readAuthorProfiles,
  renumberLessonChecks,
} = require('../../main/resources/static/js/lesson-author.js');

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

test('removing a lesson check keeps names, IDs, labels and selected answers together', () => {
  function checkRow(index, selected) {
    const fields = ['prompt', 'optionA', 'optionF', 'correctOption'].map((field) => ({
      id: `checks-${index}-${field}`,
      name: `checks[${index}].${field}`,
      value: field === 'correctOption' ? selected : `Kept ${field}`,
    }));
    const labels = fields.map((field) => ({ htmlFor: field.id }));
    return {
      dataset: { questionIndex: index },
      fields,
      labels,
      legend: { textContent: '' },
      querySelector: function () {
        return this.legend;
      },
      querySelectorAll: (selector) => (selector === '[for]' ? labels : fields),
    };
  }
  const rows = [checkRow(1, '5'), checkRow(3, '1')];
  renumberLessonChecks(rows);
  rows.forEach((row, index) => {
    assert.equal(row.dataset.questionIndex, index);
    assert.equal(row.legend.textContent, `Câu ${index + 1}`);
    row.fields.forEach((field, position) => {
      assert.ok(field.name.startsWith(`checks[${index}].`));
      assert.ok(field.id.startsWith(`checks-${index}-`));
      assert.equal(row.labels[position].htmlFor, field.id);
    });
  });
  assert.equal(rows[0].fields[3].value, '5');
  assert.equal(rows[1].fields[3].value, '1');
});
