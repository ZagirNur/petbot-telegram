package dev.zagirnur.petbot.sleeping.petbot.handler

import dev.zagirnur.petbot.sdk.BotSender
import dev.zagirnur.petbot.sdk.BotUtils.getFrom
import dev.zagirnur.petbot.sdk.annotations.OnMessage
import dev.zagirnur.petbot.sleeping.petbot.context.UserContext
import dev.zagirnur.petbot.sleeping.petbot.exceptions.UserNotFoundException
import dev.zagirnur.petbot.sleeping.petbot.model.Expense
import dev.zagirnur.petbot.sleeping.petbot.service.ExpenseService
import dev.zagirnur.petbot.sleeping.petbot.service.GroupService
import dev.zagirnur.petbot.sleeping.petbot.service.UserService
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class ExpenseHandler(
    val bot: BotSender,
    val userService: UserService,
    val groupService: GroupService,
    val expenseService: ExpenseService,
    val groupHandler: GroupHandler,
) {


    companion object {
        // button prefixes

        // chat states

    }


    //language=RegExp
    @OnMessage(regexp = """^\w?\s?[\d., ]+\D{0,3}\s.*$""")
    fun onExpense(update: Update, ctx: UserContext) {
        val amount = update.message.text.split(" ")[0].replace(",", ".").toBigDecimal()
        val description = update.message.text.split(" ").drop(1).joinToString(" ")

        val user = userService.findByTelegramId(getFrom(update).id)
            ?: throw UserNotFoundException(getFrom(update).id)

        val defaultGroupId = ctx.defaultGroup ?: run {
            groupHandler.onGroups(update, ctx)
            return
        }

        val defaultGroup = groupService.getById(defaultGroupId)

        val newExpense = expenseService.createNew(
            groupId = defaultGroup.id,
            amount = amount,
            description = description,
            paidBy = mapOf(user.id!! to amount),
            splitBy = mapOf(user.id!! to amount),
        )

        showExpenseView(update, ctx, newExpense, "Добавлен новый расход\n\n")
    }

    fun showExpenseView(
        update: Update,
        ctx: UserContext,
        newExpense: Expense,
        prefix: String
    ) {
        bot.reply(update)
            .text(
                """
                $prefix
                |Сумма: ${newExpense.amount}
                |Описание: ${newExpense.description}
                |Заплатили:
                |${
                    newExpense.paidBy.entries.joinToString("\n") {
                        "${userService.getById(it.key).getViewName()}: ${it.value}"
                    }
                }
                |Участвуют:
                |${
                    newExpense.splitBy.entries.joinToString("\n") {
                        "${userService.getById(it.key).getViewName()}: ${it.value}"
                    }
                }
            """.trimMargin()
            )
            .editIfCallbackMessageOrSend()
    }

}