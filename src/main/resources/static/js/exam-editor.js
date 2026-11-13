(() => {
  const examType = document.getElementById('examType');
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
    function filterQuestions() {
      section.querySelectorAll('.section-question-label').forEach((label) => {
        const matchesSkill = label.dataset.skill === field.value;
        label.hidden = !matchesSkill;
        label.querySelector('input').disabled = !matchesSkill;
      });
    }
    field.addEventListener('change', () => {
      filterQuestions();
      filterSchemes();
    });
    filterQuestions();
  }
  filterSchemes();
})();
