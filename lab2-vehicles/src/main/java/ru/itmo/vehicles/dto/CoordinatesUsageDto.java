package ru.itmo.vehicles.dto;

import ru.itmo.vehicles.model.Coordinates;

/** Координаты вместе с числом транспортных средств, которые на них ссылаются. */
public record CoordinatesUsageDto(Long id, long x, long y, long usedBy) {

    public static CoordinatesUsageDto from(Coordinates c, long usedBy) {
        return new CoordinatesUsageDto(c.getId(), c.getX(), c.getY(), usedBy);
    }
}
