function readAuthorProfiles(root) {
  const fields = [
    'exampleText',
    'practicePrompt',
    'strategy',
    'analysis',
    'checklist',
    'sampleResponse',
  ];
  return Object.fromEntries(
    Array.from(root.querySelectorAll('#lesson-author-profiles [data-skill]'), (element) => [
      element.dataset.skill,
      {
        labels: fields.map(
          (field) => element.querySelector('[data-field="' + field + '"]').textContent,
        ),
        tasks: Array.from(element.querySelectorAll('[data-task]'), (task) => task.textContent),
        hint: element.dataset.hint,
      },
    ]),
  );
}
if (typeof module !== 'undefined' && module.exports) module.exports = { readAuthorProfiles };
(() => {
  if (typeof document === 'undefined') return;
  const skill = document.getElementById('skill');
  if (!skill) return;
  const profiles = readAuthorProfiles(document);
  function update() {
    const p = profiles[skill.value];
    if (!p) return;
    document.querySelector('.lesson-core-fields').dataset.authorSkill = skill.value;
    document.getElementById('skill-guidance').textContent = p.hint;
    document.querySelectorAll('.lesson-media-placement').forEach((el) => (el.textContent = p.hint));
    [
      'exampleText',
      'practicePrompt',
      'strategy',
      'analysis',
      'checklist',
      'sampleResponse',
    ].forEach((id, i) => {
      document.querySelector('label[for="' + id + '"]').textContent = p.labels[i];
      document.getElementById(id).placeholder = p.labels[i];
    });
    document
      .getElementById('task-suggestions')
      .replaceChildren(...p.tasks.map((t) => new Option(t, t)));
    document.getElementById('listening-audio').hidden = skill.value !== 'Listening';
    document.getElementById('writing-settings').hidden = skill.value !== 'Writing';
    const sample = document.getElementById('sampleVisibility'),
      listenOption = sample.querySelector('[value="AFTER_LISTEN"]');
    if (skill.value === 'Listening' && !listenOption)
      sample.append(new Option('Sau khi nghe ít nhất 90%', 'AFTER_LISTEN'));
    if (skill.value !== 'Listening' && listenOption) {
      if (sample.value === 'AFTER_LISTEN') sample.value = 'AFTER_SUBMIT';
      listenOption.remove();
    }
    const publishing = document.getElementById('status').value === 'PUBLISHED';
    document.getElementById('practicePrompt').required =
      publishing && ['Writing', 'Speaking'].includes(skill.value);
  }
  skill.addEventListener('change', update);
  document.getElementById('status').addEventListener('change', update);
  update();
  for (const [id, max] of [
    ['audio', 20],
    ['image', 8],
    ['video', 50],
  ]) {
    const input = document.getElementById(id);
    input.addEventListener('change', () => {
      input.setCustomValidity(
        input.files[0]?.size > max * 1024 * 1024 ? 'Tệp vượt quá ' + max + ' MB.' : '',
      );
      input.reportValidity();
    });
  }

  const list = document.getElementById('question-list'),
    add = document.getElementById('add-question'),
    template = document.getElementById('question-template'),
    empty = document.getElementById('no-question');
  function reindex() {
    const rows = [...list.querySelectorAll('.lesson-question-editor')];
    rows.forEach((row, index) => {
      row.dataset.questionIndex = index;
      row.querySelector('legend').textContent = `Câu ${index + 1}`;
      row
        .querySelectorAll('[name]')
        .forEach((el) => (el.name = el.name.replace(/checks\[\d+\]/, `checks[${index}]`)));
      row
        .querySelectorAll('[id]')
        .forEach((el) => (el.id = el.id.replace(/checks\d+/, `checks${index}`)));
    });
    empty.hidden = rows.length > 0;
    add.disabled = rows.length >= 20;
  }
  function optionRows(row) {
    return [...row.querySelectorAll('.question-option')];
  }
  function refreshOptions(row) {
    const options = optionRows(row),
      visible = options.filter((option) => !option.hidden).length,
      correct = row.querySelector('.correct-option');
    options.forEach((option, index) => {
      const choice = correct?.querySelector(`option[value="${index}"]`);
      if (choice) choice.hidden = option.hidden;
    });
    row.querySelector('.add-option').disabled = visible >= 6;
  }
  function removeOption(row, index) {
    const options = optionRows(row),
      correct = row.querySelector('.correct-option'),
      visible = options.filter((option) => !option.hidden).length;
    if (index < 2 || visible <= 2) return;
    for (let i = index; i < options.length - 1; i++)
      options[i].querySelector('textarea').value = options[i + 1].hidden
        ? ''
        : options[i + 1].querySelector('textarea').value;
    const lastVisible = visible - 1;
    options[lastVisible].querySelector('textarea').value = '';
    options[lastVisible].hidden = true;
    if (correct.value !== '' && Number(correct.value) === index) correct.value = '';
    else if (correct.value !== '' && Number(correct.value) > index)
      correct.value = String(Number(correct.value) - 1);
    refreshOptions(row);
  }
  function wire(row) {
    row.querySelector('.remove-question').addEventListener('click', () => {
      row.remove();
      reindex();
    });
    const options = optionRows(row);
    let visible = Math.max(
      2,
      options.reduce(
        (count, option, index) =>
          count + (index < 2 || option.querySelector('textarea').value.trim() ? 1 : 0),
        0,
      ),
    );
    options.forEach((option, index) => {
      option.hidden = index >= visible;
      option
        .querySelector('.remove-option')
        ?.addEventListener('click', () => removeOption(row, index));
    });
    row.querySelector('.add-option').addEventListener('click', () => {
      const next = options.find((option) => option.hidden);
      if (next) {
        next.hidden = false;
        next.querySelector('textarea').focus();
        refreshOptions(row);
      }
    });
    refreshOptions(row);
  }
  list.querySelectorAll('.lesson-question-editor').forEach(wire);
  add.addEventListener('click', () => {
    if (list.children.length >= 20) return;
    const row = template.content.firstElementChild.cloneNode(true),
      index = list.children.length;
    row.querySelectorAll('[data-field]').forEach((el) => {
      const field = el.dataset.field;
      el.name = `checks[${index}].${field}`;
      el.id = `checks${index}.${field}`;
    });
    list.appendChild(row);
    wire(row);
    reindex();
    row.querySelector('textarea').focus();
  });
  reindex();
})();
