package dev.zagirnur.petbot.sleeping.petbot.handler

import dev.zagirnur.petbot.sdk.BotSender
import dev.zagirnur.petbot.sdk.BotUtils.getFrom
import dev.zagirnur.petbot.sdk.ReplyBuilder.btn
import dev.zagirnur.petbot.sdk.ReplyBuilder.row
import dev.zagirnur.petbot.sdk.StringUpdateData
import dev.zagirnur.petbot.sdk.TableBuilder
import dev.zagirnur.petbot.sdk.annotations.OnCallback
import dev.zagirnur.petbot.sdk.annotations.OnMessage
import dev.zagirnur.petbot.sleeping.petbot.context.UserContext
import dev.zagirnur.petbot.sleeping.petbot.dao.entity.BotUserEntity
import dev.zagirnur.petbot.sleeping.petbot.exceptions.UserNotFoundException
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler.Companion.BTN_EDIT_PAID_BY
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler.Companion.BTN_EDIT_SPLIT_BY
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler.Companion.BTN_GROUP_EXPENSES
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler.Companion.BTN_SET_SPLIT_EQUALLY
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler.Companion.BTN_SPLIT_BY_ADD_EQUALLY
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler.Companion.BTN_SPLIT_BY_DELETE_EQUALLY
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler.Companion.BTN_TMP_CANCEL_EDIT_PAID_BY
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler.Companion.BTN_TMP_CANCEL_EDIT_SPLIT_BY
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler.Companion.BTN_TMP_DELETE_FROM_PAID_BY
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler.Companion.BTN_TMP_EDIT_PAID_AMOUNT
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler.Companion.BTN_TMP_EDIT_PAID_AMOUNT_SET_FULL
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler.Companion.BTN_TMP_SAVE_PAID_BY
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler.Companion.BTN_TMP_SAVE_SPLIT_BY
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler.Companion.BTN_TMP_SPLIT_BY_DELETE
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler.Companion.BTN_TMP_SPLIT_BY_EDIT_AMOUNT
import dev.zagirnur.petbot.sleeping.petbot.handler.ExpenseHandler.Companion.BTN_VIEW_ONE_EXPENSE
import dev.zagirnur.petbot.sleeping.petbot.model.Expense
import dev.zagirnur.petbot.sleeping.petbot.model.Group
import dev.zagirnur.petbot.sleeping.petbot.model.SplitType
import dev.zagirnur.petbot.sleeping.petbot.model.SplitType.EQUALLY
import dev.zagirnur.petbot.sleeping.petbot.service.ExpenseService
import dev.zagirnur.petbot.sleeping.petbot.service.GroupService
import dev.zagirnur.petbot.sleeping.petbot.service.UserService
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
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
        const val BTN_ADD_NEW_EXPENSE_TO_GROUP = "ADD_NEW_EXPENSE_TO_GROUP:"


        const val BTN_SPLIT_BY_DELETE_EQUALLY = "DELETE_FROM_SPLIT_BY:"
        const val BTN_TMP_SPLIT_BY_DELETE = "DELETE_FROM_SPLIT_BY_NOT_EQUALLY:"
        const val BTN_SPLIT_BY_ADD_EQUALLY = "PUT_TO_SPLIT_BY:"
        const val BTN_TMP_SPLIT_BY_EDIT_AMOUNT = "TMP_EDIT_SPLIT_AMOUNT:"


        // chat states
        const val STT_EDIT_PAID_AMOUNT = "EDIT_PAID_AMOUNT:"
        const val STT_EDIT_SPLIT_AMOUNT = "EDIT_SPLIT_AMOUNT:"
        const val STT_WAITING_NEW_EXPENSE_AMOUNT = "WAITING_NEW_EXPENSE_AMOUNT:"

    }

    @OnCallback(prefix = "UNSUPPORTED")
    fun onUnsupported(update: Update, ctx: UserContext) {
        bot.reply(update)
            .text("Эта функция пока не поддерживается")
            .sendPopup()
    }

    //language=RegExp
    @OnMessage(regexp = """^\w?\s?[\d., ]+\D{0,3}\s.*$""")
    fun onCreateNewExpense(update: Update, ctx: UserContext) {
        val amount = update.message.text.split(" ")[0].replace(",", ".").toBigDecimal()
        val description = update.message.text.split(" ").drop(1).joinToString(" ")

        val user = getUser(update)
        val defaultGroup = ctx.defaultGroup
            ?.let { defaultGroupId ->
                groupService.getById(defaultGroupId)
            }
            ?: run {
                groupHandler.onGroups(update, ctx)
                return
            }

        val newExpense = expenseService.createNew(
            groupId = defaultGroup.id,
            amount = amount,
            description = description,
            paidBy = mapOf(user.id!! to amount),
            splitBy = mapOf(user.id!! to amount),
        )

        val model = toOneExpenseViewModel(newExpense, defaultGroup)
        bot.viewOneExpense(update, model, "Добавлен новый расход")
    }

    @OnCallback(prefix = BTN_GROUP_EXPENSES)
    fun onGroupExpenses(update: Update, ctx: UserContext, btn: StringUpdateData) {
        val group = groupService.getById(btn.data.toLong())
        val expenses = expenseService.getGroupExpenses(group.id)

        bot.reply(update)
            .text(
                """
                |
                |Расходы группы ${group.name}:
                |
                |${
                    TableBuilder('-', " | ")
                        .column("Название") { idx -> expenses[idx].description }
                        .numberColumn("Сумма") { idx -> expenses[idx].amount.toPlainString() }
                        .column("Дата") { idx -> expenses[idx].createdAt.toLocalDate().toString() }
                        .build()
                }
                |""".trimMargin()
            )
            .inlineKeyboard(
                row("➕Добавить расход", BTN_ADD_NEW_EXPENSE_TO_GROUP + group.id),
                *expenses.map {
                    btn(it.description, BTN_VIEW_ONE_EXPENSE + it.id)
                }.chunked(2).toTypedArray(),
                row("⬅️Список групп", GroupHandler.BTN_VIEW_ALL_GROUPS),
            )
            .editIfCallbackMessageOrSend()
    }

    @OnCallback(prefix = BTN_ADD_NEW_EXPENSE_TO_GROUP)
    fun onAddNewExpenseToGroup(update: Update, ctx: UserContext, btn: StringUpdateData) {
        val group = groupService.getById(btn.data.toLong())

        ctx.state = STT_WAITING_NEW_EXPENSE_AMOUNT + group.id
        bot.reply(update)
            .text(
                """
                |Группа: ${group.name}
                |Введите сумму и описание нового расхода через пробел:
                """.trimMargin()
            )
            .editIfCallbackMessageOrSend()
    }

    //language=RegExp
    @OnMessage(state = STT_WAITING_NEW_EXPENSE_AMOUNT, regexp = """^\w?\s?[\d., ]+\D{0,3}\s.*$""")
    fun onNewExpenseAmount(update: Update, ctx: UserContext) {
        val groupId = ctx.state.removePrefix(STT_WAITING_NEW_EXPENSE_AMOUNT).toLong()
        val group = groupService.getById(groupId)
        val user = getUser(update)

        val (amount, description) = update.message.text.split(" ", limit = 2)
        val newExpense = expenseService.createNew(
            groupId = group.id,
            amount = amount.toBigDecimal(),
            description = description,
            paidBy = mapOf(user.id!! to amount.toBigDecimal()),
            splitBy = mapOf(user.id!! to amount.toBigDecimal()),
        )

        ctx.state = ""
        val model = toOneExpenseViewModel(newExpense, group)
        bot.viewOneExpense(update, model, "Добавлен новый расход")
    }

    @OnCallback(prefix = BTN_VIEW_ONE_EXPENSE)
    fun onViewOneExpense(update: Update, ctx: UserContext, btn: StringUpdateData) {
        val expense = expenseService.getById(btn.data.toLong())
        val group = groupService.getById(expense.groupId)

        val model = toOneExpenseViewModel(expense, group)
        bot.viewOneExpense(update, model, "")
    }

    @OnCallback(prefix = BTN_EDIT_PAID_BY)
    fun onViewEditPaidBy(update: Update, ctx: UserContext, btn: StringUpdateData) {
        val expenseId = btn.data.toLong()
        val editingExpense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }
        val group = groupService.getById(editingExpense.groupId)

        val viewModel = toEditPaidByViewModel(editingExpense, group)
        bot.viewEditPaidBy(update, viewModel)
    }

    @OnCallback(prefix = BTN_TMP_SAVE_PAID_BY)
    fun onSavePaidBy(update: Update, ctx: UserContext, btn: StringUpdateData) {
        val expenseId = btn.data.toLong()
        val newPaidBy = ctx.editingExpenses[expenseId]!!.paidBy
        val expense = expenseService.getById(expenseId)
        val group = groupService.getById(expense.groupId)

        val newDisbalance = expense.amount - newPaidBy.values.reduce(BigDecimal::add)
        if (BigDecimal.ZERO.compareTo(newDisbalance) != 0) {
            val model = toEditPaidByViewModel(expense, group)
            bot.viewEditPaidBy(update, model)
            throw IllegalArgumentException("Недоплата/переплата не равна нулю: $newDisbalance")
        }

        expenseService.save(expense.copy(paidBy = newPaidBy))

        val model = toOneExpenseViewModel(expense, group)
        bot.viewOneExpense(update, model, "Изменения сохранены")
    }

    @OnCallback(prefix = BTN_TMP_CANCEL_EDIT_PAID_BY)
    fun onTmpCancelEditPaidBy(update: Update, ctx: UserContext, btn: StringUpdateData) {
        val expenseId = btn.data.toLong()
        ctx.editingExpenses.remove(expenseId)
        val expense = expenseService.getById(expenseId)
        val group = groupService.getById(expense.groupId)

        val model = toOneExpenseViewModel(expense, group)
        bot.viewOneExpense(update, model, "")
    }

    @OnCallback(prefix = BTN_TMP_DELETE_FROM_PAID_BY)
    fun onTmpDeleteFromPaidBy(update: Update, ctx: UserContext, btn: StringUpdateData) {
        val (expenseId, userId) = btn.data.split(":").map { it.toLong() }
        val editingExpense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }
        val group = groupService.getById(editingExpense.groupId)

        editingExpense.paidBy.remove(userId)

        val model = toEditPaidByViewModel(editingExpense, group)
        bot.viewEditPaidBy(update, model)
    }

    @OnCallback(prefix = BTN_TMP_EDIT_PAID_AMOUNT)
    fun onTmpEditPaidAmount(update: Update, ctx: UserContext, btn: StringUpdateData) {
        val (expenseId, userId) = btn.data.split(":").map { it.toLong() }
        val expense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }
        val user = userService.getById(userId)

        val disbalance = expense.amount - expense.paidBy.values.reduce(BigDecimal::add)

        ctx.state = "$STT_EDIT_PAID_AMOUNT$expenseId:$userId"

        val paidBySorted = expense.paidBy.entries.sortedBy { it.key }
            .map { userService.getById(it.key) to it.value }
        bot.viewInputPaidAmount(update, expense, user, disbalance, paidBySorted)
    }

    @OnMessage(state = STT_EDIT_PAID_AMOUNT)
    fun onEditPaidAmount(update: Update, ctx: UserContext) {
        val (expenseId, userId) = ctx.state.removePrefix(STT_EDIT_PAID_AMOUNT)
            .split(":").map { it.toLong() }
        val expense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }
        val user = userService.getById(userId)

        val newAmount = update.message.text.toBigDecimalOrNull()
            ?: run {
                val disbalance = expense.amount - expense.paidBy.values.reduce(BigDecimal::add)
                val paidBySorted = expense.paidBy.entries.sortedBy { it.key }
                    .map { userService.getById(it.key) to it.value }
                bot.viewInputPaidAmount(update, expense, user, disbalance, paidBySorted, "Некорректная сумма")
                return
            }

        ctx.editingExpenses.getOrPut(expenseId) { expense }
            .paidBy[user.id!!] = newAmount
        ctx.cleanState()

        val group = groupService.getById(expense.groupId)
        val model = toEditPaidByViewModel(expense, group)
        bot.viewEditPaidBy(update, model)
    }

    @OnCallback(prefix = BTN_TMP_EDIT_PAID_AMOUNT_SET_FULL)
    fun onTmpEditPaidAmountSetFull(update: Update, ctx: UserContext, btn: StringUpdateData) {
        val (expenseId, userId) = btn.data.split(":").map { it.toLong() }
        val expense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }
        val user = userService.getById(userId)

        val disbalance = expense.amount - expense.paidBy.values.reduce(BigDecimal::add)
        if (disbalance > BigDecimal.ZERO) {
            expense.paidBy[user.id!!] = disbalance + (expense.paidBy[user.id!!] ?: BigDecimal.ZERO)
        } else {
            throw IllegalArgumentException("Нет остатка для платежа")
        }
        ctx.cleanState()

        val group = groupService.getById(expense.groupId)
        val model = toEditPaidByViewModel(expense, group)
        bot.viewEditPaidBy(update, model)
    }

    @OnCallback(prefix = BTN_SET_SPLIT_EQUALLY)
    fun onSetSplitEqually(update: Update, ctx: UserContext, btn: StringUpdateData) {
        val expenseId = btn.data.toLong()
        val expense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }

        val sortedSplitBy = expense.splitBy.entries.sortedBy { it.key }
            .map { userService.getById(it.key) to it.value }

        bot.reply(update)
            .text(
                """
                |Расход: ${expense.description}
                |Сумма: ${expense.amount}
                |
                |${
                    TableBuilder('-', " | ")
                        .column("Участник") { idx -> sortedSplitBy[idx].first.getViewName() }
                        .numberColumn("Сумма") { idx -> sortedSplitBy[idx].second.toPlainString() }
                        .build()
                }
                |
                |Вы уверены, что хотите разделить расход поровну?
                |Текущее распределение будет удалено.
                """.trimMargin(),
            )
            .inlineKeyboard(
                row("✅Поровну", BTN_SET_SPLIT_EQUALLY_APPROVE + expense.id),
                row("⬅️Отменить", BTN_VIEW_ONE_EXPENSE + expense.id),
            )
            .editIfCallbackMessageOrSend()
    }

    @OnCallback(prefix = BTN_SET_SPLIT_EQUALLY_APPROVE)
    fun onSetSplitEquallyApprove(update: Update, ctx: UserContext, btn: StringUpdateData) {
        val expenseId = btn.data.toLong()
        setSplitEquallyApprove(expenseId, ctx, update)
    }

    private fun setSplitEquallyApprove(
        expenseId: Long,
        ctx: UserContext,
        update: Update
    ) {
        var expense = expenseService.getById(expenseId)
        val group = groupService.getById(expense.groupId)

        val amount = expense.amount / expense.splitBy.size.toBigDecimal()
        val newSplitBy = expense.splitBy.keys.associateWith { amount }

        expense = expenseService.save(
            expense.copy(
                splitBy = newSplitBy.toMutableMap(),
                splitType = EQUALLY
            )
        )

        ctx.cleanState()

        val model = toOneExpenseViewModel(expense, group)
        bot.viewOneExpense(update, model)
    }

    @OnCallback(prefix = BTN_TMP_SPLIT_BY_EDIT_AMOUNT)
    fun onTmpEditSplitAmount(update: Update, ctx: UserContext, btn: StringUpdateData) {
        val (expenseId, userId) = btn.data.split(":").map { it.toLong() }
        val expense = ctx.editingExpenses.getOrElse(expenseId) { expenseService.getById(expenseId) }
        val user = userService.getById(userId)
        val disbalance = expense.amount - expense.splitBy.values.reduce(BigDecimal::add)
        val splitBySorted = expense.splitBy.entries.sortedBy { it.key }
            .map { userService.getById(it.key) to it.value }
        val fromOneExpenseView = !ctx.editingExpenses.containsKey(expenseId)

        ctx.state = "$STT_EDIT_SPLIT_AMOUNT$expenseId:$userId"

        bot.viewInputSplitAmount(update, expense, user, disbalance, splitBySorted, fromOneExpenseView)
    }

    @OnMessage(state = STT_EDIT_SPLIT_AMOUNT)
    fun onEditSplitAmount(update: Update, ctx: UserContext) {
        val (expenseId, userId) = ctx.state.removePrefix(STT_EDIT_SPLIT_AMOUNT)
            .split(":").map { it.toLong() }
        val expense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }
        val user = userService.getById(userId)

        val newAmount = update.message.text.toBigDecimalOrNull()
            ?: run {
                val disbalance = expense.amount - expense.splitBy.values.reduce(BigDecimal::add)
                val splitBySorted = expense.splitBy.entries.sortedBy { it.key }
                    .map { userService.getById(it.key) to it.value }
                bot.viewInputSplitAmount(update, expense, user, disbalance, splitBySorted, false, "Некорректная сумма")
                return
            }

        expense.splitBy[user.id!!] = newAmount
        expense.splitType = SplitType.NOT_EQUALLY
        ctx.cleanState()

        val group = groupService.getById(expense.groupId)
        val model = toEditSplitUnequallyViewModel(expense, group)
        bot.viewEditSplitUnequally(update, model)
    }

    @OnCallback(prefix = BTN_EDIT_SPLIT_BY)
    fun onEditSplitBy(update: Update, ctx: UserContext, btn: StringUpdateData) {
        val expenseId = btn.data.toLong()
        val editingExpense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }
        val group = groupService.getById(expenseId)
        val model = toEditSplitUnequallyViewModel(editingExpense, group)
        bot.viewEditSplitUnequally(update, model)
    }

    @OnCallback(prefix = BTN_TMP_SAVE_SPLIT_BY)
    fun onTmpSaveSplitBy(update: Update, ctx: UserContext, btn: StringUpdateData) {
        val expenseId = btn.data.toLong()
        val editingExpense = ctx.editingExpenses[expenseId]!!
        var expense = expenseService.getById(expenseId)
        val newSplitBy = editingExpense.splitBy
        editingExpense.amount = expense.amount
        expense = expense.copy(splitBy = newSplitBy, splitType = SplitType.NOT_EQUALLY)

        val newDisbalance = editingExpense.amount - newSplitBy.values.reduce(BigDecimal::add)
        if (BigDecimal.ZERO.compareTo(newDisbalance) != 0) {
            val group = groupService.getById(editingExpense.groupId)
            val model = toEditSplitUnequallyViewModel(editingExpense, group)
            bot.viewEditSplitUnequally(update, model)
            throw IllegalArgumentException("Недоплата/переплата не равна нулю: $newDisbalance")
        }

        expenseService.save(expense)
        ctx.editingExpenses.remove(expenseId)

        val group = groupService.getById(expense.groupId)

        val model = toOneExpenseViewModel(expense, group)
        bot.viewOneExpense(update, model, "Изменения сохранены")
    }

    @OnCallback(prefix = BTN_TMP_CANCEL_EDIT_SPLIT_BY)
    fun onTmpCancelEditSplitBy(update: Update, ctx: UserContext) {
        val expenseId = update.callbackQuery.data.removePrefix(BTN_TMP_CANCEL_EDIT_SPLIT_BY).toLong()
        ctx.editingExpenses.remove(expenseId)
        val expense = expenseService.getById(expenseId)
        val group = groupService.getById(expense.groupId)

        val model = toOneExpenseViewModel(expense, group)
        bot.viewOneExpense(update, model, "")
    }

    @OnCallback(prefix = BTN_SPLIT_BY_ADD_EQUALLY)
    fun onPutToSplitByEqually(update: Update, ctx: UserContext) {
        val (expenseId, userId) = update.callbackQuery.data.removePrefix(BTN_SPLIT_BY_ADD_EQUALLY)
            .split(":").map { it.toLong() }
        val expense = expenseService.getById(expenseId)

        if (expense.splitType != EQUALLY) {
            throw IllegalArgumentException("Распределение не поровну")
        }

        expense.splitBy[userId] = BigDecimal.ZERO
        expenseService.save(expense)

        setSplitEquallyApprove(expenseId, ctx, update)
    }

    @OnCallback(prefix = BTN_SPLIT_BY_DELETE_EQUALLY)
    fun onDeleteFromSplitByEqually(update: Update, ctx: UserContext) {
        val (expenseId, userId) = update.callbackQuery.data.removePrefix(BTN_SPLIT_BY_DELETE_EQUALLY)
            .split(":").map { it.toLong() }
        val expense = expenseService.getById(expenseId)

        if (expense.splitType != EQUALLY) {
            throw IllegalArgumentException("Распределение не поровну")
        }

        expense.splitBy.remove(userId)

        expenseService.save(expense)

        setSplitEquallyApprove(expenseId, ctx, update)
    }

    @OnCallback(prefix = BTN_TMP_SPLIT_BY_DELETE)
    fun onDeleteFromSplitByNotEqually(update: Update, ctx: UserContext) {
        val (expenseId, userId) = update.callbackQuery.data.removePrefix(BTN_TMP_SPLIT_BY_DELETE)
            .split(":").map { it.toLong() }
        val expense = ctx.editingExpenses.getOrPut(expenseId) { expenseService.getById(expenseId) }

        if (expense.splitType != SplitType.NOT_EQUALLY) {
            throw IllegalArgumentException("Распределение поровну")
        }

        expense.splitBy.remove(userId)

        val group = groupService.getById(expense.groupId)
        val model = toEditSplitUnequallyViewModel(expense, group)
        bot.viewEditSplitUnequally(update, model)
    }

    private fun toOneExpenseViewModel(
        expense: Expense,
        defaultGroup: Group
    ): OneExpenseViewModel {
        val idToUser = defaultGroup.members.associateWith { userService.getById(it) }
        return OneExpenseViewModel(
            expense = expense,
            group = defaultGroup,
            sortedPaidBy = expense.paidBy
                .map { (id, amount) -> idToUser[id]!! to amount }
                .sortedBy { it.first.id },
            sortedSplitBy = expense.splitBy
                .map { (userId, amount) -> idToUser[userId]!! to amount }
                .sortedBy { it.first.id },
            sortedGroupMembers = idToUser.values.sortedBy { it.id },
        )
    }

    private fun toEditPaidByViewModel(
        editingExpense: Expense,
        group: Group
    ) = EditPaidByViewModel(
        expense = editingExpense,
        group = group,
        sortedPaidBy = editingExpense.paidBy
            .map { (id, amount) -> userService.getById(id) to amount }
            .sortedBy { it.first.id },
        disbalance = editingExpense.amount - editingExpense.paidBy.values.reduce(BigDecimal::add),
        sortedGroupMembers = group.members.map { userService.getById(it) }.sortedBy { it.id },
    )

    private fun toEditSplitUnequallyViewModel(
        editingExpense: Expense,
        group: Group
    ) = EditSplitUnequallyViewModel(
        expense = editingExpense,
        group = group,
        sortedSplitBy = editingExpense.splitBy
            .map { (id, amount) -> userService.getById(id) to amount }
            .sortedBy { it.first.id },
        disbalance = editingExpense.amount - editingExpense.splitBy.values.reduce(BigDecimal::add),
        sortedGroupMembers = group.members.map { userService.getById(it) }.sortedBy { it.id },
    )

    private fun getUser(update: Update) =
        (userService.findByTelegramId(getFrom(update).id)
            ?: throw UserNotFoundException(getFrom(update).id))
}

