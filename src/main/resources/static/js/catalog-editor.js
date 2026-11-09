// Page behavior; form values and URLs come from the rendered HTML.
(() => {
  const root = document.getElementById('question-items');
  const add = document.getElementById('add-item');
  if (!root || !add) return;
  const skillTypes = {
    GRAMMAR_VOCABULARY: ['SINGLE_CHOICE', 'MATCHING', 'ORDERING'],
    READING: ['SINGLE_CHOICE', 'MATCHING', 'ORDERING'],
    LISTENING: ['SINGLE_CHOICE', 'MATCHING', 'ORDERING'],
    WRITING: ['WRITING_TEXT'],
    SPEAKING: ['SPEAKING_AUDIO'],
  };
  const typeGuides = {
    SINGLE_CHOICE:
      'Mỗi mục có từ hai phương án trở lên. Nhập các phương án từ A trở đi rồi chọn một đáp án đúng.',
    MATCHING:
      'Mỗi mục là một ý cần ghép với phương án phù hợp. Nhập các phương án và chọn vị trí ghép đúng cho từng ý.',
    ORDERING:
      'Nhập nhiều mục rồi đặt vị trí đúng cho từng mục. Các vị trí phải đủ và không được trùng nhau.',
    WRITING_TEXT:
      'Mỗi mục là một yêu cầu viết. Học viên sẽ trả lời bằng ô văn bản nên không cần nhập phương án hay đáp án đúng.',
    SPEAKING_AUDIO:
      'Mỗi mục là một yêu cầu nói. Học viên sẽ ghi âm trực tiếp nên không cần nhập phương án hay đáp án đúng.',
  };
  const skillHelp = {
    GRAMMAR_VOCABULARY: 'Có thể dùng chọn đáp án, ghép nối hoặc sắp xếp.',
    READING: 'Có thể dùng đoạn văn kèm chọn đáp án, ghép nối hoặc sắp xếp.',
    LISTENING: 'Nên gắn audio ở phần tài nguyên đính kèm rồi chọn dạng trả lời phù hợp.',
    WRITING: 'Chỉ dùng dạng trả lời bằng văn bản.',
    SPEAKING: 'Chỉ dùng dạng trả lời bằng âm thanh và microphone của học viên.',
  };
  const selectedType = () => document.getElementById('questionType')?.value || 'SINGLE_CHOICE';
  const selectedSkill = () => document.getElementById('skill')?.value || 'READING';
  const setFieldState = (field, visible) => {
    field.hidden = !visible;
    field
      .querySelectorAll('input, select, textarea')
      .forEach((input) => (input.disabled = !visible));
  };
  const renumberItems = () => {
    root.querySelectorAll('.question-item-editor').forEach((block, index) => {
      block.dataset.itemIndex = index;
      const number = block.querySelector('.question-item-editor-number');
      if (number) number.textContent = String(index + 1);
      block
        .querySelectorAll('[name]')
        .forEach((input) => (input.name = input.name.replace(/items\[\d+\]/g, `items[${index}]`)));
      block
        .querySelectorAll('[id]')
        .forEach((input) => (input.id = input.id.replace(/items-\d+-/, `items-${index}-`)));
      block
        .querySelectorAll('[for]')
        .forEach(
          (label) => (label.htmlFor = label.htmlFor.replace(/items-\d+-/, `items-${index}-`)),
        );
    });
  };
  const updateQuestionType = () => {
    const skill = selectedSkill();
    const allowed = skillTypes[skill] || skillTypes.READING;
    const typeSelect = document.getElementById('questionType');
    if (typeSelect && !allowed.includes(typeSelect.value)) typeSelect.value = allowed[0];
    const type = selectedType();
    typeSelect
      ?.querySelectorAll('option')
      .forEach((option) => (option.hidden = !allowed.includes(option.value)));
    const readingField = document.getElementById('reading-passage-field');
    if (readingField) setFieldState(readingField, skill === 'READING');
    const skillText = document.getElementById('skill-help');
    if (skillText) skillText.textContent = skillHelp[skill] || '';
    const typeText = document.getElementById('question-type-help');
    if (typeText) typeText.textContent = typeGuides[type] || '';
    const guide = document.getElementById('question-type-guide');
    if (guide) guide.textContent = typeGuides[type] || '';
    root.querySelectorAll('.question-item-editor').forEach((block) => {
      block.querySelectorAll('[data-only-types]').forEach((field) => {
        const types = field.dataset.onlyTypes.split(',');
        setFieldState(field, types.includes(type));
      });
      block
        .querySelectorAll('.option-correct-text')
        .forEach(
          (text) => (text.textContent = type === 'MATCHING' ? 'Vị trí ghép đúng' : 'Đáp án đúng'),
        );
      const remove = block.querySelector('.remove-item');
      if (remove) remove.disabled = root.querySelectorAll('.question-item-editor').length === 1;
    });
  };
  const createItem = (index) => {
    const block = document.createElement('div');
    block.className = 'question-item-editor';
    block.dataset.itemIndex = index;
    block.innerHTML = `<div class="question-item-editor-title"><div class="question-item-editor-heading"><span class="question-item-editor-number">${index + 1}</span><h3>Mục câu hỏi</h3></div><button class="button button-secondary button-small remove-item" type="button">Xóa mục</button></div>
      <div class="field"><label for="items-${index}-content">Nội dung mục câu hỏi</label><textarea id="items-${index}-content" name="items[${index}].content" rows="3" required></textarea></div>
      <div class="catalog-form-grid item-rules-grid"><div class="field conditional-field correct-position-field" data-only-types="ORDERING"><label>Vị trí đúng trong thứ tự</label><input type="number" min="1" name="items[${index}].correctPosition"><small class="field-help">Mỗi mục phải có một vị trí khác nhau từ 1 đến số lượng mục.</small></div><div class="field conditional-field item-writing-rule" data-only-types="WRITING_TEXT"><label>Số từ tối thiểu</label><input type="number" min="0" name="items[${index}].minWordCount"></div><div class="field conditional-field item-writing-rule" data-only-types="WRITING_TEXT"><label>Số từ tối đa</label><input type="number" min="0" name="items[${index}].maxWordCount"></div><div class="field conditional-field item-response-rule" data-only-types="WRITING_TEXT,SPEAKING_AUDIO"><label>Thời gian trả lời (giây)</label><input type="number" min="1" name="items[${index}].responseSeconds"></div></div>
      <fieldset class="catalog-option-fields conditional-field" data-only-types="SINGLE_CHOICE,MATCHING"><legend>Phương án trả lời</legend><p class="field-help">Nhập phương án liên tục từ A trở đi rồi chọn đúng một đáp án đúng.</p>${['A', 'B', 'C'].map((code, optionIndex) => `<div><label class="option-code-label" for="items-${index}-options-${optionIndex}-content">Phương án ${code}</label><input id="items-${index}-options-${optionIndex}-content" type="text" name="items[${index}].options[${optionIndex}].content"><label class="option-correct-label"><input type="radio" name="items[${index}].correctPosition" value="${optionIndex + 1}"><span class="option-correct-text">Đáp án đúng</span></label><input type="hidden" name="items[${index}].options[${optionIndex}].code" value="${code}"></div>`).join('')}</fieldset>`;
    return block;
  };
  document.getElementById('skill')?.addEventListener('change', updateQuestionType);
  document.getElementById('questionType')?.addEventListener('change', updateQuestionType);
  root.addEventListener('click', (event) => {
    const remove = event.target.closest('.remove-item');
    if (!remove || remove.disabled) return;
    remove.closest('.question-item-editor')?.remove();
    renumberItems();
    updateQuestionType();
  });
  add.addEventListener('click', () => {
    root.appendChild(createItem(root.querySelectorAll('.question-item-editor').length));
    updateQuestionType();
  });
  updateQuestionType();
})();
(() => {
  const sections = document.getElementById('exam-sections');
  const add = document.getElementById('add-exam-section');
  const examType = document.getElementById('examType');
  const certificate = document.getElementById('certificateId');
  const scheme = document.getElementById('scoringSchemeId');
  const options = scheme ? [...scheme.options] : [];
  const originalScheme = scheme?.value;
  const originalType = examType?.value;
  const updateSchemes = () => {
    if (!scheme || !examType || !certificate) return;
    const selected = scheme.value;
    const skill = sections?.querySelector('.section-skill')?.value;
    const allowed = options.filter(
      (option) =>
        !option.value ||
        (option.dataset.certificate === certificate.value &&
          (option.dataset.examType
            ? option.dataset.examType === examType.value &&
              (!option.dataset.skill || option.dataset.skill === skill)
            : option.value === originalScheme && examType.value === originalType)),
    );
    scheme.replaceChildren(...allowed);
    scheme.value = allowed.some((option) => option.value === selected) ? selected : '';
  };
  const renumberSections = () =>
    sections?.querySelectorAll('.exam-section-editor').forEach((section, index) => {
      section.dataset.sectionIndex = index;
      const number = section.querySelector('.exam-section-number');
      if (number) number.textContent = String(index + 1);
    });
  const sync = () =>
    sections?.querySelectorAll('.exam-section-editor').forEach((section) => {
      const selected = new Set(
        [...section.querySelectorAll('.section-question:checked')].map(
          (input) => input.dataset.questionId,
        ),
      );
      const field = section.querySelector('.section-question-ids');
      const retained = field.value.split(',').filter((id) => selected.delete(id.trim()));
      field.value = [...retained, ...selected].join(',');
    });
  const updateQuestionVisibility = (section) => {
    const skill = section.querySelector('.section-skill')?.value;
    section.querySelectorAll('.section-question-label').forEach((label) => {
      const input = label.querySelector('.section-question');
      const visible = label.dataset.skill === skill;
      label.hidden = !visible;
      if (!visible && input) input.checked = false;
    });
  };
  sections?.querySelectorAll('.exam-section-editor').forEach(updateQuestionVisibility);
  updateSchemes();
  examType?.addEventListener('change', updateSchemes);
  certificate?.addEventListener('change', updateSchemes);
  sections?.addEventListener('change', (event) => {
    const section = event.target.closest('.exam-section-editor');
    if (section && event.target.matches('.section-skill')) {
      updateQuestionVisibility(section);
      updateSchemes();
    }
    sync();
  });
  document.getElementById('exam-sections')?.closest('form')?.addEventListener('submit', sync);
  add?.addEventListener('click', () => {
    const index = sections.querySelectorAll('.exam-section-editor').length;
    const source = sections.querySelector('.exam-section-editor');
    const clone = source.cloneNode(true);
    clone.dataset.sectionIndex = index;
    clone
      .querySelectorAll('[id]')
      .forEach((node) => (node.id = node.id.replace(/sections-\d+-/, `sections-${index}-`)));
    clone
      .querySelectorAll('[for]')
      .forEach(
        (node) => (node.htmlFor = node.htmlFor.replace(/sections-\d+-/, `sections-${index}-`)),
      );
    clone
      .querySelectorAll('[name]')
      .forEach((node) => (node.name = node.name.replace(/sections\[\d+\]/g, `sections[${index}]`)));
    clone.querySelector('.section-question-ids').value = '';
    clone.querySelectorAll('.section-question').forEach((input) => {
      input.checked = false;
      input.dataset.sectionIndex = index;
    });
    sections.appendChild(clone);
    renumberSections();
    updateQuestionVisibility(clone);
  });
})();

