package dev.zagirnur.petbot.sleeping.petbot.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class Expense(
    var amount: BigDecimal,
    var description: String,
    val groupId: Long,
    var id: Long? = null,
    val paidBy: MutableMap<Long, BigDecimal> = mutableMapOf(),
    val splitBy: MutableMap<Long, BigDecimal> = mutableMapOf(),
    var splitType: SplitType = SplitType.EQUALLY,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