data class OneExpenseViewModel(
    val expense: Expense,
    val group: Group,
    val sortedPaidBy: List<Pair<BotUserEntity, BigDecimal>>,
    val sortedSplitBy: List<Pair<BotUserEntity, BigDecimal>>,
    val sortedGroupMembers: List<BotUserEntity>,
)

fun BotSender.viewOneExpense(
    update: Update,
    viewModel: OneExpenseViewModel,
    prefix: String = ""
) {
    val expense = viewModel.expense
    val splitBy = viewModel.sortedSplitBy
    val paidBy = viewModel.sortedPaidBy
    reply(update)
        .text(
            """
            |${prefix}
            |
            |Описание: ${expense.description}
            |Сумма: ${expense.amount}
            |Распределение: ${if (expense.splitType == EQUALLY) "поровну" else "не поровну"}
            |
            |${
                TableBuilder('-', " | ")
                    .column("Заплатил") { idx -> paidBy[idx].first.getViewName() }
                    .numberColumn("Сумма") { idx -> paidBy[idx].second.toPlainString() }
                    .build()
            }
            |
            |${
                TableBuilder('-', " | ")
                    .column("Участник") { idx -> splitBy[idx].first.getViewName() }
                    .numberColumn("Сумма") { idx -> splitBy[idx].second.toPlainString() }
                    .build()
            }
            | 
            """.trimMargin()
        )
        .inlineKeyboard(
            row("✏️Плательщики", BTN_EDIT_PAID_BY + expense.id),
            row("🤝Разделить поровну", BTN_SET_SPLIT_EQUALLY + expense.id)
                .filter { expense.splitType != EQUALLY },
            *viewModel.sortedGroupMembers.map { user ->
                val userSplitAmount = expense.splitBy[user.id!!] ?: BigDecimal.ZERO
                val isSplit = expense.splitBy.containsKey(user.id)
                val nameBtnAction = when (expense.splitType) {
                    EQUALLY -> if (isSplit) BTN_SPLIT_BY_DELETE_EQUALLY else BTN_SPLIT_BY_ADD_EQUALLY
                    else -> if (isSplit) BTN_TMP_SPLIT_BY_DELETE else BTN_TMP_SPLIT_BY_EDIT_AMOUNT
                }
                val check = if (isSplit) "✅" else ""
                row(
                    btn("$check${user.getViewName()}", nameBtnAction + expense.id + ":" + user.id),
                    btn("$userSplitAmount", BTN_TMP_SPLIT_BY_EDIT_AMOUNT + expense.id + ":" + user.id)
                )
            }.toTypedArray(),
            row("⬅️Расходы группы", BTN_GROUP_EXPENSES + expense.groupId),
        )
        .editIfCallbackMessageOrSend()
}

