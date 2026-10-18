package ru.itmo.vehicles.dto;

/** Фильтры по строковым колонкам таблицы (только полное совпадение). */
public record VehicleFilter(String name, String type, String fuelType) {
}
