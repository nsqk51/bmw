# Транспортные средства — информационная система

Spring MVC (Spring Boot 3.5) + Hibernate (JPA) + PostgreSQL. Фронтенд — статические страницы на чистом JS.
Объекты: `Vehicle` и вспомогательный `Coordinates` (отдельная таблица, на одни координаты могут ссылаться несколько транспортных средств).

## Сборка (macOS)

```bash
brew install --cask temurin@17
brew install maven
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

cd vehicles
mvn clean package -DskipTests
unzip -l target/vehicles-1.0.0.jar | grep VehiclesApplication   # проверка: класс на месте
```

Готовый файл: `target/vehicles-1.0.0.jar`.

## Запуск на helios

```bash
# 1. загрузить jar (с вашего компьютера)
scp -P 2222 target/vehicles-1.0.0.jar sNNNNNN@helios.cs.ifmo.ru:~/

# 2. зайти на helios и создать run.sh по образцу deploy/run.sh.example
ssh -p 2222 sNNNNNN@helios.cs.ifmo.ru
chmod 700 run.sh

# 3. запустить в фоне
nohup ./run.sh > app.log 2>&1 &
sleep 20; tail -5 app.log           # в конце: Started VehiclesApplication
# остановить: pkill -f vehicles-1.0.0.jar
```

Открыть приложение с вашего компьютера через проброс порта:

```bash
ssh -p 2222 -L 8080:localhost:ПОРТ sNNNNNN@helios.cs.ifmo.ru
# затем http://localhost:8080  (ПОРТ — значение PORT из run.sh)
```

Таблицы и функции создаются в схеме `DB_SCHEMA` (по умолчанию — личная схема, совпадающая с логином). Если в ней уже есть таблицы прежних работ с такими же именами, создайте отдельную схему (`CREATE SCHEMA vehicles_lab;` в `psql -h pg -d studs`) и задайте `DB_SCHEMA=vehicles_lab`.

После первого запуска таблицы и функции создаются автоматически. Проверка на helios:
`psql -h pg -d studs` → `\dt` (таблицы `app_user`, `coordinates`, `vehicle`) и `\df fn_*` (пять функций).

## Запуск на своём компьютере (по желанию)

Нужен проброс порта к БД: `ssh -N -L 15432:localhost:5432 -p 2222 sNNNNNN@helios.cs.ifmo.ru`, затем
`DB_HOST=localhost DB_PORT=15432 DB_USER=sNNNNNN DB_PASSWORD=... mvn spring-boot:run` → http://localhost:8080.

## Структура

| Пакет | Назначение |
|---|---|
| `model` | JPA-сущности `Vehicle`, `Coordinates`, `AppUser`, enum'ы `VehicleType`, `FuelType`; ограничения через Bean Validation |
| `repository` | Spring Data JPA; вызовы функций БД для спецопераций |
| `service` | бизнес-логика, валидаторы, фильтры (`VehicleSpecs`), спецоперации |
| `web` | REST-контроллеры (Spring MVC) |
| `dto`, `error`, `event`, `config` | DTO, ошибки, SSE-рассылка изменений, безопасность |

## Соответствие заданию

- Ограничения полей: Bean Validation на сущностях + `CHECK`, `NOT NULL`, `FOREIGN KEY` в `schema.sql`; `id` генерирует БД (`BIGSERIAL`).
- Спецоперации — функции БД `fn_*` в `schema.sql`, вызываются из `SpecialOperationsService` через `VehicleRepository`.
- Фильтрация таблицы — только по полному совпадению (`VehicleSpecs`); сортировка по любой колонке; пагинация.
- Удаление координат, на которые ссылается транспортное средство, отменяется (ответ 409 и сообщение пользователю).
- Изменения синхронизируются между пользователями через SSE после коммита транзакции.
- В `schema.sql` команды разделены строкой-разделителем (`spring.sql.init.separator`): в теле функций есть `;`.
