package io.miragon.blueprint

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.time.Clock

@SpringBootApplication
class CibsevenBikeLeasingApplication {

    /** Single source of "now" for the app, so time-dependent logic can be pinned in tests. */
    @Bean
    fun clock(): Clock = Clock.systemDefaultZone()
}

fun main(args: Array<String>) {
    runApplication<CibsevenBikeLeasingApplication>(*args)
}
