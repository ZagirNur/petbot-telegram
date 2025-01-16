package dev.zagirnur.petbot.sleeping.petbot.service

import dev.zagirnur.petbot.sleeping.petbot.dao.entity.ExpenseEntity
import dev.zagirnur.petbot.sleeping.petbot.dao.repository.ExpenseRepository
import dev.zagirnur.petbot.sleeping.petbot.model.Expense
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class ExpenseService(
    val expenseRepository: ExpenseRepository,
) {

    fun createNew(
        groupId: Long,
        amount: BigDecimal,
        description: String,
        paidBy: Map<Long, BigDecimal>,
        splitBy: Map<Long, BigDecimal>,
    ): Expense {
        val entity = expenseRepository.save(
            ExpenseEntity(
                groupId = groupId,
                amount = amount,
                description = description,
                paidBy = paidBy.toMutableMap(),
                splitBy = splitBy.toMutableMap()
            )
        )
        return Expense(
            amount = entity.amount,
            description = entity.description,
            groupId = entity.groupId,
            id = entity.id,
            paidBy = entity.paidBy,
            splitBy = entity.splitBy,
        )
    }

}