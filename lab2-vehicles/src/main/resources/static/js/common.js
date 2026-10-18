'use strict';

const VEHICLE_TYPES = ['CAR', 'BICYCLE', 'MOTORCYCLE', 'CHOPPER', 'HOVERBOARD'];
const FUEL_TYPES = ['KEROSENE', 'DIESEL', 'ALCOHOL', 'ANTIMATTER'];

/* Смайлики только для красоты: на сервер по-прежнему уходят значения enum. */
const TYPE_ICONS = { CAR: '🚗', BICYCLE: '🚲', MOTORCYCLE: '🏍️', CHOPPER: '🚁', HOVERBOARD: '🛹' };
const FUEL_ICONS = { KEROSENE: '🛢️', DIESEL: '⛽', ALCOHOL: '🍺', ANTIMATTER: '🌌' };

function iconOf(value) { return TYPE_ICONS[value] || FUEL_ICONS[value] || ''; }

/** Цветной значок для типа или топлива. */
function badge(value) {
  if (value == null || value === '') return '';
  return `<span class="badge b-${esc(value)}">${iconOf(value)} ${esc(value)}</span>`;
}

/** Варианты для select: значение — enum, подпись — со смайликом. */
function enumOptions(list) {
  return list.map(v => `<option value="${v}">${iconOf(v)} ${v}</option>`).join('');
}

class ApiError extends Error {
  constructor(status, data) {
    super((data && data.message) || ('Ошибка ' + status));
    this.status = status;
    this.data = data;
  }
  /** Список понятных сообщений для показа пользователю. */
  get messages() {
    if (this.data && Array.isArray(this.data.errors) && this.data.errors.length) {
      return this.data.errors.map(e => e.message);
    }
    return [this.message];
  }
}

