-- Команды разделены нестандартным разделителем (параметр spring.sql.init.separator в application.properties):
-- в теле функций есть символ точки с запятой, поэтому обычный разделитель не подходит.

CREATE TABLE IF NOT EXISTS app_user (
    id       BIGSERIAL PRIMARY KEY,
    username VARCHAR(32)  NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL
)
^^^ END OF STATEMENT

CREATE TABLE IF NOT EXISTS coordinates (
    id BIGSERIAL PRIMARY KEY CHECK (id > 0),
    x  BIGINT NOT NULL,
    y  BIGINT NOT NULL CHECK (y > -913)
)
^^^ END OF STATEMENT

CREATE TABLE IF NOT EXISTS vehicle (
    id                 BIGSERIAL PRIMARY KEY CHECK (id > 0),
    name               VARCHAR(255)     NOT NULL CHECK (length(trim(name)) > 0),
    coordinates_id     BIGINT           NOT NULL REFERENCES coordinates (id) ON DELETE RESTRICT,
    creation_date      TIMESTAMP        NOT NULL DEFAULT now(),
    vehicle_type       VARCHAR(20)      NOT NULL CHECK (vehicle_type IN ('CAR', 'BICYCLE', 'MOTORCYCLE', 'CHOPPER', 'HOVERBOARD')),
    engine_power       DOUBLE PRECISION CHECK (engine_power > 0),
    number_of_wheels   BIGINT           NOT NULL CHECK (number_of_wheels > 0),
    capacity           DOUBLE PRECISION NOT NULL CHECK (capacity > 0),
    distance_travelled DOUBLE PRECISION CHECK (distance_travelled > 0),
    fuel_consumption   DOUBLE PRECISION NOT NULL CHECK (fuel_consumption > 0),
    fuel_type          VARCHAR(20)      NOT NULL CHECK (fuel_type IN ('KEROSENE', 'DIESEL', 'ALCOHOL', 'ANTIMATTER'))
)
^^^ END OF STATEMENT

CREATE INDEX IF NOT EXISTS idx_vehicle_coordinates ON vehicle (coordinates_id)
^^^ END OF STATEMENT

-- ===== Специальные операции: функции БД, вызываются из уровня бизнес-логики =====

-- 1. Сумма значений fuelConsumption для всех объектов
CREATE OR REPLACE FUNCTION fn_sum_fuel_consumption()
RETURNS DOUBLE PRECISION
LANGUAGE sql STABLE
AS $$
    SELECT COALESCE(SUM(fuel_consumption), 0) FROM vehicle
$$
^^^ END OF STATEMENT

-- 2. Количество объектов, у которых fuelType меньше заданного (порядок констант enum FuelType)
CREATE OR REPLACE FUNCTION fn_count_fuel_type_less(p_fuel_type TEXT)
RETURNS BIGINT
LANGUAGE sql STABLE
AS $$
    SELECT COUNT(*)
    FROM vehicle
    WHERE array_position(ARRAY['KEROSENE', 'DIESEL', 'ALCOHOL', 'ANTIMATTER'], fuel_type::text)
        < array_position(ARRAY['KEROSENE', 'DIESEL', 'ALCOHOL', 'ANTIMATTER'], p_fuel_type)
$$
^^^ END OF STATEMENT

-- 3. Объекты, имя которых содержит заданную подстроку (с учётом регистра)
CREATE OR REPLACE FUNCTION fn_find_vehicles_by_name(p_substring TEXT)
RETURNS SETOF vehicle
LANGUAGE sql STABLE
AS $$
    SELECT * FROM vehicle WHERE strpos(name, p_substring) > 0 ORDER BY id
$$
^^^ END OF STATEMENT

-- 4. Все транспортные средства заданного типа
CREATE OR REPLACE FUNCTION fn_find_vehicles_by_type(p_type TEXT)
RETURNS SETOF vehicle
LANGUAGE sql STABLE
AS $$
    SELECT * FROM vehicle WHERE vehicle_type = p_type ORDER BY id
$$
^^^ END OF STATEMENT

-- 5. Все транспортные средства с числом колёс в заданном диапазоне (включительно)
CREATE OR REPLACE FUNCTION fn_find_vehicles_by_wheels(p_min BIGINT, p_max BIGINT)
RETURNS SETOF vehicle
LANGUAGE sql STABLE
AS $$
    SELECT * FROM vehicle WHERE number_of_wheels BETWEEN p_min AND p_max ORDER BY id
$$
^^^ END OF STATEMENT