data class EditPaidByViewModel(
    val expense: Expense,
    val group: Group,
    val sortedPaidBy: List<Pair<BotUserEntity, BigDecimal>>,
    val disbalance: BigDecimal,
    val sortedGroupMembers: List<BotUserEntity>,
)

fun BotSender.viewEditPaidBy(
    update: Update,
    viewModel: EditPaidByViewModel,
    prefix: String = ""
) {
    val editingExpense = viewModel.expense
    val paidBy = viewModel.sortedPaidBy
    reply(update)
        .text(
            """
            |${prefix}
            |Расход ${editingExpense.description}
            |Сумма: ${editingExpense.amount}
            |
            |${
                TableBuilder('-', " | ")
                    .column("Плательщик") { idx -> paidBy[idx].first.getViewName() }
                    .numberColumn("Сумма") { idx -> paidBy[idx].second.toPlainString() }
                    .build()
            }
            |
            |${
                if (viewModel.disbalance > BigDecimal.ZERO) {
                    "🔴Недоплата: $viewModel.disbalance"
                } else if (viewModel.disbalance < BigDecimal.ZERO) {
                    "🔴Переплата: ${viewModel.disbalance.negate()}"
                } else ""
            }
            """.trimMargin()
        )
        .inlineKeyboard(
            *viewModel.sortedGroupMembers.map { m ->
                val amount = editingExpense.paidBy[m.id!!] ?: BigDecimal.ZERO
                val check = if (editingExpense.paidBy.containsKey(m.id)) "✅" else ""
                val nameBtnAction =
                    if (editingExpense.paidBy.containsKey(m.id))
                        BTN_TMP_DELETE_FROM_PAID_BY
                    else BTN_TMP_EDIT_PAID_AMOUNT
                row(
                    btn("$check${m.getViewName()}", nameBtnAction + editingExpense.id + ":" + m.id),
                    btn("$amount", BTN_TMP_EDIT_PAID_AMOUNT + editingExpense.id + ":" + m.id)
                )
            }.toTypedArray(),
            row("✅Сохранить", BTN_TMP_SAVE_PAID_BY + editingExpense.id),
            row("⬅️Отменить изменения", BTN_TMP_CANCEL_EDIT_PAID_BY + editingExpense.id),
        )
        .editIfCallbackMessageOrSend()

}