async function api(method, url, body) {
  const res = await fetch(url, {
    method,
    headers: body !== undefined ? { 'Content-Type': 'application/json' } : {},
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  if (res.status === 401) {
    location.href = '/login.html';
    throw new ApiError(401, { message: 'Требуется вход в систему' });
  }
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch (e) { data = { message: text }; }
  if (!res.ok) throw new ApiError(res.status, data);
  return data;
}

function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function fmtDate(s) {
  return s ? new Date(s).toLocaleString('ru-RU') : '';
}

function showErrors(ul, messages) {
  ul.innerHTML = messages.map(m => '<li>' + esc(m) + '</li>').join('');
}

/* ---------- меню ---------- */

function renderMenu(active) {
  const items = [['index.html', '🚘 Гараж'], ['coordinates.html', '📍 Координаты'], ['special.html', '🧪 Спецоперации']];
  document.getElementById('menu').innerHTML =
    '<span class="car" aria-hidden="true">🚙</span>' +
    '<a href="/index.html" class="brand"><span class="wheel">🛞</span>Врум</a>' +
    items.map(([href, title]) => `<a href="/${href}" class="${href === active ? 'active' : ''}">${title}</a>`).join('') +
    '<span class="spacer"></span><span id="whoami" class="chip"></span><a href="#" id="logout">Выход</a>';
  api('GET', '/api/auth/me').then(u => { document.getElementById('whoami').textContent = u.username; }).catch(() => {});
  document.getElementById('logout').onclick = async (e) => {
    e.preventDefault();
    await fetch('/logout', { method: 'POST' });
    location.href = '/login.html';
  };
}

/* ---------- изменения в реальном времени (SSE) ---------- */

function subscribeChanges(handler) {
  let timer;
  const es = new EventSource('/api/events');
  es.addEventListener('change', ev => {
    clearTimeout(timer);
    timer = setTimeout(() => handler(JSON.parse(ev.data)), 150);
  });
  return es;
}

/* ---------- карточки объектов ---------- */

function coordinatesCardHtml(c) {
  return `<dl>
    <dt>ID</dt><dd>${c.id}</dd>
    <dt>X</dt><dd>${c.x}</dd>
    <dt>Y</dt><dd>${c.y}</dd></dl>`;
}

function opt(v) {
  return v == null ? '—' : esc(v);
}

function vehicleCardHtml(v) {
  return `<h3>Транспортное средство #${v.id}</h3><dl>
    <dt>Имя</dt><dd>${esc(v.name)}</dd>
    <dt>Создано</dt><dd>${fmtDate(v.creationDate)}</dd>
    <dt>Тип</dt><dd>${badge(v.type)}</dd>
    <dt>Мощность двигателя</dt><dd>${opt(v.enginePower)}</dd>
    <dt>Число колёс</dt><dd>${v.numberOfWheels}</dd>
    <dt>Вместимость</dt><dd>${v.capacity}</dd>
    <dt>Пройденное расстояние</dt><dd>${opt(v.distanceTravelled)}</dd>
    <dt>Расход топлива</dt><dd>${v.fuelConsumption}</dd>
    <dt>Тип топлива</dt><dd>${badge(v.fuelType)}</dd></dl>
    <h4>Связанный объект: координаты</h4>${coordinatesCardHtml(v.coordinates)}`;
}

/** Компактная таблица результатов (для спецопераций). */
function vehiclesTableHtml(list) {
  if (list.length === 0) return 'Ничего не нашлось 🕵️ Попробуйте другое значение';
  const rows = list.map(v => `<tr><td>${v.id}</td><td>${esc(v.name)}</td><td>${badge(v.type)}</td>
    <td>${v.numberOfWheels}</td><td>${v.fuelConsumption}</td><td>${badge(v.fuelType)}</td>
    <td>#${v.coordinates.id} (${v.coordinates.x}; ${v.coordinates.y})</td></tr>`).join('');
  return `<div>Найдено: ${list.length}</div><div class="tablewrap"><table>
    <thead><tr><th>ID</th><th>Имя</th><th>Тип</th><th>Колёс</th><th>Расход</th><th>Топливо</th><th>Координаты</th></tr></thead>
    <tbody>${rows}</tbody></table></div>`;
}

/* ---------- форма транспортного средства ---------- */

function vehicleFieldsHtml() {
  const opts = enumOptions;
  return `
    <label>Имя<input name="name" maxlength="255" required></label>
    <label>Координаты
      <select name="coordMode">
        <option value="existing">Связать с существующими</option>
        <option value="new">Создать новые</option>
      </select>
    </label>
    <div data-coord="existing">
      <label>Существующие координаты<select name="coordinatesId"></select></label>
    </div>
    <div data-coord="new">
      <label>Координата X (целое число)<input name="x" type="number" step="1"></label>
      <label>Координата Y (целое число, больше -913)<input name="y" type="number" step="1"></label>
    </div>
    <label>Тип<select name="type" required>${opts(VEHICLE_TYPES)}</select></label>
    <label>Мощность двигателя (больше 0, необязательно)<input name="enginePower" type="number" step="any" min="0"></label>
    <label>Число колёс (целое, больше 0)<input name="numberOfWheels" type="number" step="1" min="1" required></label>
    <label>Вместимость (больше 0)<input name="capacity" type="number" step="any" min="0" required></label>
    <label>Пройденное расстояние (больше 0, необязательно)<input name="distanceTravelled" type="number" step="any" min="0"></label>
    <label>Расход топлива (больше 0)<input name="fuelConsumption" type="number" step="any" min="0" required></label>
    <label>Тип топлива<select name="fuelType" required>${opts(FUEL_TYPES)}</select></label>`;
}

/** Показывает блок «существующие» или «новые» координаты в зависимости от выбранного режима. */
function syncCoordMode(form) {
  const mode = form.elements.coordMode.value;
  form.querySelectorAll('[data-coord]').forEach(div => { div.hidden = div.dataset.coord !== mode; });
}

function num(v) {
  return v === '' || v == null ? null : Number(v);
}

function readVehicle(form) {
  const e = form.elements; // form.name — это имя самой формы, поэтому обращаемся через elements
  const existing = e.coordMode.value === 'existing';
  return {
    name: e.name.value,
    coordinatesId: existing ? num(e.coordinatesId.value) : null,
    x: existing ? null : num(e.x.value),
    y: existing ? null : num(e.y.value),
    type: e.type.value || null,
    enginePower: num(e.enginePower.value),
    numberOfWheels: num(e.numberOfWheels.value),
    capacity: num(e.capacity.value),
    distanceTravelled: num(e.distanceTravelled.value),
    fuelConsumption: num(e.fuelConsumption.value),
    fuelType: e.fuelType.value || null,
  };
}

/** Заполняет форму; для существующего объекта выбираются его координаты. */
function fillVehicle(form, v) {
  const e = form.elements;
  e.name.value = v ? v.name : '';
  e.x.value = '';
  e.y.value = '';
  e.type.value = v ? v.type : VEHICLE_TYPES[0];
  e.enginePower.value = v && v.enginePower != null ? v.enginePower : '';
  e.numberOfWheels.value = v ? v.numberOfWheels : '';
  e.capacity.value = v ? v.capacity : '';
  e.distanceTravelled.value = v && v.distanceTravelled != null ? v.distanceTravelled : '';
  e.fuelConsumption.value = v ? v.fuelConsumption : '';
  e.fuelType.value = v ? v.fuelType : FUEL_TYPES[0];
  if (v) e.coordinatesId.value = String(v.coordinates.id);
  syncCoordMode(form);
}

/** Проверка на клиенте: возвращает список сообщений об ошибках. */
function validateVehicle(v) {
  const errs = [];
  const isInt = n => Number.isSafeInteger(n);
  const positive = n => Number.isFinite(n) && n > 0;
  if (!v.name || !v.name.trim()) errs.push('Имя не может быть пустым');
  if (v.coordinatesId == null) {
    if (v.x == null || !isInt(v.x)) errs.push('Координата X обязательна и должна быть целым числом');
    if (v.y == null || !isInt(v.y)) errs.push('Координата Y обязательна и должна быть целым числом');
    else if (!(v.y > -913)) errs.push('Координата Y должна быть больше -913');
  }
  if (!v.type) errs.push('Выберите тип');
  if (v.enginePower != null && !positive(v.enginePower)) errs.push('Мощность двигателя должна быть больше 0 (или не задана)');
  if (v.numberOfWheels == null || !isInt(v.numberOfWheels) || v.numberOfWheels <= 0) errs.push('Число колёс должно быть целым числом больше 0');
  if (v.capacity == null || !positive(v.capacity)) errs.push('Вместимость должна быть больше 0');
  if (v.distanceTravelled != null && !positive(v.distanceTravelled)) errs.push('Пройденное расстояние должно быть больше 0 (или не задано)');
  if (v.fuelConsumption == null || !positive(v.fuelConsumption)) errs.push('Расход топлива должен быть больше 0');
  if (!v.fuelType) errs.push('Выберите тип топлива');
  return errs;
}

/** Заполняет select существующими координатами. Возвращает их список. */
async function fillCoordinatesSelect(select, selected) {
  const list = await api('GET', '/api/coordinates');
  select.innerHTML = list.map(c => `<option value="${c.id}">#${c.id} — (${c.x}; ${c.y})</option>`).join('');
  if (selected != null && list.some(c => c.id === selected)) select.value = String(selected);
  return list;
}
