package ru.itmo.vehicles.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.vehicles.model.Coordinates;

import java.util.Optional;

public interface CoordinatesRepository extends JpaRepository<Coordinates, Long> {

    /** Общая блокировка (FOR SHARE): пока объект связывается с координатами, их нельзя удалить. */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select c from Coordinates c where c.id = :id")
    Optional<Coordinates> lockShared(@Param("id") Long id);

    /** Исключительная блокировка (FOR UPDATE): на время проверки и удаления координат. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coordinates c where c.id = :id")
    Optional<Coordinates> lockExclusive(@Param("id") Long id);
}