fun BotSender.viewInputPaidAmount(
    update: Update,
    expense: Expense,
    user: BotUserEntity,
    disbalance: BigDecimal,
    paidBySorted: List<Pair<BotUserEntity, BigDecimal>>,
    prefix: String = ""
) {
    reply(update)
        .text(
            """
                |$prefix
                |
                |Расход ${expense.description}
                |Сумма: ${expense.amount}
                |${
                if (disbalance > BigDecimal.ZERO) "🔴Не хватает: $disbalance"
                else if (disbalance < BigDecimal.ZERO) "🔴Избыток: ${disbalance.negate()}"
                else ""
            }
                |
                |${
                TableBuilder('-', " | ")
                    .column("Заплатил") { idx -> paidBySorted[idx].first.getViewName() }
                    .numberColumn("Сумма") { idx -> paidBySorted[idx].second.toPlainString() }
                    .build()
            }
                |
                |Введите сколько заплатил ${user.getViewName()}:
                """.trimMargin()
        ).inlineKeyboard(
            row("Ввести всю сумму", BTN_TMP_EDIT_PAID_AMOUNT_SET_FULL + expense.id + ":" + user.id)
                .filter { disbalance > BigDecimal.ZERO },
            row("⬅️Отменить", BTN_EDIT_PAID_BY + expense.id),
        ).editIfCallbackMessageOrSend()
}

