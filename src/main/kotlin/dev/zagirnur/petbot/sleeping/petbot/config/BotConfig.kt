package dev.zagirnur.petbot.sleeping.petbot.config

import dev.zagirnur.petbot.sdk.HandlerRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Configuration
import org.springframework.retry.annotation.EnableRetry

@EnableRetry
@Configuration
@EnableCaching
class BotConfig {

    @Autowired
    fun botSettings(registry: HandlerRegistry) {
        registry.setHandlersOrder(
            listOf(
//                TestHandler::class.java
//                GroupHandler::class.java,
//                ExpenseHandler::class.java
            ), true
        )
    }

}