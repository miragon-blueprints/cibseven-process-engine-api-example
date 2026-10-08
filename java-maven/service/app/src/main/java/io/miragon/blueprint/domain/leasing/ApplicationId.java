package io.miragon.blueprint.domain.leasing;

import java.util.UUID;

public record ApplicationId(UUID value) {

    public static ApplicationId newId() {
        return new ApplicationId(UUID.randomUUID());
    }

    public static ApplicationId of(String value) {
        return new ApplicationId(UUID.fromString(value));
    }
}
