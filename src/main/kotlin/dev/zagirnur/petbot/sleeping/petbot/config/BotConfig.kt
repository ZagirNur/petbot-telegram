package dev.zagirnur.petbot.sleeping.petbot.config

import dev.zagirnur.petbot.sdk.HandlerRegistry
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler
import dev.zagirnur.petbot.sleeping.petbot.handler.GroupHandler
//import dev.zagirnur.petbot.sleeping.petbot.handler.TestHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

@Configuration
class BotConfig {

    @Autowired
    fun botSettings(registry: HandlerRegistry) {
        registry.setHandlersOrder(
            listOf(
//                TestHandler::class.java
                GroupHandler::class.java,
                ExpenseHandler::class.java
            ), true
        )
    }

}