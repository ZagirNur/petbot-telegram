package dev.zagirnur.petbot.sleeping.petbot.context

import dev.zagirnur.petbot.sdk.ChatContext
import java.math.BigDecimal
import java.time.LocalDateTime

private const val STATE_KEY = "chatState"

data class UserContext(
    val editingExpenses: MutableList<EditingExpense> = mutableListOf()
) : ChatContext {
    private var state: String = ""

    override fun getState(): String {
        return state
    }

    override fun setState(state: String?) {
        this.state = state ?: ""
    }

    override fun cleanState() {
        state = ""
    }

}

data class EditingExpense(
    val amount: BigDecimal,
    val description: String,
    val paidBy: MutableMap<Long, BigDecimal> = mutableMapOf(),
    val whoSplitIt: MutableMap<Long, BigDecimal> = mutableMapOf(),
    var id: Int? = null,
    val splitType: SplitType = SplitType.EQUALLY,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

enum class SplitType {
    EQUALLY,
    BY_AMOUNT,
}