(() => {
  const type = document.getElementById('schemeExamType');
  const field = document.getElementById('scheme-skill-field');
  const skill = document.getElementById('schemeScopeSkill');
  const configuration = document.getElementById('scheme-score-configuration');
  const target = document.getElementById('scheme-target-score');
  const ranges = document.getElementById('scheme-combined-ranges');
  const legacy = document.getElementById('cefr-bands');
  const certificate = document.getElementById('schemeCertificate');
  const updateCefrLevels = () => {
    const general =
      certificate?.selectedOptions[0]?.dataset.certificateCode?.toUpperCase() === 'APTIS_GENERAL';
    document.querySelectorAll('[data-c2]').forEach((control) => {
      control.hidden = general;
      if (control.tagName === 'OPTION') {
        control.disabled = general;
        if (general && control.selected) control.parentElement.value = '';
      } else {
        control.querySelectorAll('input').forEach((input) => {
          input.disabled = general;
        });
      }
    });
  };
  if (!type || !field || !skill) return;
  const updateSkill = () => {
    const single = type.value === 'PRACTICE' || type.value === 'CHECKPOINT';
    field.hidden = !single;
    skill.required = single;
    if (configuration) configuration.hidden = !type.value;
    if (target) {
      target.hidden = !single;
      target.disabled = configuration?.dataset.readOnly === 'true' || target.hidden;
    }
    if (ranges) {
      ranges.hidden = !type.value;
      ranges.querySelectorAll('[data-scoring-skill]').forEach((section) => {
        section.hidden = single && section.dataset.scoringSkill !== skill.value;
        section.disabled =
          configuration?.dataset.readOnly === 'true' || section.hidden || !type.value;
      });
    }
    const combinedHelp = document.getElementById('scheme-combined-help');
    if (combinedHelp) combinedHelp.hidden = single || !type.value;
    const borderlineHelp = document.getElementById('scheme-borderline-help');
    if (borderlineHelp) borderlineHelp.hidden = single || !type.value;
    ranges?.querySelectorAll('.below-a1-row').forEach((row) => {
      row.hidden = type.value !== 'FULL_MOCK';
    });
    if (legacy) legacy.hidden = Boolean(type.value);
  };
  type.addEventListener('change', updateSkill);
  skill.addEventListener('change', updateSkill);
  certificate?.addEventListener('change', updateCefrLevels);
  updateSkill();
  updateCefrLevels();
})();
