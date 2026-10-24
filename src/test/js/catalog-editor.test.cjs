const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

function section(index) {
  const fields = [
    { id: `sections-${index}-skill`, name: `sections[${index}].skill`, value: 'READING' },
    { htmlFor: `sections-${index}-skill` },
    { name: `sections[${index}].durationMinutes`, value: '10' },
    { name: `sections[${index}].questionIds`, value: '42' },
    { id: `sections-${index}-question-42`, checked: true, dataset: { sectionIndex: index } },
  ];
  return {
    dataset: { sectionIndex: index },
    fields,
    number: { textContent: String(index + 1) },
    cloneNode() {
      return section(index);
    },
    querySelector(selector) {
      if (selector === '.section-skill') return fields[0];
      if (selector === '.section-question-ids') return fields[3];
      if (selector === '.exam-section-number') return this.number;
      return null;
    },
    querySelectorAll(selector) {
      if (selector === '[id]') return fields.filter((field) => field.id);
      if (selector === '[for]') return fields.filter((field) => field.htmlFor);
      if (selector === '[name]') return fields.filter((field) => field.name);
      if (selector === '.section-question') return [fields[4]];
      return [];
    },
  };
}

test('adding exam sections gives each field a separate binding and label without copying answers', () => {
  const rows = [section(0)];
  const listeners = {};
  const sections = {
    querySelectorAll: () => rows,
    querySelector: () => rows[0],
    addEventListener() {},
    closest: () => ({ addEventListener() {} }),
    appendChild: (row) => rows.push(row),
  };
  const add = { addEventListener: (event, callback) => (listeners[event] = callback) };
  const document = {
    getElementById(id) {
      if (id === 'exam-sections') return sections;
      if (id === 'add-exam-section') return add;
      return null;
    },
  };
  vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/catalog-editor.js', 'utf8'), {
    document,
  });
  listeners.click();
  listeners.click();
  assert.equal(rows.length, 3);
  rows.forEach((row, index) => {
    assert.equal(row.fields[0].id, `sections-${index}-skill`);
    assert.equal(row.fields[1].htmlFor, row.fields[0].id);
    assert.equal(row.fields[0].name, `sections[${index}].skill`);
    assert.equal(row.fields[2].name, `sections[${index}].durationMinutes`);
    assert.equal(row.fields[3].name, `sections[${index}].questionIds`);
    assert.equal(row.number.textContent, String(index + 1));
    if (index > 0) {
      assert.equal(row.fields[3].value, '');
      assert.equal(row.fields[4].checked, false);
    }
  });
  assert.equal(rows[0].fields[3].value, '42');
  assert.equal(rows[0].fields[4].checked, true);
});
