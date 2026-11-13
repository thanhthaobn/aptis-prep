(function () {
  'use strict';

  function init() {
    var form = document.getElementById('account-unlock-form');
    if (!form) return;

    var password = document.getElementById('unlock-password');
    var toggle = form.querySelector('.account-password-toggle');
    if (password && toggle) {
      toggle.addEventListener('click', function () {
        var visible = password.type === 'text';
        password.type = visible ? 'password' : 'text';
        toggle.setAttribute('aria-pressed', String(!visible));
        var label = toggle.querySelector('.visually-hidden');
        if (label) label.textContent = visible ? 'Hiện mật khẩu' : 'Ẩn mật khẩu';
      });
    }

    var message = document.getElementById('unlock-message');
    var counter = document.getElementById('unlock-message-count');
    if (message && counter) {
      var updateCounter = function () {
        counter.textContent = message.value.length + '/' + message.maxLength;
      };
      message.addEventListener('input', updateCounter);
      updateCounter();
    }

    var submit = form.querySelector('.account-submit-button');
    form.addEventListener('submit', function () {
      if (!submit || submit.disabled) return;
      submit.disabled = true;
      submit.textContent = 'Đang gửi...';
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
