package ru.itmo.vehicles.dto;

import ru.itmo.vehicles.model.Coordinates;

public record CoordinatesDto(Long id, long x, long y) {

    public static CoordinatesDto from(Coordinates c) {
        return new CoordinatesDto(c.getId(), c.getX(), c.getY());
    }
}
