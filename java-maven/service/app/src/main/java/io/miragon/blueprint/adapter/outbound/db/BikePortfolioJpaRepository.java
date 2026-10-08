package io.miragon.blueprint.adapter.outbound.db;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BikePortfolioJpaRepository extends JpaRepository<BikeEntity, String> {
}
