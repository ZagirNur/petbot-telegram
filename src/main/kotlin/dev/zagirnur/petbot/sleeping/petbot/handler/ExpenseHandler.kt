package dev.zagirnur.petbot.sleeping.petbot.handler

import dev.zagirnur.petbot.sdk.BotSender
import dev.zagirnur.petbot.sdk.ReplyBuilder.row
import dev.zagirnur.petbot.sdk.annotations.OnCallback
import dev.zagirnur.petbot.sdk.annotations.OnMessage
import dev.zagirnur.petbot.sleeping.petbot.context.EditingExpense
import dev.zagirnur.petbot.sleeping.petbot.context.UserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class ExpenseHandler {


    companion object {
        // button prefixes

        // chat states

    }

    @Autowired
    lateinit var bot: BotSender

    //language=RegExp
    @OnMessage(regexp = """^\w?\s?[\d., ]+\D{0,3}\s.*$""")
    fun onExpense(update: Update, ctx: UserContext) {
        val userText = update.message.text

        val amount = userText.split(" ")[0].toBigDecimalOrNull()
        if (amount == null) {
            bot.reply(update)
                .text("Не могу распознать сумму. Попробуйте еще раз.")
                .send()
            return
        }
        val description = userText.substringAfter(" ")

        ctx.editingExpenses.add(
            EditingExpense(
                amount = amount,
                description = description,
                paidBy = mutableMapOf(update.message.from.id to amount),
                whoSplitIt = mutableMapOf(update.message.from.id to amount)
            )
        )

        bot.reply(update)
            .text("""
                |Сумма: $amount
                |Описание: $description
                |""".trimMargin())
            .inlineKeyboard(
                row("Добавить участника", "add_participant"),
                row("Сохранить", "save_expense")
            )
            .send()
    }

    @OnCallback(prefix = "add_participant")
    fun onAddParticipant(update: Update, ctx: UserContext) {
        ctx.state = "adding_participant"
        bot.reply(update)
            .text("Введите имя участника")
            .send()
    }

    @OnMessage(state = "adding_participant")
    fun onAddingParticipant(update: Update, ctx: UserContext) {
        val userText = update.message.text
        val amount = userText.split(" ")[0].toBigDecimalOrNull()
        if (amount == null) {
            bot.reply(update)
                .text("Не могу распознать сумму. Попробуйте еще раз.")
                .send()
            return
        }
        val description = userText.substringAfter(" ")

        ctx.editingExpenses.add(
            EditingExpense(
                amount = amount,
                description = description,
                paidBy = mutableMapOf(update.message.from.id to amount),
                whoSplitIt = mutableMapOf(update.message.from.id to amount)
            )
        )

        bot.reply(update)
            .text("""
                |Сумма: $amount
                |Описание: $description
                |""".trimMargin())
            .inlineKeyboard(
                row("Добавить участника", "add_participant"),
                row("Сохранить", "save_expense")
            )
            .send()
    }
}