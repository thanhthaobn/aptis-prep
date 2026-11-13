(() => {
  const examType = document.getElementById('examType');
  const targetLevel = document.getElementById('targetLevel');
  const scheme = document.getElementById('scoringSchemeId');
  const sections = document.getElementById('exam-sections');
  const sectionSkills = [...sections.querySelectorAll('.section-skill')];
  const options = [...scheme.options];

  function filterSchemes() {
    const selected = scheme.value;
    const available = options.filter((option) => {
      if (!option.value) return true;
      const { examType: type, skill } = option.dataset;
      // Legacy schemes without an exam type are kept for old records only.
      // They should not be offered when creating a new exam.
      if (!type) return option.value === selected;
      return (
        type === examType.value && (!skill || sectionSkills.every((field) => field.value === skill))
      );
    });
    scheme.replaceChildren(...available);
    scheme.value = available.some((option) => option.value === selected) ? selected : '';
  }

  examType.addEventListener('change', filterSchemes);
  for (const field of sectionSkills) {
    const section = field.closest('.exam-section-editor');

    function syncQuestionIds() {
      section.querySelector('.section-question-ids').value = [...section.querySelectorAll(
        '.section-question-label input:checked:not(:disabled)',
      )]
        .map((input) => input.value)
        .join(',');
    }

    function filterQuestions() {
      const selectedLevel = targetLevel.value;
      section.querySelectorAll('.section-question-label').forEach((label) => {
        const matchesSkill = label.dataset.skill === field.value;
        const matchesLevel = !selectedLevel || label.dataset.level === selectedLevel;
        const matches = matchesSkill && matchesLevel;
        const input = label.querySelector('input');
        label.hidden = !matches;
        input.disabled = !matches;
        if (!matches) input.checked = false;
      });
      syncQuestionIds();
    }

    field.addEventListener('change', () => {
      filterQuestions();
      filterSchemes();
    });
    section.querySelectorAll('.section-question-label input').forEach((input) => {
      input.addEventListener('change', syncQuestionIds);
    });
    filterQuestions();
  }
  targetLevel.addEventListener('change', () => {
    sectionSkills.forEach((field) => field.dispatchEvent(new Event('change')));
  });
  filterSchemes();
})();
