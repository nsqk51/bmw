package ru.itmo.vehicles.dto;

/** Данные для создания/изменения координат. Типы-обёртки нужны, чтобы отличать «не задано» от нуля. */
public record CoordinatesRequest(Long x, Long y) {
}
