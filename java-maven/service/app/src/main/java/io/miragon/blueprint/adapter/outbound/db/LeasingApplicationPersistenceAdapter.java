package io.miragon.blueprint.adapter.outbound.db;

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class LeasingApplicationPersistenceAdapter implements LeasingApplicationRepository {

    private final LeasingApplicationJpaRepository repository;

    public LeasingApplicationPersistenceAdapter(LeasingApplicationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public LeasingApplication save(LeasingApplication application) {
        LeasingApplicationEntity entity = repository.save(LeasingApplicationEntityMapper.toEntity(application));
        return LeasingApplicationEntityMapper.toDomain(entity);
    }

    @Override
    public Optional<LeasingApplication> findById(ApplicationId id) {
        return repository.findByApplicationId(id.value()).map(LeasingApplicationEntityMapper::toDomain);
    }

    @Override
    public LeasingApplicationRepository.Page findAll(LeasingApplicationRepository.Criteria criteria) {
        // Newest first — the list shows the most recent applications at the top. Spring Data's paging
        // types are used only here, inside the adapter, and never returned to the application layer.
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), Sort.by(Sort.Direction.DESC, "createdAt"));
        // Fully qualified: the simple name Page is the port's own (inherited) Page record here.
        org.springframework.data.domain.Page<LeasingApplicationEntity> result =
                criteria.status() != null
                        ? repository.findAllByStatus(criteria.status(), pageable)
                        : repository.findAll(pageable);
        return new LeasingApplicationRepository.Page(
                result.getContent().stream().map(LeasingApplicationEntityMapper::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
