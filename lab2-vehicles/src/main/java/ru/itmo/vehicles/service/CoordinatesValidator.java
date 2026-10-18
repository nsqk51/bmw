package ru.itmo.vehicles.service;

import org.springframework.stereotype.Component;
import ru.itmo.vehicles.dto.CoordinatesRequest;
import ru.itmo.vehicles.error.AppValidationException;
import ru.itmo.vehicles.error.FieldProblem;

import java.util.ArrayList;
import java.util.List;

@Component
public class CoordinatesValidator {

    public static final long MIN_Y_EXCLUSIVE = -913;

    public void validate(CoordinatesRequest r) {
        if (r == null) {
            throw new AppValidationException("body", "Не переданы данные координат");
        }
        List<FieldProblem> p = new ArrayList<>();
        check(r.x(), r.y(), "x", "y", p);
        if (!p.isEmpty()) {
            throw new AppValidationException(p);
        }
    }

    /** Общая проверка пары x/y; используется и при создании транспортного средства с новыми координатами. */
    void check(Long x, Long y, String xField, String yField, List<FieldProblem> p) {
        if (x == null) {
            p.add(new FieldProblem(xField, "Координата X обязательна и должна быть целым числом"));
        }
        if (y == null) {
            p.add(new FieldProblem(yField, "Координата Y обязательна и должна быть целым числом"));
        } else if (y <= MIN_Y_EXCLUSIVE) {
            p.add(new FieldProblem(yField, "Координата Y должна быть больше -913"));
        }
    }
}
