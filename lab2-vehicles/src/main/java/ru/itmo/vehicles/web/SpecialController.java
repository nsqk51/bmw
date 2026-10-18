package ru.itmo.vehicles.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.vehicles.dto.VehicleDto;
import ru.itmo.vehicles.service.SpecialOperationsService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/special")
public class SpecialController {

    private final SpecialOperationsService service;

    public SpecialController(SpecialOperationsService service) {
        this.service = service;
    }

    @GetMapping("/fuel-sum")
    public Map<String, Double> fuelSum() {
        return Map.of("sum", service.sumFuelConsumption());
    }

    @GetMapping("/fuel-less")
    public Map<String, Long> fuelLess(@RequestParam(required = false) String fuelType) {
        return Map.of("count", service.countWithFuelTypeLess(fuelType));
    }

    @GetMapping("/by-name")
    public List<VehicleDto> byName(@RequestParam(required = false) String substring) {
        return service.findByNameSubstring(substring);
    }

    @GetMapping("/by-type")
    public List<VehicleDto> byType(@RequestParam(required = false) String type) {
        return service.findByType(type);
    }

    @GetMapping("/by-wheels")
    public List<VehicleDto> byWheels(@RequestParam(required = false) Long min,
                                     @RequestParam(required = false) Long max) {
        return service.findByWheelsRange(min, max);
    }
}
