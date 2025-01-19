package dev.zagirnur.petbot.sleeping.petbot.dao.repository

import dev.zagirnur.petbot.sleeping.petbot.dao.entity.BotUserEntity
import dev.zagirnur.petbot.sleeping.petbot.dao.entity.ExpenseEntity
import dev.zagirnur.petbot.sleeping.petbot.dao.entity.GroupEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface GroupRepository : CrudRepository<GroupEntity, Long> {

    @Query("SELECT * FROM bot_group g WHERE g.members @> to_jsonb(:userId)", nativeQuery = true)
    fun findAllByMembersContaining(userId: Long): List<GroupEntity>
}


interface ExpenseRepository : CrudRepository<ExpenseEntity, Long> {
    fun findAllByGroupId(groupId: Long): List<ExpenseEntity>
}

interface UserRepository : CrudRepository<BotUserEntity, Long> {
    fun findByTelegramId(telegramId: Long): BotUserEntity?
}