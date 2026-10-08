package io.miragon.blueprint.domain.leasing

import java.util.UUID

@JvmInline
value class ApplicationId(val value: UUID) {
    companion object {
        fun new(): ApplicationId = ApplicationId(UUID.randomUUID())

        fun of(value: String): ApplicationId = ApplicationId(UUID.fromString(value))
    }
}
