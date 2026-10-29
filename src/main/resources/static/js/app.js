(function () {
  // ---- theme ----
  var root = document.documentElement;
  try { var saved = localStorage.getItem('theme'); if (saved) root.setAttribute('data-theme', saved); } catch (e) {}
  window.toggleTheme = function () {
    var next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
    root.setAttribute('data-theme', next);
    try { localStorage.setItem('theme', next); } catch (e) {}
  };

  function fillChips(el, list, cls, emptyText) {
    el.textContent = '';
    if (!list.length) {
      var p = document.createElement('div');
      p.className = 'board-empty'; p.textContent = emptyText; el.appendChild(p); return;
    }
    list.forEach(function (code) {
      var s = document.createElement('span');
      s.className = 'tok ' + (cls || ''); s.textContent = code; el.appendChild(s);
    });
  }

  // ---- public live board ----
  var board = document.getElementById('board');
  if (board) {
    var load = function () {
      fetch('/api/queue', { cache: 'no-store' }).then(function (r) { return r.json(); }).then(function (d) {
        fillChips(document.getElementById('col-preparing'), d.preparing, '', 'Nothing cooking right now');
        fillChips(document.getElementById('col-ready'), d.ready, '', 'No orders waiting at the counter');
        fillChips(document.getElementById('col-waiting'), d.waiting, 'dim', 'No one in the queue');
        var c = document.getElementById('clock');
        if (c) c.textContent = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      }).catch(function () {});
    };
    load();
    setInterval(load, 4000);
  }

  // ---- order page: poll status ----
  var tracker = document.getElementById('tracker');
  if (tracker) {
    var url = tracker.getAttribute('data-url');
    var current = tracker.getAttribute('data-status');
    setInterval(function () {
      fetch(url, { cache: 'no-store' }).then(function (r) { return r.json(); }).then(function (d) {
        if (d.status !== current) { window.location.reload(); return; }
        var pos = document.getElementById('pos'), eta = document.getElementById('eta');
        if (pos) pos.textContent = d.position;
        if (eta) eta.textContent = d.eta;
      }).catch(function () {});
    }, 6000);
  }

  // ---- kitchen: auto refresh when idle ----
  if (document.body.hasAttribute('data-autorefresh')) {
    setInterval(function () {
      var tag = document.activeElement && document.activeElement.tagName;
      if (tag !== 'INPUT' && tag !== 'SELECT' && tag !== 'TEXTAREA') window.location.reload();
    }, 20000);
  }
})();
