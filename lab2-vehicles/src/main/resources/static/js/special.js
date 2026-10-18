'use strict';

const $ = id => document.getElementById(id);

function show(el, html, isError) {
  el.className = 'result' + (isError ? ' error' : '');
  el.innerHTML = html;
}

async function run(resultEl, request, render) {
  try {
    show(resultEl, render(await request()));
  } catch (err) {
    show(resultEl, err.messages.map(esc).join('<br>'), true);
  }
}

const options = enumOptions;
$('lessForm').elements.fuelType.innerHTML = options(FUEL_TYPES);
$('typeForm').elements.type.innerHTML = options(VEHICLE_TYPES);

/* ---------- 1. сумма fuelConsumption ---------- */
$('btnSum').onclick = () => run($('resSum'), () => api('GET', '/api/special/fuel-sum'),
  r => `Сумма: <b>${r.sum}</b>`);

/* ---------- 2. fuelType меньше заданного ---------- */
$('lessForm').onsubmit = (ev) => {
  ev.preventDefault();
  const fuelType = ev.target.elements.fuelType.value;
  run($('resLess'), () => api('GET', '/api/special/fuel-less?fuelType=' + encodeURIComponent(fuelType)),
    r => `Количество объектов с fuelType меньше ${fuelType}: <b>${r.count}</b>`);
};

/* ---------- 3. имя содержит подстроку ---------- */
$('nameForm').onsubmit = (ev) => {
  ev.preventDefault();
  const substring = ev.target.elements.substring.value;
  if (substring === '') { show($('resName'), 'Подстрока не должна быть пустой', true); return; }
  run($('resName'), () => api('GET', '/api/special/by-name?substring=' + encodeURIComponent(substring)), vehiclesTableHtml);
};

/* ---------- 4. заданный тип ---------- */
$('typeForm').onsubmit = (ev) => {
  ev.preventDefault();
  const type = ev.target.elements.type.value;
  run($('resType'), () => api('GET', '/api/special/by-type?type=' + encodeURIComponent(type)), vehiclesTableHtml);
};

/* ---------- 5. число колёс в диапазоне ---------- */
$('wheelsForm').onsubmit = (ev) => {
  ev.preventDefault();
  const e = ev.target.elements;
  const min = e.min.value === '' ? null : Number(e.min.value);
  const max = e.max.value === '' ? null : Number(e.max.value);
  if (min == null || max == null || !Number.isSafeInteger(min) || !Number.isSafeInteger(max)) {
    show($('resWheels'), 'Укажите обе границы диапазона целыми числами', true);
    return;
  }
  if (min > max) { show($('resWheels'), 'Нижняя граница не может быть больше верхней', true); return; }
  run($('resWheels'), () => api('GET', `/api/special/by-wheels?min=${min}&max=${max}`), vehiclesTableHtml);
};

renderMenu('special.html');
