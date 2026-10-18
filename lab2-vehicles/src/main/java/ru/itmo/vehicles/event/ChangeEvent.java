package ru.itmo.vehicles.event;

/** Событие изменения данных: entity — vehicle/coordinates, action — created/updated/deleted. */
public record ChangeEvent(String entity, String action, Long id) {
}
