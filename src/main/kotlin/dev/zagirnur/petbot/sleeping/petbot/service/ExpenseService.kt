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
            splitType = entity.splitType,
        )
    }

    fun getGroupExpenses(groupId: Long): List<Expense> {
        return expenseRepository.findAllByGroupId(groupId)
            .map {
                Expense(
                    amount = it.amount,
                    description = it.description,
                    groupId = it.groupId,
                    id = it.id,
                    paidBy = it.paidBy,
                    splitBy = it.splitBy,
                    splitType = it.splitType,
                )
            }

    }

    fun getById(expenseId: Long): Expense {
        val entity = expenseRepository.findById(expenseId)
            .orElseThrow { IllegalArgumentException("Expense with id $expenseId not found") }
        return Expense(
            amount = entity.amount,
            description = entity.description,
            groupId = entity.groupId,
            id = entity.id,
            paidBy = entity.paidBy,
            splitBy = entity.splitBy,
            splitType = entity.splitType,
        )
    }

    fun save(copy: Expense) : Expense {
        expenseRepository.save(
            ExpenseEntity(
                id = copy.id,
                groupId = copy.groupId,
                amount = copy.amount,
                description = copy.description,
                paidBy = copy.paidBy.toMutableMap(),
                splitBy = copy.splitBy.toMutableMap(),
                splitType = copy.splitType
            )
        )
        return copy
    }

}