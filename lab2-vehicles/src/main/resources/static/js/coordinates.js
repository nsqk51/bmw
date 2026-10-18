'use strict';

const $ = id => document.getElementById(id);
let editingId = null;

async function load() {
  try {
    const list = await api('GET', '/api/coordinates');
    $('tbody').innerHTML = list.length === 0
      ? '<tr><td colspan="5" class="muted">Координат пока нет</td></tr>'
      : list.map(c => `<tr>
          <td>${c.id}</td><td>${c.x}</td><td>${c.y}</td><td>${c.usedBy}</td>
          <td class="actions">
            <button data-act="edit" data-id="${c.id}">Изменить</button>
            <button data-act="delete" data-id="${c.id}" class="danger">Удалить</button>
          </td></tr>`).join('');
  } catch (err) {
    $('tbody').innerHTML = `<tr><td colspan="5" class="muted">${esc(err.message)}</td></tr>`;
  }
}

$('tbody').onclick = (ev) => {
  const btn = ev.target.closest('button[data-act]');
  if (!btn) return;
  const id = Number(btn.dataset.id);
  if (btn.dataset.act === 'edit') openEdit(id);
  if (btn.dataset.act === 'delete') removeCoordinates(id);
};

/* ---------- создание / изменение ---------- */

function openDialog(c) {
  editingId = c ? c.id : null;
  $('dlgTitle').textContent = c ? `Изменение координат #${c.id}` : 'Новые координаты';
  const e = $('coordForm').elements;
  e.x.value = c ? c.x : '';
  e.y.value = c ? c.y : '';
  showErrors($('formErrors'), []);
  $('coordDialog').showModal();
}

async function openEdit(id) {
  try {
    openDialog(await api('GET', '/api/coordinates/' + id));
  } catch (err) {
    alert(err.message);
  }
}

$('btnCreate').onclick = () => openDialog(null);
$('btnCancel').onclick = () => $('coordDialog').close();

$('coordForm').onsubmit = async (ev) => {
  ev.preventDefault();
  const e = ev.target.elements;
  const body = { x: num(e.x.value), y: num(e.y.value) };
  const errs = [];
  if (body.x == null || !Number.isSafeInteger(body.x)) errs.push('Координата X обязательна и должна быть целым числом');
  if (body.y == null || !Number.isSafeInteger(body.y)) errs.push('Координата Y обязательна и должна быть целым числом');
  else if (!(body.y > -913)) errs.push('Координата Y должна быть больше -913');
  showErrors($('formErrors'), errs);
  if (errs.length) return;
  try {
    if (editingId) await api('PUT', '/api/coordinates/' + editingId, body);
    else await api('POST', '/api/coordinates', body);
    $('coordDialog').close();
    load();
  } catch (err) {
    showErrors($('formErrors'), err.messages);
  }
};

/* ---------- удаление ---------- */

async function removeCoordinates(id) {
  if (!confirm(`Удалить координаты #${id}?`)) return;
  try {
    await api('DELETE', '/api/coordinates/' + id);
    load();
  } catch (err) {
    // если координаты связаны с транспортными средствами, сервер отменяет удаление и сообщает причину
    alert(err.message);
  }
}

renderMenu('coordinates.html');
load();
subscribeChanges(() => load());
