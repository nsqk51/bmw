'use strict';

// filter: 'text' — поле ввода, 'select' — выбор из списка значений (фильтрация только по полному совпадению)
const COLUMNS = [
  { key: 'id', label: 'ID' },
  { key: 'name', label: 'Имя', filter: 'text' },
  { key: 'coordinatesId', label: 'ID коорд.' },
  { key: 'x', label: 'X' },
  { key: 'y', label: 'Y' },
  { key: 'creationDate', label: 'Создано' },
  { key: 'type', label: 'Тип', filter: 'select', values: VEHICLE_TYPES },
  { key: 'enginePower', label: 'Мощность' },
  { key: 'numberOfWheels', label: 'Колёс' },
  { key: 'capacity', label: 'Вместимость' },
  { key: 'distanceTravelled', label: 'Пройдено' },
  { key: 'fuelConsumption', label: 'Расход' },
  { key: 'fuelType', label: 'Топливо', filter: 'select', values: FUEL_TYPES },
];

const state = { page: 0, size: 10, sort: 'id', dir: 'asc', filters: {} };
let editingId = null;
let infoId = null;

const $ = id => document.getElementById(id);

function cellValue(v, key) {
  switch (key) {
    case 'coordinatesId': return v.coordinates.id;
    case 'x': return v.coordinates.x;
    case 'y': return v.coordinates.y;
    case 'creationDate': return fmtDate(v.creationDate);
    default: return v[key] ?? '';
  }
}

/* ---------- таблица ---------- */

function renderHeader() {
  const sorts = COLUMNS.map(c => `<th data-sort="${c.key}">${c.label}<span class="arrow"></span></th>`).join('');
  const filters = COLUMNS.map(c => {
    if (c.filter === 'text') return `<th><input data-filter="${c.key}" placeholder="точное значение"></th>`;
    if (c.filter === 'select') {
      return `<th><select data-filter="${c.key}"><option value="">все</option>${
        enumOptions(c.values)}</select></th>`;
    }
    return '<th></th>';
  }).join('');
  $('thead').innerHTML = `<tr class="sorts">${sorts}<th>Действия</th></tr><tr class="filters">${filters}<th></th></tr>`;

  $('thead').querySelectorAll('th[data-sort]').forEach(th => {
    th.onclick = () => {
      const key = th.dataset.sort;
      if (state.sort === key) state.dir = state.dir === 'asc' ? 'desc' : 'asc';
      else { state.sort = key; state.dir = 'asc'; }
      state.page = 0;
      updateArrows();
      load();
    };
  });

  let timer;
  const apply = control => {
    clearTimeout(timer);
    timer = setTimeout(() => {
      state.filters[control.dataset.filter] = control.value.trim();
      state.page = 0;
      load();
    }, 300);
  };
  $('thead').querySelectorAll('input[data-filter]').forEach(input => { input.oninput = () => apply(input); });
  $('thead').querySelectorAll('select[data-filter]').forEach(select => { select.onchange = () => apply(select); });
  updateArrows();
}

function updateArrows() {
  $('thead').querySelectorAll('th[data-sort]').forEach(th => {
    th.querySelector('.arrow').textContent =
      th.dataset.sort === state.sort ? (state.dir === 'asc' ? ' ▲' : ' ▼') : '';
  });
}

async function load() {
  const params = new URLSearchParams({ page: state.page, size: state.size, sort: state.sort, dir: state.dir });
  for (const [k, v] of Object.entries(state.filters)) if (v) params.set(k, v);
  try {
    const data = await api('GET', '/api/vehicles?' + params);
    if (data.totalPages > 0 && data.page >= data.totalPages) {
      state.page = data.totalPages - 1;
      return load();
    }
    renderRows(data);
  } catch (err) {
    $('tbody').innerHTML = `<tr><td colspan="${COLUMNS.length + 1}" class="muted">${esc(err.message)}</td></tr>`;
  }
}

