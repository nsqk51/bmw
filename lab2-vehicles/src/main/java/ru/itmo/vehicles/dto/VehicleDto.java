package ru.itmo.vehicles.dto;

import ru.itmo.vehicles.model.FuelType;
import ru.itmo.vehicles.model.Vehicle;
import ru.itmo.vehicles.model.VehicleType;

import java.util.Date;

public record VehicleDto(
        Long id,
        String name,
        CoordinatesDto coordinates,
        Date creationDate,
        VehicleType type,
        Double enginePower,
        long numberOfWheels,
        double capacity,
        Double distanceTravelled,
        Double fuelConsumption,
        FuelType fuelType) {

    public static VehicleDto from(Vehicle v) {
        return new VehicleDto(
                v.getId(),
                v.getName(),
                CoordinatesDto.from(v.getCoordinates()),
                v.getCreationDate(),
                v.getType(),
                v.getEnginePower(),
                v.getNumberOfWheels(),
                v.getCapacity(),
                v.getDistanceTravelled(),
                v.getFuelConsumption(),
                v.getFuelType());
    }
}
