package ru.itmo.vehicles.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.vehicles.dto.CoordinatesRequest;
import ru.itmo.vehicles.dto.CoordinatesUsageDto;
import ru.itmo.vehicles.service.CoordinatesService;

import java.util.List;

@RestController
@RequestMapping("/api/coordinates")
public class CoordinatesController {

    private final CoordinatesService service;

    public CoordinatesController(CoordinatesService service) {
        this.service = service;
    }

    @GetMapping
    public List<CoordinatesUsageDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public CoordinatesUsageDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CoordinatesUsageDto create(@RequestBody CoordinatesRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CoordinatesUsageDto update(@PathVariable Long id, @RequestBody CoordinatesRequest request) {
        return service.update(id, request);
    }

    /** Если координаты используются транспортными средствами, возвращается 409 и удаление отменяется. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
