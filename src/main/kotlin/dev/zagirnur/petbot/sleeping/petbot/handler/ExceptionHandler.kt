package dev.zagirnur.petbot.sleeping.petbot.handler

import dev.zagirnur.petbot.sdk.BotSender
import dev.zagirnur.petbot.sleeping.petbot.exceptions.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class ExceptionHandler(
    private val bot: BotSender,
) : dev.zagirnur.petbot.sdk.ExceptionHandler {

    private val log: Logger = LoggerFactory.getLogger(ExceptionHandler::class.java)

    override fun handle(update: Update, t: Throwable?) {
        log.error("Error on update: $update", t)

        val replyBuilder = when (t) {

            is UserNotFoundException -> {
                bot.reply(update).text("User not found")
            }

            is GroupNotFoundException -> {
                bot.reply(update).text("Group not found")
            }

            is ExpenseNotFoundException -> {
                bot.reply(update).text("Expense not found")
            }

            is UserAlreadyInGroupException -> {
                bot.reply(update).text("User already in group")
            }

            is UserNotInGroupException -> {
                bot.reply(update).text("User not in group")
            }

            else -> {
                bot.reply(update).text("Unknown error: ${t?.message}")
            }
        }

        if (update.hasCallbackQuery()) {
            replyBuilder.sendPopup()
        } else {
            replyBuilder.send()
        }
    }
}