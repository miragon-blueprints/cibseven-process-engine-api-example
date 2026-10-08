package io.miragon.blueprint.adapter.outbound.db

import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository
import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.bike.BikeId
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class BikePortfolioPersistenceAdapter(
    private val repository: BikePortfolioJpaRepository,
) : BikePortfolioRepository {

    override fun save(bike: Bike): Bike =
        repository.save(BikeEntity(bikeId = bike.bikeId.value, model = bike.model)).toDomain()

    override fun findByBikeId(bikeId: BikeId): Bike? =
        repository.findById(bikeId.value).orElse(null)?.toDomain()

    override fun findAll(): List<Bike> =
        repository.findAll(Sort.by(Sort.Direction.ASC, "bikeId")).map { it.toDomain() }

    override fun findAllByIds(bikeIds: List<BikeId>): List<Bike> =
        if (bikeIds.isEmpty()) {
            emptyList()
        } else {
            repository.findAllById(bikeIds.map { it.value }).map { it.toDomain() }
        }

    private fun BikeEntity.toDomain() = Bike(bikeId = BikeId(bikeId), model = model)
}
