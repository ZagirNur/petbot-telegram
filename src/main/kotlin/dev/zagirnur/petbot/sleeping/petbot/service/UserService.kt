package dev.zagirnur.petbot.sleeping.petbot.service

import dev.zagirnur.petbot.sleeping.petbot.context.UserContext
import dev.zagirnur.petbot.sleeping.petbot.dao.entity.BotUserEntity
import dev.zagirnur.petbot.sleeping.petbot.dao.repository.UserRepository
import dev.zagirnur.petbot.sleeping.petbot.model.Lang
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    val userRepository: UserRepository,
) {

    fun findByTelegramId(id: Long): BotUserEntity? {
        return userRepository.findByTelegramId(id)
    }

    fun getById(key: Long): BotUserEntity {
        return userRepository.findById(key)
            .orElseThrow { IllegalArgumentException("User not found") }
    }

    fun create(
        telegramId: Long,
        telegramUserName: String,
        firstName: String?,
        lastName: String?,
        lang: Lang,
    ): BotUserEntity {
        val entity = BotUserEntity(
            telegramId = telegramId,
            telegramUserName = telegramUserName,
            firstName = firstName,
            lastName = lastName,
            languageCode = lang,
        )
        return userRepository.save(entity)
    }

    @Retryable(backoff = Backoff(delay = 1000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    fun updateContext(userId: Long, userContext: UserContext) {
        val user = getById(userId)
        user.context = userContext
        userRepository.save(user)
    }

    @Retryable(backoff = Backoff(delay = 1000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    fun updateTelegramUserName(userId: Long, telegramUserName: String?) {
        val user = getById(userId)
        user.telegramUserName = telegramUserName
        userRepository.save(user)
    }

    @Retryable(backoff = Backoff(delay = 1000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    fun updateLanguage(userId: Long, lang: Lang) {
        val user = getById(userId)
        user.languageCode = lang
        userRepository.save(user)
    }

    @Retryable(backoff = Backoff(delay = 1000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    fun updateFirstName(userId: Long, firstName: String?) {
        val user = getById(userId)
        user.firstName = firstName
        userRepository.save(user)
    }

    @Retryable(backoff = Backoff(delay = 1000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    fun updateLastName(userId: Long, lastName: String?) {
        val user = getById(userId)
        user.lastName = lastName
        userRepository.save(user)
    }

}