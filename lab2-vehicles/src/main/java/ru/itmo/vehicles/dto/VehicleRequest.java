package ru.itmo.vehicles.dto;

/**
 * Данные для создания/изменения транспортного средства.
 * Координаты задаются одним из двух способов: coordinatesId (связать с уже существующими)
 * либо x и y (создать новые координаты).
 */
public record VehicleRequest(
        String name,
        Long coordinatesId,
        Long x,
        Long y,
        String type,
        Double enginePower,
        Long numberOfWheels,
        Double capacity,
        Double distanceTravelled,
        Double fuelConsumption,
        String fuelType) {
}
