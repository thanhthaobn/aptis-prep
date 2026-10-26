// Page behavior; form values and URLs come from the rendered HTML.
document.addEventListener('DOMContentLoaded', function () {
  var toggleBtn = document.getElementById('navToggle');
  var primaryNav = document.getElementById('primaryNav');
  if (toggleBtn && primaryNav) {
    toggleBtn.addEventListener('click', function () {
      var expanded = toggleBtn.getAttribute('aria-expanded') === 'true';
      toggleBtn.setAttribute('aria-expanded', String(!expanded));
      primaryNav.classList.toggle('is-open');
    });
    document.addEventListener('click', function (e) {
      if (
        !toggleBtn.contains(e.target) &&
        !primaryNav.contains(e.target) &&
        primaryNav.classList.contains('is-open')
      ) {
        primaryNav.classList.remove('is-open');
        toggleBtn.setAttribute('aria-expanded', 'false');
      }
    });
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && primaryNav.classList.contains('is-open')) {
        primaryNav.classList.remove('is-open');
        toggleBtn.setAttribute('aria-expanded', 'false');
        toggleBtn.focus();
      }
    });
  }
});
