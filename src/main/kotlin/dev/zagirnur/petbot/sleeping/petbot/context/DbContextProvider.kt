package dev.zagirnur.petbot.sleeping.petbot.context

import dev.zagirnur.petbot.sdk.BotUtils.getFrom
import dev.zagirnur.petbot.sdk.ChatContext
import dev.zagirnur.petbot.sdk.ContextProvider
import dev.zagirnur.petbot.sleeping.petbot.dao.entity.BotUserEntity
import dev.zagirnur.petbot.sleeping.petbot.exceptions.UserNotFoundException
import dev.zagirnur.petbot.sleeping.petbot.model.Lang
import dev.zagirnur.petbot.sleeping.petbot.service.UserService
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User

@Service
class DbContextProvider(
    val userService: UserService,
) : ContextProvider {

    override fun getContext(update: Update?): UserContext {
        val telegramUser = getFrom(update)
        val userEntity: BotUserEntity = userService.findByTelegramId(telegramUser.id)
            ?.let { updateUserIfChanged(it, telegramUser) }
            ?: userService.create(
                telegramId = telegramUser.id,
                telegramUserName = telegramUser.userName,
                firstName = telegramUser.firstName,
                lastName = telegramUser.lastName,
                lang = Lang.valueOf(telegramUser.languageCode?.uppercase() ?: "EN")
            )
        return userEntity.context
    }

    override fun saveContext(update: Update?, ctx: ChatContext?) {
        val telegramUser = getFrom(update)
        userService.findByTelegramId(telegramUser.id)
            ?.let { userService.updateContext(it.id!!, ctx as UserContext) }
            ?: throw UserNotFoundException(telegramUser.id)
    }

    private fun updateUserIfChanged(existsUser: BotUserEntity, telegramUser: User): BotUserEntity {
        if (existsUser.telegramUserName != telegramUser.userName) {
            userService.updateTelegramUserName(existsUser.id!!, telegramUser.userName)
        }
        if (existsUser.firstName != telegramUser.firstName) {
            userService.updateFirstName(existsUser.id!!, telegramUser.firstName)
        }
        if (existsUser.lastName != telegramUser.lastName) {
            userService.updateLastName(existsUser.id!!, telegramUser.lastName)
        }
        val lang = Lang.valueOf(telegramUser.languageCode?.uppercase() ?: "EN")
        if (existsUser.languageCode != lang) {
            userService.updateLanguage(existsUser.id!!, lang)
        }
        return existsUser
    }
}