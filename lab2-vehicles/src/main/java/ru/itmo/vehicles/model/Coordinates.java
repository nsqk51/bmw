package ru.itmo.vehicles.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

/** Вспомогательный класс: отдельная таблица, на одни координаты могут ссылаться несколько транспортных средств. */
@Entity
@Table(name = "coordinates")
public class Coordinates {

    @Positive
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private long x;

    @DecimalMin(value = "-913", inclusive = false, message = "Координата Y должна быть больше -913")
    @Column(nullable = false)
    private long y; // Значение поля должно быть больше -913

    public Coordinates() {
    }

    public Coordinates(long x, long y) {
        this.x = x;
        this.y = y;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getX() {
        return x;
    }

    public void setX(long x) {
        this.x = x;
    }

    public long getY() {
        return y;
    }

    public void setY(long y) {
        this.y = y;
    }
}
