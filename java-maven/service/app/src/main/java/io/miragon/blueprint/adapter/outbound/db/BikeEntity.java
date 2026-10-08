package io.miragon.blueprint.adapter.outbound.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "bike_portfolio")
public class BikeEntity {

    @Id
    @Column(name = "bike_id", nullable = false)
    private String bikeId;

    @Column(name = "model", nullable = false)
    private String model;

    /** For JPA only. */
    protected BikeEntity() {
    }

    public BikeEntity(String bikeId, String model) {
        this.bikeId = bikeId;
        this.model = model;
    }

    public String getBikeId() {
        return bikeId;
    }

    public String getModel() {
        return model;
    }
}
