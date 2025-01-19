package dev.zagirnur.petbot.sleeping.petbot.handler

import dev.zagirnur.petbot.sdk.BotSender
import dev.zagirnur.petbot.sdk.BotUtils.getFrom
import dev.zagirnur.petbot.sdk.ReplyBuilder.btn
import dev.zagirnur.petbot.sdk.ReplyBuilder.row
import dev.zagirnur.petbot.sdk.TableBuilder
import dev.zagirnur.petbot.sdk.annotations.OnCallback
import dev.zagirnur.petbot.sdk.annotations.OnMessage
import dev.zagirnur.petbot.sleeping.petbot.context.UserContext
import dev.zagirnur.petbot.sleeping.petbot.dao.entity.BotUserEntity
import dev.zagirnur.petbot.sleeping.petbot.exceptions.UserNotFoundException
import dev.zagirnur.petbot.sleeping.petbot.model.Expense
import dev.zagirnur.petbot.sleeping.petbot.model.Group
import dev.zagirnur.petbot.sleeping.petbot.model.SplitType
import dev.zagirnur.petbot.sleeping.petbot.service.ExpenseService
import dev.zagirnur.petbot.sleeping.petbot.service.GroupService
import dev.zagirnur.petbot.sleeping.petbot.service.UserService
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.math.BigDecimal

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
        const val BTN_GROUP_EXPENSES = "GROUP_EXPENSES:"
        const val BTN_VIEW_ONE_EXPENSE = "VIEW_ONE_EXPENSE:"
        const val BTN_EDIT_PAID_BY = "EDIT_PAID_BY:"
        const val BTN_EDIT_SPLIT_BY = "EDIT_SPLIT_BY:"
        const val BTN_SET_SPLIT_EQUALLY = "SET_SPLIT_EQUALLY:"
        const val BTN_SET_SPLIT_EQUALLY_APPROVE = "SET_SPLIT_EQUALLY_APPROVE:"
        const val BTN_TMP_DELETE_FROM_PAID_BY = "TMP_DELETE_FROM_PAID_BY:"
        const val BTN_TMP_EDIT_PAID_AMOUNT = "TMP_EDIT_PAID_AMOUNT:"
        const val BTN_TMP_EDIT_PAID_AMOUNT_SET_FULL = "TMP_EDIT_PAID_AMOUNT_SET_FULL:"
        const val BTN_TMP_CANCEL_EDIT_PAID_BY = "TMP_CANCEL_EDIT_PAID_BY:"
        const val BTN_TMP_CANCEL_EDIT_SPLIT_BY = "TMP_CANCEL_EDIT_SPLIT_BY:"
        const val BTN_TMP_SAVE_PAID_BY = "TMP_SAVE_PAID_BY:"
        const val BTN_TMP_SAVE_SPLIT_BY = "TMP_SAVE_SPLIT_BY:"

        const val BTN_DELETE_FROM_SPLIT_BY_EQUALLY = "DELETE_FROM_SPLIT_BY:"
        const val BTN_TMP_DELETE_FROM_SPLIT_BY_NOT_EQUALLY = "DELETE_FROM_SPLIT_BY_NOT_EQUALLY:"
        const val BTN_PUT_TO_SPLIT_BY_EQUALLY = "PUT_TO_SPLIT_BY:"
        const val BTN_TMP_EDIT_SPLIT_AMOUNT = "TMP_EDIT_SPLIT_AMOUNT:"


        // chat states
        const val STT_EDIT_PAID_AMOUNT = "EDIT_PAID_AMOUNT:"
        const val STT_EDIT_SPLIT_AMOUNT = "EDIT_SPLIT_AMOUNT:"

    }

    @OnCallback(prefix = "UNSUPPORTED")
    fun onUnsupported(update: Update, ctx: UserContext) {
        bot.reply(update)
            .text("Эта функция пока не поддерживается")
            .sendPopup()
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

        showExpenseView(update, ctx, newExpense, defaultGroup, "Добавлен новый расход\n\n")
    }

    fun showExpenseView(
        update: Update,
        ctx: UserContext,
        newExpense: Expense,
        group: Group,
        prefix: String
    ) {
        val paidBy = newExpense.paidBy.entries.toList()
        val splitBy = newExpense.splitBy.entries.toList()
        val idToUser = group.members.associateWith { userService.getById(it) }
        bot.reply(update)
            .text(
                """
                $prefix
                |ПРОСМОТР РАСХОДА
                |Описание: ${newExpense.description}
                |Сумма: ${newExpense.amount}
                |Распределение: ${if (newExpense.splitType == SplitType.EQUALLY) "поровну" else "не поровну"}
                |
                |${
                    TableBuilder(
                        listOf("Заплатил", "Сумма"), '-', " | "
                    ).column { idx ->
                        paidBy[idx].let { idToUser[it.key]?.getViewName() }
                    }.numberColumn {
                        paidBy[it].value.toPlainString()
                    }.build()
                }
                |
                |${
                    TableBuilder(
                        listOf("Участник", "Сумма"), '-', " | "
                    ).column { idx ->
                        splitBy[idx].let { idToUser[it.key]?.getViewName() }
                    }.numberColumn { idx ->
                        splitBy[idx].value.toPlainString()
                    }.build()
                }
                | 
                """.trimMargin()
            )
            .inlineKeyboard(
                row("✏️Плательщики", BTN_EDIT_PAID_BY + newExpense.id),
                row("🤝Разделить поровну", BTN_SET_SPLIT_EQUALLY + newExpense.id),
                *group.members.map { mId ->
                    if (newExpense.splitBy.containsKey(mId)) {
                        splitParticipantRow(newExpense, userService.getById(mId))
                    } else {
                        notSplitParticipantRow(newExpense, userService.getById(mId))
                    }
                }.toTypedArray(),
                row("⬅️Расходы группы", BTN_GROUP_EXPENSES + newExpense.groupId),
            )
            .editIfCallbackMessageOrSend()
    }

    @OnCallback(prefix = BTN_GROUP_EXPENSES)
    fun onGroupExpenses(update: Update, ctx: UserContext) {
        val groupId = update.callbackQuery.data.removePrefix(BTN_GROUP_EXPENSES).toLong()
        val group = groupService.getById(groupId)

        val expenses = expenseService.getGroupExpenses(groupId)

        bot.reply(update)
            .text(
                """
                |Расходы группы ${group.name}
                |${expenses.joinToString("\n") { it.description }}
                """.trimMargin()
            )
            .inlineKeyboard(
                *expenses.map {
                    row(it.description, BTN_VIEW_ONE_EXPENSE + it.id)
                }.toTypedArray(),
            )
            .editIfCallbackMessageOrSend()
    }

    @OnCallback(prefix = BTN_VIEW_ONE_EXPENSE)
    fun onViewOneExpense(update: Update, ctx: UserContext) {
        val expenseId = update.callbackQuery.data.removePrefix(BTN_VIEW_ONE_EXPENSE).toLong()
        val expense = expenseService.getById(expenseId)

        val group = groupService.getById(expense.groupId)

        showExpenseView(update, ctx, expense, group, "")
    }

    @OnCallback(prefix = BTN_EDIT_PAID_BY)
    fun onEditPaidBy(update: Update, ctx: UserContext) {
        val expenseId = update.callbackQuery.data.removePrefix(BTN_EDIT_PAID_BY).toLong()
        editPaidByView(expenseId, ctx, update)
    }

    private fun editPaidByView(
        expenseId: Long,
        ctx: UserContext,
        update: Update
    ) {
        val editingExpense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }
        val group = groupService.getById(editingExpense.groupId)
        val idToUser = group.members.associateWith { userService.getById(it) }

        val disbalance = editingExpense.amount - editingExpense.paidBy.values.reduce(BigDecimal::add)
        val paidBayList = editingExpense.paidBy.entries.toList()

        bot.reply(update)
            .text(
                """
                |ПЛАТЕЛЬЩИКИ
                |Расход ${editingExpense.description}
                |Сумма: ${editingExpense.amount}
                |
                |${
                    TableBuilder('-', " | ")
                        .column("Плательщик") { idx ->
                            val userId = paidBayList[idx].key
                            idToUser[userId]?.getViewName()
                        }
                        .numberColumn("Сумма") { idx ->
                            paidBayList[idx].value.toPlainString()
                        }.build()
                }
                |
                |${
                    if (disbalance > BigDecimal.ZERO) {
                        "🔴Недоплата: $disbalance"
                    } else if (disbalance < BigDecimal.ZERO) {
                        "🔴Переплата: ${disbalance.negate()}"
                    } else ""
                }
                """.trimMargin()
            )
            .inlineKeyboard(
                *group.members.map { mId ->
                    if (editingExpense.paidBy.containsKey(mId)) {
                        participantRow(editingExpense, userService.getById(mId))
                    } else {
                        notParticipantRow(editingExpense, userService.getById(mId))
                    }
                }.toTypedArray(),
                row("✅Сохранить", BTN_TMP_SAVE_PAID_BY + editingExpense.id),
                row("⬅️Отменить изменения", BTN_TMP_CANCEL_EDIT_PAID_BY + editingExpense.id),
            )
            .editIfCallbackMessageOrSend()
    }

    @OnCallback(prefix = BTN_TMP_SAVE_PAID_BY)
    fun onTmpSavePaidBy(update: Update, ctx: UserContext) {
        val expenseId = update.callbackQuery.data.removePrefix(BTN_TMP_SAVE_PAID_BY).toLong()
        val newPaidBy = ctx.editingExpenses[expenseId]!!.paidBy
        val expense = expenseService.getById(expenseId)

        val newDisbalance = expense.amount - newPaidBy.values.reduce(BigDecimal::add)
        if (BigDecimal.ZERO.compareTo(newDisbalance) != 0) {
            editPaidByView(expenseId, ctx, update)
            throw IllegalArgumentException("Недоплата/переплата не равна нулю: $newDisbalance")
        }

        expenseService.save(expense.copy(paidBy = newPaidBy))

        val group = groupService.getById(expense.groupId)
        showExpenseView(update, ctx, expense, group, "Изменения сохранены\n\n")
    }

    @OnCallback(prefix = BTN_TMP_CANCEL_EDIT_PAID_BY)
    fun onTmpCancelEditPaidBy(update: Update, ctx: UserContext) {
        val expenseId = update.callbackQuery.data.removePrefix(BTN_TMP_CANCEL_EDIT_PAID_BY).toLong()
        ctx.editingExpenses.remove(expenseId)
        val expense = expenseService.getById(expenseId)
        val group = groupService.getById(expense.groupId)
        showExpenseView(update, ctx, expense, group, "")
    }


    fun participantRow(
        expense: Expense,
        user: BotUserEntity,
    ): List<InlineKeyboardButton> {
        val amount = expense.paidBy[user.id!!]!!
        return row(
            btn("✅${user.getViewName()}", BTN_TMP_DELETE_FROM_PAID_BY + expense.id + ":" + user.id),
            btn(amount.toPlainString(), BTN_TMP_EDIT_PAID_AMOUNT + expense.id + ":" + user.id)
        )
    }

    fun notParticipantRow(
        editingExpense: Expense,
        user: BotUserEntity,
    ): List<InlineKeyboardButton> {
        return row(
            btn(user.getViewName(), "$BTN_TMP_EDIT_PAID_AMOUNT${editingExpense.id}:${user.id}"),
            btn("-", "$BTN_TMP_EDIT_PAID_AMOUNT${editingExpense.id}:${user.id}")
        )
    }

    fun splitParticipantRow(
        expense: Expense,
        user: BotUserEntity,
    ): List<InlineKeyboardButton> {
        val amount = expense.splitBy[user.id!!]!!
        val nameBtn = if (expense.splitType == SplitType.EQUALLY) {
            btn("✅${user.getViewName()}", BTN_DELETE_FROM_SPLIT_BY_EQUALLY + expense.id + ":" + user.id)
        } else {
            btn("✅${user.getViewName()}", BTN_TMP_DELETE_FROM_SPLIT_BY_NOT_EQUALLY + expense.id + ":" + user.id)
        }
        return row(
            nameBtn,
            btn(amount.toPlainString(), BTN_TMP_EDIT_SPLIT_AMOUNT + expense.id + ":" + user.id)
        )
    }

    fun notSplitParticipantRow(
        editingExpense: Expense,
        user: BotUserEntity,
    ): List<InlineKeyboardButton> {
        val amount = editingExpense.splitBy[user.id!!] ?: BigDecimal.ZERO
        val nameBtn = if (editingExpense.splitType == SplitType.EQUALLY) {
            btn(user.getViewName(), BTN_PUT_TO_SPLIT_BY_EQUALLY + editingExpense.id + ":" + user.id)
        } else {
            btn(user.getViewName(), BTN_TMP_EDIT_SPLIT_AMOUNT + editingExpense.id + ":" + user.id)
        }
        return row(
            nameBtn,
            btn(amount.toPlainString(), BTN_TMP_EDIT_SPLIT_AMOUNT + editingExpense.id + ":" + user.id)
        )
    }

    @OnCallback(prefix = BTN_TMP_DELETE_FROM_PAID_BY)
    fun onTmpDeleteFromPaidBy(update: Update, ctx: UserContext) {
        val (expenseId, userId) = update.callbackQuery.data.removePrefix(BTN_TMP_DELETE_FROM_PAID_BY)
            .split(":").map { it.toLong() }
        val expense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }
        val user = userService.getById(userId)

        expense.paidBy.remove(user.id)

        editPaidByView(expenseId, ctx, update)
    }

    @OnCallback(prefix = BTN_TMP_EDIT_PAID_AMOUNT)
    fun onTmpEditPaidAmount(update: Update, ctx: UserContext) {
        val (expenseId, userId) = update.callbackQuery.data.removePrefix(BTN_TMP_EDIT_PAID_AMOUNT)
            .split(":").map { it.toLong() }
        tmpEditPaidAmountView(update, ctx, expenseId, userId, "")
    }

    private fun tmpEditPaidAmountView(
        update: Update,
        ctx: UserContext,
        expenseId: Long,
        userId: Long,
        prefix: String,
    ) {

        val expense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }
        val user = userService.getById(userId)

        val disbalance = expense.amount - expense.paidBy.values.reduce(BigDecimal::add)

        ctx.state = "$STT_EDIT_PAID_AMOUNT$expenseId:$userId"
        bot.reply(update)
            .text(
                prefix + """
                    |Расход ${expense.description}
                    |Сумма: ${expense.amount}
                    |Платит: ${user.getViewName()}
                    |${if (disbalance > BigDecimal.ZERO) "🔴Текущая недоплата: $disbalance" else "🔴Текущая переплата: ${disbalance.negate()}"}
                    |
                    |Введите новую сумму:
                    """.trimMargin()
            ).inlineKeyboard(
                row("Ввести всю сумму", BTN_TMP_EDIT_PAID_AMOUNT_SET_FULL + expense.id + ":" + user.id)
                    .filter { disbalance > BigDecimal.ZERO },
                row("⬅️Отменить", BTN_EDIT_PAID_BY + expense.id),
            ).editIfCallbackMessageOrSend()
    }

    @OnMessage(state = STT_EDIT_PAID_AMOUNT)
    fun onEditPaidAmount(update: Update, ctx: UserContext) {
        val (expenseId, userId) = ctx.state.removePrefix(STT_EDIT_PAID_AMOUNT)
            .split(":").map { it.toLong() }
        val expense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }
        val user = userService.getById(userId)

        val newAmount = update.message.text.toBigDecimalOrNull()
            ?: run {
                tmpEditPaidAmountView(update, ctx, expenseId, userId, "Некорректная сумма\n")
                return
            }

        ctx.editingExpenses.getOrPut(expenseId) { expense }
            .paidBy[user.id!!] = newAmount

        ctx.cleanState()
        editPaidByView(expenseId, ctx, update)
    }

    @OnCallback(prefix = BTN_TMP_EDIT_PAID_AMOUNT_SET_FULL)
    fun onTmpEditPaidAmountSetFull(update: Update, ctx: UserContext) {
        val (expenseId, userId) = update.callbackQuery.data.removePrefix(BTN_TMP_EDIT_PAID_AMOUNT_SET_FULL)
            .split(":").map { it.toLong() }
        val expense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }
        val user = userService.getById(userId)

        val disbalance = expense.amount - expense.paidBy.values.reduce(BigDecimal::add)
        if (disbalance > BigDecimal.ZERO) {
            expense.paidBy[user.id!!] = disbalance + (expense.paidBy[user.id!!] ?: BigDecimal.ZERO)
        } else {
            throw IllegalArgumentException("Нет остатка для платежа")
        }
        ctx.cleanState()

        editPaidByView(expenseId, ctx, update)
    }

    @OnCallback(prefix = BTN_SET_SPLIT_EQUALLY)
    fun onSetSplitEqually(update: Update, ctx: UserContext) {
        val expenseId = update.callbackQuery.data.removePrefix(BTN_SET_SPLIT_EQUALLY).toLong()
        val expense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }

        val group = groupService.getById(expense.groupId)
        val members = group.members

        val splitByList = expense.splitBy.entries.toList()

        val idToUser = members.associateWith { userService.getById(it) }

        bot.reply(update)
            .text(
                """
                |Расход: ${expense.description}
                |Сумма: ${expense.amount}
                |
                |${
                    TableBuilder('-', " | ")
                        .column("Участник") { idx ->
                            val userId = splitByList[idx].key
                            idToUser[userId]?.getViewName()
                        }
                        .numberColumn("Сумма") { idx ->
                            splitByList[idx].value.toPlainString()
                        }.build()
                }
                |
                |Вы уверены, что хотите разделить расход поровну? Текущее распределение будет удалено.
                """.trimMargin(),
            )
            .inlineKeyboard(
                row("✅Поровну", BTN_SET_SPLIT_EQUALLY_APPROVE + expense.id),
                row("⬅️Отменить", BTN_VIEW_ONE_EXPENSE + expense.id),
            )
            .editIfCallbackMessageOrSend()
    }

    @OnCallback(prefix = BTN_SET_SPLIT_EQUALLY_APPROVE)
    fun onSetSplitEquallyApprove(update: Update, ctx: UserContext) {
        val expenseId = update.callbackQuery.data.removePrefix(BTN_SET_SPLIT_EQUALLY_APPROVE).toLong()
        setSplitEquallyApprove(expenseId, ctx, update)
    }

    private fun setSplitEquallyApprove(
        expenseId: Long,
        ctx: UserContext,
        update: Update
    ) {
        var expense = expenseService.getById(expenseId)

        val splitByUsers = expense.splitBy.keys
        val amount = expense.amount / splitByUsers.size.toBigDecimal()

        val newSplitBy = splitByUsers.associateWith { amount }

        expense = expenseService.save(expense.copy(splitBy = newSplitBy.toMutableMap(), splitType = SplitType.EQUALLY))

        ctx.cleanState()

        val group = groupService.getById(expense.groupId)
        showExpenseView(update, ctx, expense, group, "Распределение изменено\n\n")
    }

    @OnCallback(prefix = BTN_EDIT_SPLIT_BY)
    fun onEditSplitBy(update: Update, ctx: UserContext) {
        val expenseId = update.callbackQuery.data.removePrefix(BTN_EDIT_SPLIT_BY).toLong()
        editSplitByView(expenseId, ctx, update)
    }

    fun editSplitByView(
        expenseId: Long,
        ctx: UserContext,
        update: Update
    ) {
        val editingExpense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }
        val group = groupService.getById(editingExpense.groupId)
        val idToUser = group.members.associateWith { userService.getById(it) }

        val disbalance = editingExpense.amount - editingExpense.splitBy.values.reduce(BigDecimal::add)
        val splitByList = editingExpense.splitBy.entries.toList()

        bot.reply(update)
            .text(
                """
                    |Расход ${editingExpense.description}
                    |Сумма: ${editingExpense.amount}
                    |
                    |${
                    TableBuilder('-', " | ")
                        .column("Участник") { idx ->
                            val userId = splitByList[idx].key
                            idToUser[userId]?.getViewName()
                        }
                        .numberColumn("Сумма") { idx ->
                            splitByList[idx].value.toPlainString()
                        }.build()
                }
                    |
                    |${
                    if (disbalance > BigDecimal.ZERO) {
                        "🔴Не хватает: $disbalance"
                    } else if (disbalance < BigDecimal.ZERO) {
                        "🔴Избыток: ${disbalance.negate()}"
                    } else ""
                }
                    """.trimMargin()
            )
            .inlineKeyboard(
                *group.members.map { mId ->
                    if (editingExpense.splitBy.containsKey(mId)) {
                        splitParticipantRow(editingExpense, userService.getById(mId))
                    } else {
                        notSplitParticipantRow(editingExpense, userService.getById(mId))
                    }
                }.toTypedArray(),
                row("✅Сохранить", BTN_TMP_SAVE_SPLIT_BY + editingExpense.id),
                row("⬅️Отменить изменения", BTN_TMP_CANCEL_EDIT_SPLIT_BY + editingExpense.id),
            )
            .editIfCallbackMessageOrSend()
    }

    @OnCallback(prefix = BTN_TMP_SAVE_SPLIT_BY)
    fun onTmpSaveSplitBy(update: Update, ctx: UserContext) {
        val expenseId = update.callbackQuery.data.removePrefix(BTN_TMP_SAVE_SPLIT_BY).toLong()
        val newSplitBy = ctx.editingExpenses[expenseId]!!.splitBy
        var expense = expenseService.getById(expenseId)

        val newDisbalance = expense.amount - newSplitBy.values.reduce(BigDecimal::add)
        if (BigDecimal.ZERO.compareTo(newDisbalance) != 0) {
            editSplitByView(expenseId, ctx, update)
            throw IllegalArgumentException("Недоплата/переплата не равна нулю: $newDisbalance")
        }

        expense = expenseService.save(expense.copy(splitBy = newSplitBy, splitType = SplitType.NOT_EQUALLY))
        ctx.editingExpenses.remove(expenseId)

        val group = groupService.getById(expense.groupId)
        showExpenseView(update, ctx, expense, group, "Изменения сохранены\n\n")
    }

    @OnCallback(prefix = BTN_TMP_CANCEL_EDIT_SPLIT_BY)
    fun onTmpCancelEditSplitBy(update: Update, ctx: UserContext) {
        val expenseId = update.callbackQuery.data.removePrefix(BTN_TMP_CANCEL_EDIT_SPLIT_BY).toLong()
        ctx.editingExpenses.remove(expenseId)
        val expense = expenseService.getById(expenseId)
        val group = groupService.getById(expense.groupId)
        showExpenseView(update, ctx, expense, group, "")
    }

    @OnCallback(prefix = BTN_TMP_EDIT_SPLIT_AMOUNT)
    fun onTmpEditSplitAmount(update: Update, ctx: UserContext) {
        val (expenseId, userId) = update.callbackQuery.data.removePrefix(BTN_TMP_EDIT_SPLIT_AMOUNT)
            .split(":").map { it.toLong() }
        tmpEditSplitAmountView(update, ctx, expenseId, userId, "")
    }

    private fun tmpEditSplitAmountView(
        update: Update,
        ctx: UserContext,
        expenseId: Long,
        userId: Long,
        prefix: String,
    ) {
        val expense = expenseService.getById(expenseId)
        val user = userService.getById(userId)

        val disbalance = expense.amount - expense.splitBy.values.reduce(BigDecimal::add)

        ctx.state = "$STT_EDIT_SPLIT_AMOUNT$expenseId:$userId"
        bot.reply(update)
            .text(
                prefix + """
                |Расход: ${expense.description}
                |Сумма: ${expense.amount}
                |Участник: ${user.getViewName()}
                |${if (expense.splitBy.containsKey(user.id)) "Текущая сумма: ${expense.splitBy[user.id]}" else ""}
                |${
                    if (disbalance > BigDecimal.ZERO) "🔴Не достаточно распределено: $disbalance"
                    else if (disbalance < BigDecimal.ZERO) "🔴Избыток: ${disbalance.negate()}"
                    else ""
                }
                |
                |Введите новую сумму:
                """.trimMargin()
            ).inlineKeyboard(
                row("⬅️Отменить", BTN_EDIT_SPLIT_BY + expense.id)
                    .filter { ctx.editingExpenses.containsKey(expenseId) },
                row("⬅️Отменить", BTN_VIEW_ONE_EXPENSE + expense.id)
                    .filter { !ctx.editingExpenses.containsKey(expenseId) },
            ).editIfCallbackMessageOrSend()
    }

    @OnMessage(state = STT_EDIT_SPLIT_AMOUNT)
    fun onEditSplitAmount(update: Update, ctx: UserContext) {
        val (expenseId, userId) = ctx.state.removePrefix(STT_EDIT_SPLIT_AMOUNT)
            .split(":").map { it.toLong() }
        val expense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }
        val user = userService.getById(userId)

        val newAmount = update.message.text.toBigDecimalOrNull()
            ?: run {
                tmpEditSplitAmountView(update, ctx, expenseId, userId, "Некорректная сумма\n")
                return
            }

        expense.splitBy[user.id!!] = newAmount
        expense.splitType = SplitType.NOT_EQUALLY
        ctx.cleanState()

        editSplitByView(expenseId, ctx, update)
    }

    @OnCallback(prefix = BTN_PUT_TO_SPLIT_BY_EQUALLY)
    fun onPutToSplitByEqually(update: Update, ctx: UserContext) {
        val (expenseId, userId) = update.callbackQuery.data.removePrefix(BTN_PUT_TO_SPLIT_BY_EQUALLY)
            .split(":").map { it.toLong() }
        val expense = expenseService.getById(expenseId)

        if (expense.splitType != SplitType.EQUALLY) {
            throw IllegalArgumentException("Распределение не поровну")
        }

        expense.splitBy[userId] = BigDecimal.ZERO

        expenseService.save(expense)

        setSplitEquallyApprove(expenseId, ctx, update)
    }

    @OnCallback(prefix = BTN_DELETE_FROM_SPLIT_BY_EQUALLY)
    fun onDeleteFromSplitByEqually(update: Update, ctx: UserContext) {
        val (expenseId, userId) = update.callbackQuery.data.removePrefix(BTN_DELETE_FROM_SPLIT_BY_EQUALLY)
            .split(":").map { it.toLong() }
        val expense = expenseService.getById(expenseId)

        if (expense.splitType != SplitType.EQUALLY) {
            throw IllegalArgumentException("Распределение не поровну")
        }

        expense.splitBy.remove(userId)

        expenseService.save(expense)

        setSplitEquallyApprove(expenseId, ctx, update)
    }

    @OnCallback(prefix = BTN_TMP_DELETE_FROM_SPLIT_BY_NOT_EQUALLY)
    fun onDeleteFromSplitByNotEqually(update: Update, ctx: UserContext) {
        val (expenseId, userId) = update.callbackQuery.data.removePrefix(BTN_TMP_DELETE_FROM_SPLIT_BY_NOT_EQUALLY)
            .split(":").map { it.toLong() }
        val expense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }

        if (expense.splitType != SplitType.NOT_EQUALLY) {
            throw IllegalArgumentException("Распределение поровну")
        }

        expense.splitBy.remove(userId)

        editSplitByView(expenseId, ctx, update)
    }


}