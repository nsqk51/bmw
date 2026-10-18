package ru.itmo.vehicles.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.vehicles.model.Vehicle;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    long countByCoordinatesId(Long coordinatesId);

    /** Сколько транспортных средств использует каждые координаты: строки вида [coordinatesId, count]. */
    @Query("select v.coordinates.id, count(v) from Vehicle v group by v.coordinates.id")
    List<Object[]> usageByCoordinates();

    // ===== Специальные операции: вызовы функций БД (см. schema.sql) =====

    @Query(value = "SELECT fn_sum_fuel_consumption()", nativeQuery = true)
    Double sumFuelConsumption();

    @Query(value = "SELECT fn_count_fuel_type_less(:fuelType)", nativeQuery = true)
    long countWithFuelTypeLess(@Param("fuelType") String fuelType);

    @Query(value = "SELECT * FROM fn_find_vehicles_by_name(:substring)", nativeQuery = true)
    List<Vehicle> findByNameSubstring(@Param("substring") String substring);

    @Query(value = "SELECT * FROM fn_find_vehicles_by_type(:type)", nativeQuery = true)
    List<Vehicle> findByTypeFunction(@Param("type") String type);

    @Query(value = "SELECT * FROM fn_find_vehicles_by_wheels(:min, :max)", nativeQuery = true)
    List<Vehicle> findByWheelsRange(@Param("min") long min, @Param("max") long max);
}