data class EditSplitUnequallyViewModel(
    val expense: Expense,
    val group: Group,
    val sortedSplitBy: List<Pair<BotUserEntity, BigDecimal>>,
    val disbalance: BigDecimal,
    val sortedGroupMembers: List<BotUserEntity>,
)

fun BotSender.viewEditSplitUnequally(
    update: Update,
    viewModel: EditSplitUnequallyViewModel,
    prefix: String = ""
) {
    val editingExpense = viewModel.expense
    val splitBy = viewModel.sortedSplitBy
    reply(update)
        .text(
            """
            |${prefix}
            |Расход ${editingExpense.description}
            |Сумма: ${editingExpense.amount}
            |
            |${
                TableBuilder('-', " | ")
                    .column("Участник") { idx -> splitBy[idx].first.getViewName() }
                    .numberColumn("Сумма") { idx -> splitBy[idx].second.toPlainString() }
                    .build()
            }
            |
            |${
                if (viewModel.disbalance > BigDecimal.ZERO) {
                    "🔴Недоплата: ${viewModel.disbalance}"
                } else if (viewModel.disbalance < BigDecimal.ZERO) {
                    "🔴Переплата: ${viewModel.disbalance.negate()}"
                } else ""
            }
            """.trimMargin()
        )
        .inlineKeyboard(
            *viewModel.sortedGroupMembers.map { m ->
                val amount = editingExpense.splitBy[m.id!!] ?: BigDecimal.ZERO
                val check = if (editingExpense.splitBy.containsKey(m.id)) "✅" else ""
                val nameBtnAction =
                    if (editingExpense.splitBy.containsKey(m.id))
                        BTN_TMP_SPLIT_BY_DELETE
                    else BTN_TMP_SPLIT_BY_EDIT_AMOUNT
                row(
                    btn("$check${m.getViewName()}", nameBtnAction + editingExpense.id + ":" + m.id),
                    btn("$amount", BTN_TMP_SPLIT_BY_EDIT_AMOUNT + editingExpense.id + ":" + m.id)
                )
            }.toTypedArray(),
            row("✅Сохранить", BTN_TMP_SAVE_SPLIT_BY + editingExpense.id),
            row("⬅️Отменить изменения", BTN_TMP_CANCEL_EDIT_SPLIT_BY + editingExpense.id),
        )
        .editIfCallbackMessageOrSend()
}

