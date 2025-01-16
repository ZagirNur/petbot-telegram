package dev.zagirnur.petbot.sleeping.petbot.exceptions

data class UserNotFoundException(val userId: Long) : RuntimeException()
data class GroupNotFoundException(val groupId: Long) : RuntimeException()
data class ExpenseNotFoundException(val groupId: Long, val expenseId: Long) : RuntimeException()
data class UserAlreadyInGroupException(val userId: Long, val groupId: Long) : RuntimeException()
data class UserNotInGroupException(val userId: Long, val groupId: Long) : RuntimeException()
