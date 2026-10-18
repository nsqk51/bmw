package ru.itmo.vehicles.service;

import org.springframework.stereotype.Component;
import ru.itmo.vehicles.dto.VehicleRequest;
import ru.itmo.vehicles.error.AppValidationException;
import ru.itmo.vehicles.error.FieldProblem;
import ru.itmo.vehicles.model.FuelType;
import ru.itmo.vehicles.model.VehicleType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Проверка входных данных транспортного средства с понятными сообщениями об ошибках. */
@Component
public class VehicleValidator {

    private final CoordinatesValidator coordinatesValidator;

    public VehicleValidator(CoordinatesValidator coordinatesValidator) {
        this.coordinatesValidator = coordinatesValidator;
    }

    public void validate(VehicleRequest r) {
        if (r == null) {
            throw new AppValidationException("body", "Не переданы данные транспортного средства");
        }
        List<FieldProblem> p = new ArrayList<>();

        if (r.name() == null || r.name().isBlank()) {
            p.add(new FieldProblem("name", "Имя не может быть пустым"));
        } else if (r.name().length() > 255) {
            p.add(new FieldProblem("name", "Имя не должно быть длиннее 255 символов"));
        }

        // координаты: либо связь с существующими, либо новые x и y
        if (r.coordinatesId() == null) {
            coordinatesValidator.check(r.x(), r.y(), "x", "y", p);
        }

        if (r.type() == null || r.type().isBlank()) {
            p.add(new FieldProblem("type", "Тип обязателен"));
        } else if (Arrays.stream(VehicleType.values()).noneMatch(t -> t.name().equals(r.type()))) {
            p.add(new FieldProblem("type", "Недопустимый тип. Допустимо: " + Arrays.toString(VehicleType.values())));
        }

        if (r.enginePower() != null && !isPositive(r.enginePower())) {
            p.add(new FieldProblem("enginePower", "Мощность двигателя должна быть больше 0 (или не задана)"));
        }

        if (r.numberOfWheels() == null) {
            p.add(new FieldProblem("numberOfWheels", "Число колёс обязательно"));
        } else if (r.numberOfWheels() <= 0) {
            p.add(new FieldProblem("numberOfWheels", "Число колёс должно быть больше 0"));
        }

        if (r.capacity() == null) {
            p.add(new FieldProblem("capacity", "Вместимость обязательна"));
        } else if (!isPositive(r.capacity())) {
            p.add(new FieldProblem("capacity", "Вместимость должна быть больше 0"));
        }

        if (r.distanceTravelled() != null && !isPositive(r.distanceTravelled())) {
            p.add(new FieldProblem("distanceTravelled", "Пройденное расстояние должно быть больше 0 (или не задано)"));
        }

        if (r.fuelConsumption() == null) {
            p.add(new FieldProblem("fuelConsumption", "Расход топлива обязателен"));
        } else if (!isPositive(r.fuelConsumption())) {
            p.add(new FieldProblem("fuelConsumption", "Расход топлива должен быть больше 0"));
        }

        if (r.fuelType() == null || r.fuelType().isBlank()) {
            p.add(new FieldProblem("fuelType", "Тип топлива обязателен"));
        } else if (Arrays.stream(FuelType.values()).noneMatch(f -> f.name().equals(r.fuelType()))) {
            p.add(new FieldProblem("fuelType", "Недопустимый тип топлива. Допустимо: " + Arrays.toString(FuelType.values())));
        }

        if (!p.isEmpty()) {
            throw new AppValidationException(p);
        }
    }

    private static boolean isPositive(Double v) {
        return !v.isNaN() && !v.isInfinite() && v > 0;
    }
}