fun BotSender.viewInputSplitAmount(
    update: Update,
    expense: Expense,
    user: BotUserEntity,
    disbalance: BigDecimal,
    splitBySorted: List<Pair<BotUserEntity, BigDecimal>>,
    fromOneExpenseView: Boolean,
    prefix: String = ""
) {
    reply(update)
        .text(
            """
                |$prefix
                |
                |Расход ${expense.description}
                |Сумма: ${expense.amount}
                |${
                if (disbalance > BigDecimal.ZERO) "🔴Не хватает: $disbalance"
                else if (disbalance < BigDecimal.ZERO) "🔴Избыток: ${disbalance.negate()}"
                else ""
            }
                |
                |${
                TableBuilder('-', " | ")
                    .column("Участник") { idx -> splitBySorted[idx].first.getViewName() }
                    .numberColumn("Сумма") { idx -> splitBySorted[idx].second.toPlainString() }
                    .build()
            }
                |
                |Введите сколько должен ${user.getViewName()}:
                """.trimMargin()
        ).inlineKeyboard(
            row("⬅️Отменить", BTN_EDIT_SPLIT_BY + expense.id)
                .filter { !fromOneExpenseView },
            row("⬅️Отменить", BTN_VIEW_ONE_EXPENSE + expense.id)
                .filter { fromOneExpenseView },
        ).editIfCallbackMessageOrSend()
}
