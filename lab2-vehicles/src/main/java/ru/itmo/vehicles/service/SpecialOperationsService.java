package ru.itmo.vehicles.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.vehicles.dto.VehicleDto;
import ru.itmo.vehicles.error.AppValidationException;
import ru.itmo.vehicles.model.FuelType;
import ru.itmo.vehicles.model.VehicleType;
import ru.itmo.vehicles.repository.VehicleRepository;

import java.util.Arrays;
import java.util.List;

/**
 * Специальные операции. Сама работа выполняется функциями БД (см. schema.sql),
 * этот сервис проверяет входные параметры и вызывает их через репозиторий.
 */
@Service
@Transactional(readOnly = true)
public class SpecialOperationsService {

    private final VehicleRepository vehicles;

    public SpecialOperationsService(VehicleRepository vehicles) {
        this.vehicles = vehicles;
    }

    /** Сумма значений fuelConsumption для всех объектов. */
    public double sumFuelConsumption() {
        Double sum = vehicles.sumFuelConsumption();
        return sum == null ? 0 : sum;
    }

    /** Количество объектов, значение fuelType которых меньше заданного (по порядку констант enum). */
    public long countWithFuelTypeLess(String fuelType) {
        if (fuelType == null || Arrays.stream(FuelType.values()).noneMatch(f -> f.name().equals(fuelType))) {
            throw new AppValidationException("fuelType",
                    "Выберите тип топлива из списка: " + Arrays.toString(FuelType.values()));
        }
        return vehicles.countWithFuelTypeLess(fuelType);
    }

    /** Объекты, значение поля name которых содержит заданную подстроку. */
    public List<VehicleDto> findByNameSubstring(String substring) {
        if (substring == null || substring.isEmpty()) {
            throw new AppValidationException("substring", "Подстрока не должна быть пустой");
        }
        return vehicles.findByNameSubstring(substring).stream().map(VehicleDto::from).toList();
    }

    /** Все транспортные средства заданного типа. */
    public List<VehicleDto> findByType(String type) {
        if (type == null || Arrays.stream(VehicleType.values()).noneMatch(t -> t.name().equals(type))) {
            throw new AppValidationException("type",
                    "Выберите тип из списка: " + Arrays.toString(VehicleType.values()));
        }
        return vehicles.findByTypeFunction(type).stream().map(VehicleDto::from).toList();
    }

    /** Все транспортные средства с числом колёс в заданном диапазоне (границы включаются). */
    public List<VehicleDto> findByWheelsRange(Long min, Long max) {
        if (min == null) {
            throw new AppValidationException("min", "Укажите нижнюю границу диапазона");
        }
        if (max == null) {
            throw new AppValidationException("max", "Укажите верхнюю границу диапазона");
        }
        if (min > max) {
            throw new AppValidationException("min", "Нижняя граница не может быть больше верхней");
        }
        return vehicles.findByWheelsRange(min, max).stream().map(VehicleDto::from).toList();
    }
}
