package ru.itmo.vehicles.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.vehicles.dto.PageDto;
import ru.itmo.vehicles.dto.VehicleDto;
import ru.itmo.vehicles.dto.VehicleFilter;
import ru.itmo.vehicles.dto.VehicleRequest;
import ru.itmo.vehicles.error.AppValidationException;
import ru.itmo.vehicles.error.NotFoundException;
import ru.itmo.vehicles.event.ChangeEvent;
import ru.itmo.vehicles.model.Coordinates;
import ru.itmo.vehicles.model.FuelType;
import ru.itmo.vehicles.model.Vehicle;
import ru.itmo.vehicles.model.VehicleType;
import ru.itmo.vehicles.repository.CoordinatesRepository;
import ru.itmo.vehicles.repository.VehicleRepository;

import java.util.Map;

@Service
@Transactional
public class VehicleService {

    /** Допустимые колонки сортировки: ключ из UI -> путь в сущности. */
    private static final Map<String, String> SORT_FIELDS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("name", "name"),
            Map.entry("coordinatesId", "coordinates.id"),
            Map.entry("x", "coordinates.x"),
            Map.entry("y", "coordinates.y"),
            Map.entry("creationDate", "creationDate"),
            Map.entry("type", "type"),
            Map.entry("enginePower", "enginePower"),
            Map.entry("numberOfWheels", "numberOfWheels"),
            Map.entry("capacity", "capacity"),
            Map.entry("distanceTravelled", "distanceTravelled"),
            Map.entry("fuelConsumption", "fuelConsumption"),
            Map.entry("fuelType", "fuelType"));

    private final VehicleRepository vehicles;
    private final CoordinatesRepository coordinates;
    private final VehicleValidator validator;
    private final ApplicationEventPublisher events;

    public VehicleService(VehicleRepository vehicles, CoordinatesRepository coordinates,
                          VehicleValidator validator, ApplicationEventPublisher events) {
        this.vehicles = vehicles;
        this.coordinates = coordinates;
        this.validator = validator;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public PageDto<VehicleDto> list(VehicleFilter filter, int page, int size, String sort, String dir) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        String property = SORT_FIELDS.getOrDefault(sort, "id");
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort order = Sort.by(direction, property);
        if (!property.equals("id")) {
            order = order.and(Sort.by(Sort.Direction.ASC, "id")); // стабильная пагинация
        }
        Page<Vehicle> result = vehicles.findAll(VehicleSpecs.filter(filter), PageRequest.of(safePage, safeSize, order));
        return new PageDto<>(
                result.getContent().stream().map(VehicleDto::from).toList(),
                result.getTotalElements(), result.getNumber(), result.getSize(), result.getTotalPages());
    }

    /** Информация об объекте по id вместе со связанным объектом (координатами). */
    @Transactional(readOnly = true)
    public VehicleDto get(Long id) {
        return VehicleDto.from(find(id));
    }

    public VehicleDto create(VehicleRequest request) {
        validator.validate(request);
        Coordinates target = resolveCoordinates(request);
        Vehicle vehicle = new Vehicle();
        apply(vehicle, request, target);
        vehicle = vehicles.saveAndFlush(vehicle);
        events.publishEvent(new ChangeEvent("vehicle", "created", vehicle.getId()));
        return VehicleDto.from(vehicle);
    }

    public VehicleDto update(Long id, VehicleRequest request) {
        Vehicle vehicle = find(id);
        validator.validate(request);
        Coordinates target = resolveCoordinates(request);
        apply(vehicle, request, target);
        vehicle = vehicles.saveAndFlush(vehicle);
        events.publishEvent(new ChangeEvent("vehicle", "updated", vehicle.getId()));
        return VehicleDto.from(vehicle);
    }

    /** Удаляется только транспортное средство; его координаты остаются в системе. */
    public void delete(Long id) {
        Vehicle vehicle = find(id);
        vehicles.delete(vehicle);
        vehicles.flush();
        events.publishEvent(new ChangeEvent("vehicle", "deleted", id));
    }

    private Vehicle find(Long id) {
        return vehicles.findById(id)
                .orElseThrow(() -> new NotFoundException("Транспортное средство с id " + id + " не найдено"));
    }

    /**
     * Связь с уже существующими координатами (они блокируются на чтение, чтобы их не удалили
     * до завершения транзакции) либо создание новых координат по x и y.
     */
    private Coordinates resolveCoordinates(VehicleRequest r) {
        if (r.coordinatesId() != null) {
            return coordinates.lockShared(r.coordinatesId())
                    .orElseThrow(() -> new AppValidationException("coordinatesId",
                            "Координаты с id " + r.coordinatesId() + " не найдены"));
        }
        Coordinates created = coordinates.saveAndFlush(new Coordinates(r.x(), r.y()));
        events.publishEvent(new ChangeEvent("coordinates", "created", created.getId()));
        return created;
    }

    private void apply(Vehicle v, VehicleRequest r, Coordinates c) {
        v.setName(r.name().trim());
        v.setCoordinates(c);
        v.setType(VehicleType.valueOf(r.type()));
        v.setEnginePower(r.enginePower());
        v.setNumberOfWheels(r.numberOfWheels());
        v.setCapacity(r.capacity());
        v.setDistanceTravelled(r.distanceTravelled());
        v.setFuelConsumption(r.fuelConsumption());
        v.setFuelType(FuelType.valueOf(r.fuelType()));
    }
}
