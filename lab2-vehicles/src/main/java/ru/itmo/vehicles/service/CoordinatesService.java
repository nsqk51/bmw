package ru.itmo.vehicles.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.vehicles.dto.CoordinatesRequest;
import ru.itmo.vehicles.dto.CoordinatesUsageDto;
import ru.itmo.vehicles.error.ConflictException;
import ru.itmo.vehicles.error.NotFoundException;
import ru.itmo.vehicles.event.ChangeEvent;
import ru.itmo.vehicles.model.Coordinates;
import ru.itmo.vehicles.repository.CoordinatesRepository;
import ru.itmo.vehicles.repository.VehicleRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class CoordinatesService {

    private final CoordinatesRepository coordinates;
    private final VehicleRepository vehicles;
    private final CoordinatesValidator validator;
    private final ApplicationEventPublisher events;

    public CoordinatesService(CoordinatesRepository coordinates, VehicleRepository vehicles,
                              CoordinatesValidator validator, ApplicationEventPublisher events) {
        this.coordinates = coordinates;
        this.vehicles = vehicles;
        this.validator = validator;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<CoordinatesUsageDto> list() {
        Map<Long, Long> usage = new HashMap<>();
        for (Object[] row : vehicles.usageByCoordinates()) {
            usage.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return coordinates.findAll(Sort.by("id")).stream()
                .map(c -> CoordinatesUsageDto.from(c, usage.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CoordinatesUsageDto get(Long id) {
        Coordinates c = find(id);
        return CoordinatesUsageDto.from(c, vehicles.countByCoordinatesId(id));
    }

    public CoordinatesUsageDto create(CoordinatesRequest request) {
        validator.validate(request);
        Coordinates c = coordinates.saveAndFlush(new Coordinates(request.x(), request.y()));
        events.publishEvent(new ChangeEvent("coordinates", "created", c.getId()));
        return CoordinatesUsageDto.from(c, 0);
    }

    public CoordinatesUsageDto update(Long id, CoordinatesRequest request) {
        Coordinates c = find(id);
        validator.validate(request);
        c.setX(request.x());
        c.setY(request.y());
        c = coordinates.saveAndFlush(c);
        events.publishEvent(new ChangeEvent("coordinates", "updated", c.getId()));
        events.publishEvent(new ChangeEvent("vehicle", "updated", null)); // у связанных объектов изменились x и y
        return CoordinatesUsageDto.from(c, vehicles.countByCoordinatesId(id));
    }

    /**
     * Удаление координат. Если на них ссылается хотя бы одно транспортное средство,
     * операция отменяется и пользователю сообщается о невозможности удаления.
     */
    public void delete(Long id) {
        Coordinates c = coordinates.lockExclusive(id)
                .orElseThrow(() -> new NotFoundException("Координаты с id " + id + " не найдены"));
        long used = vehicles.countByCoordinatesId(id);
        if (used > 0) {
            throw new ConflictException("Удаление невозможно: эти координаты используются транспортными средствами ("
                    + used + "). Сначала измените или удалите их");
        }
        coordinates.delete(c);
        coordinates.flush();
        events.publishEvent(new ChangeEvent("coordinates", "deleted", id));
    }

    private Coordinates find(Long id) {
        return coordinates.findById(id)
                .orElseThrow(() -> new NotFoundException("Координаты с id " + id + " не найдены"));
    }
}
