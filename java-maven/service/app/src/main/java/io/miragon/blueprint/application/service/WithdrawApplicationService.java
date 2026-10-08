package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.WithdrawApplicationUseCase;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.application.port.outbound.LeasingProcess;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WithdrawApplicationService implements WithdrawApplicationUseCase {

    private final LeasingProcess process;
    private final LeasingApplicationRepository repository;

    public WithdrawApplicationService(LeasingProcess process, LeasingApplicationRepository repository) {
        this.process = process;
        this.repository = repository;
    }

    /**
     * Correlates the withdrawal and immediately moves the read model to WITHDRAWN, so the UI shows
     * the state change right away. The actual cancellation runs asynchronously in the process (it may
     * park on a return-clarification task) and reaches the terminal CANCELLED only once it completes.
     */
    @Override
    public void withdraw(ApplicationId id) {
        process.correlateApplicationWithdrawn(id);
        LeasingApplication application = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Unknown application " + id));
        repository.save(application.withdraw());
    }
}
