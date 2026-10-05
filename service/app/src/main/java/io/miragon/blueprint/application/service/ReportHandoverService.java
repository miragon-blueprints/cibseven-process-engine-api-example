package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.ReportHandoverUseCase;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.application.port.outbound.LeasingProcess;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReportHandoverService implements ReportHandoverUseCase {

    private final LeasingProcess process;
    private final LeasingApplicationRepository repository;

    public ReportHandoverService(LeasingProcess process, LeasingApplicationRepository repository) {
        this.process = process;
        this.repository = repository;
    }

    @Override
    public void reportHandover(ApplicationId id) {
        process.correlateHandoverReported(id);
        LeasingApplication application = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Unknown application " + id));
        repository.save(application.reportHandover());
    }
}
