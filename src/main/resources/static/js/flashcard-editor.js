(() => {
  const root = document.getElementById('card-rows');
  const form = document.getElementById('set-editor');
  let dirty = false;
  form.addEventListener('input', () => {
    dirty = true;
  });
  form.addEventListener('click', (event) => {
    if (event.target.closest('.remove-card,#add-card,#import-cards,#undo-remove')) dirty = true;
  });
  document.getElementById('leave-editor').addEventListener('click', (event) => {
    if (dirty && !window.confirm('Bạn có thay đổi chưa lưu. Rời trang và bỏ các thay đổi này?'))
      event.preventDefault();
  });
  const message = document.getElementById('import-message');
  const editorMessage = document.getElementById('editor-message'),
    undo = document.getElementById('undo-remove');
  let removed = null;
  undo.addEventListener('click', () => {
    if (removed) {
      root.insertBefore(removed.row, root.children[removed.index] || null);
      removed = null;
      undo.hidden = true;
      reindex();
      editorMessage.textContent = 'Đã khôi phục thẻ.';
    }
  });
  function updateCount(input) {
    const counter = input.closest('.field')?.querySelector('.char-count');
    if (counter) counter.textContent = `${input.value.length}/${input.maxLength}`;
  }
  function reindex() {
    [...root.children].forEach((row, i) => {
      row.querySelectorAll('[data-name]').forEach((input) => {
        input.name = `rows[${i}].${input.dataset.name}`;
        if (input.matches('textarea')) {
          input.id = `card-${i}-${input.dataset.name}`;
          const label = input.closest('.field')?.querySelector('label');
          if (label) label.htmlFor = input.id;
        }
        updateCount(input);
      });
      const number = row.querySelector('.card-number');
      if (number) number.textContent = i + 1;
    });
    const count = document.getElementById('card-count');
    if (count) count.textContent = `${root.children.length}/200 thẻ`;
  }
  function add(term = '', definition = '') {
    if (root.children.length >= 200) {
      message.textContent = 'Mỗi bộ tối đa 200 thẻ.';
      return;
    }
    const row = document.createElement('div');
    row.className = 'dashboard-panel card-editor-row';
    const number = document.createElement('span');
    number.className = 'card-number';
    row.append(number);
    for (const [field, label, value] of [
      ['term', 'Thuật ngữ', term],
      ['definition', 'Định nghĩa', definition],
    ]) {
      const box = document.createElement('div');
      box.className = 'field';
      const wrap = document.createElement('label');
      wrap.textContent = label;
      const input = document.createElement('textarea');
      input.dataset.name = field;
      input.value = value;
      input.required = true;
      input.rows = 2;
      input.maxLength = field === 'term' ? 200 : 1000;
      const counter = document.createElement('small');
      counter.className = 'char-count';
      box.append(wrap, input, counter);
      row.append(box);
      updateCount(input);
    }
    const remove = document.createElement('button');
    remove.type = 'button';
    remove.className = 'remove-card';
    remove.setAttribute('aria-label', 'Xóa thẻ');
    remove.textContent = '×';
    row.append(remove);
    root.append(row);
    reindex();
  }
  root.addEventListener('click', (event) => {
    if (event.target.closest('.remove-card')) {
      const row = event.target.closest('.card-editor-row');
      removed = { row, index: [...root.children].indexOf(row) };
      row.remove();
      undo.hidden = false;
      reindex();
      editorMessage.textContent = 'Đã xóa 1 thẻ. Lưu bộ thẻ để xác nhận.';
    }
  });
  document.getElementById('add-card').addEventListener('click', () => add());
  document.getElementById('import-cards').addEventListener('click', () => {
    const lines = document
      .getElementById('bulk')
      .value.split(/\r?\n/)
      .filter((line) => line.trim());
    const parsed = [];
    for (const line of lines) {
      const index = line.includes('\t') ? line.indexOf('\t') : line.indexOf('|');
      if (index < 1 || !line.slice(index + 1).trim()) {
        message.textContent = 'Mỗi dòng cần đủ từ và nghĩa, ngăn bằng Tab hoặc |.';
        return;
      }
      parsed.push([line.slice(0, index).trim(), line.slice(index + 1).trim()]);
    }
    if (!parsed.length) {
      message.textContent = 'Hãy nhập ít nhất một cặp từ và nghĩa.';
      return;
    }
    const blank = [...root.children].filter((row) =>
      [...row.querySelectorAll('textarea')].every((input) => !input.value.trim()),
    );
    if (root.children.length - blank.length + parsed.length > 200) {
      message.textContent = 'Mỗi bộ tối đa 200 thẻ.';
      return;
    }
    blank.forEach((row) => row.remove());
    parsed.forEach((pair) => add(...pair));
    document.getElementById('bulk').value = '';
    message.textContent = `Đã thêm ${parsed.length} thẻ. Bấm Lưu bộ thẻ để lưu.`;
    reindex();
  });
  root.addEventListener('input', (event) => {
    if (event.target.matches('textarea[data-name]')) updateCount(event.target);
  });
  reindex();
  const lesson = document.getElementById('lesson'),
    level = document.getElementById('level');
  if (lesson && level)
    lesson.addEventListener('change', () => {
      const option = lesson.selectedOptions[0];
      if (option && option.dataset.level) level.value = option.dataset.level;
    });
})();