function renderRows(data) {
  if (data.items.length === 0) {
    $('tbody').innerHTML = `<tr><td colspan="${COLUMNS.length + 1}" class="empty">Тут пока пусто 🅿️ Добавьте первое транспортное средство</td></tr>`;
  } else {
    $('tbody').innerHTML = data.items.map(v => `<tr>
      ${COLUMNS.map(c => `<td>${c.key === 'type' || c.key === 'fuelType' ? badge(v[c.key]) : esc(cellValue(v, c.key))}</td>`).join('')}
      <td class="actions">
        <button data-act="info" data-id="${v.id}">Инфо</button>
        <button data-act="edit" data-id="${v.id}">Изменить</button>
        <button data-act="delete" data-id="${v.id}" class="danger">Удалить</button>
      </td></tr>`).join('');
  }
  $('pageInfo').textContent = `Страница ${data.totalPages === 0 ? 0 : data.page + 1} из ${data.totalPages}`;
  $('total').textContent = `Всего: ${data.total}`;
  $('prev').disabled = data.page <= 0;
  $('next').disabled = data.page + 1 >= data.totalPages;
}

$('tbody').onclick = (ev) => {
  const btn = ev.target.closest('button[data-act]');
  if (!btn) return;
  const id = Number(btn.dataset.id);
  if (btn.dataset.act === 'info') openInfo(id);
  if (btn.dataset.act === 'edit') openEdit(id);
  if (btn.dataset.act === 'delete') removeVehicle(id);
};

$('prev').onclick = () => { state.page--; load(); };
$('next').onclick = () => { state.page++; load(); };
$('pageSize').onchange = (e) => { state.size = Number(e.target.value); state.page = 0; load(); };

/* ---------- создание / изменение ---------- */

async function openVehicleDialog(vehicle) {
  editingId = vehicle ? vehicle.id : null;
  $('dlgTitle').textContent = vehicle ? `Изменение транспортного средства #${vehicle.id}` : 'Новое транспортное средство';
  showErrors($('formErrors'), []);
  const form = $('vehicleForm');
  let existing;
  try {
    existing = await fillCoordinatesSelect(form.elements.coordinatesId, vehicle ? vehicle.coordinates.id : null);
  } catch (err) {
    alert(err.message);
    return;
  }
  fillVehicle(form, vehicle);
  // если подходящих существующих координат ещё нет, сразу предлагаем создать новые
  form.elements.coordMode.value = vehicle || existing.length > 0 ? 'existing' : 'new';
  syncCoordMode(form);
  $('vehicleDialog').showModal();
}

async function openEdit(id) {
  try {
    openVehicleDialog(await api('GET', '/api/vehicles/' + id));
  } catch (err) {
    alert(err.message);
  }
}

$('btnCreate').onclick = () => openVehicleDialog(null);
$('btnCancel').onclick = () => $('vehicleDialog').close();

$('vehicleForm').onsubmit = async (ev) => {
  ev.preventDefault();
  const vehicle = readVehicle(ev.target);
  const errs = validateVehicle(vehicle);
  showErrors($('formErrors'), errs);
  if (errs.length) return;
  try {
    if (editingId) await api('PUT', '/api/vehicles/' + editingId, vehicle);
    else await api('POST', '/api/vehicles', vehicle);
    $('vehicleDialog').close();
    load();
  } catch (err) {
    showErrors($('formErrors'), err.messages);
  }
};

/* ---------- информация по ID ---------- */

async function openInfo(id) {
  try {
    const vehicle = await api('GET', '/api/vehicles/' + id);
    infoId = id;
    $('infoBody').innerHTML = vehicleCardHtml(vehicle);
    $('infoDialog').showModal();
  } catch (err) {
    alert(err.message);
  }
}

$('btnFind').onclick = () => {
  const id = Number($('idInput').value);
  if (!Number.isInteger(id) || id <= 0) { alert('Введите целый ID больше 0'); return; }
  openInfo(id);
};
$('idInput').onkeydown = (e) => { if (e.key === 'Enter') $('btnFind').click(); };
$('infoClose').onclick = () => $('infoDialog').close();
$('infoEdit').onclick = () => { $('infoDialog').close(); openEdit(infoId); };

/* ---------- удаление ---------- */

async function removeVehicle(id) {
  if (!confirm(`Удалить транспортное средство #${id}?`)) return;
  try {
    await api('DELETE', '/api/vehicles/' + id);
    load();
  } catch (err) {
    alert(err.message);
  }
}

/* ---------- старт ---------- */

renderMenu('index.html');
$('vehicleFields').innerHTML = vehicleFieldsHtml();
$('vehicleForm').elements.coordMode.onchange = () => syncCoordMode($('vehicleForm'));
renderHeader();
load();
subscribeChanges(() => load()); // изменения других пользователей подтягиваются автоматически
