// Preview only: no HTTP requests or account mutations. Native dialog provides
// Escape dismissal and focus containment; the trigger regains focus on close.
const accountDialog = document.getElementById('studentAccountDialog');
const accountTrigger = document.querySelector('[data-open-student-dialog]');
if (accountDialog && accountTrigger) {
    accountTrigger.addEventListener('click', () => accountDialog.showModal());
    accountDialog.addEventListener('close', () => accountTrigger.focus());
}
