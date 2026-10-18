package ru.itmo.vehicles.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.itmo.vehicles.dto.VehicleFilter;
import ru.itmo.vehicles.model.Vehicle;

import java.util.ArrayList;
import java.util.List;

/** Фильтрация по полному совпадению значения строковой колонки (enum сравниваются по имени константы). */
final class VehicleSpecs {

    private VehicleSpecs() {
    }

    static Specification<Vehicle> filter(VehicleFilter f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            equal(cb, root.<String>get("name"), f.name(), ps);
            equal(cb, root.get("type").as(String.class), f.type(), ps);
            equal(cb, root.get("fuelType").as(String.class), f.fuelType(), ps);
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private static void equal(CriteriaBuilder cb, Expression<String> expr, String value, List<Predicate> ps) {
        if (value == null || value.isBlank()) {
            return;
        }
        ps.add(cb.equal(expr, value.trim()));
    }
}
