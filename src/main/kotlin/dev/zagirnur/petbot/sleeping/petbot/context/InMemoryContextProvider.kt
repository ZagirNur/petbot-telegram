package dev.zagirnur.petbot.sleeping.petbot.context

import dev.zagirnur.petbot.sdk.ChatContext
import dev.zagirnur.petbot.sdk.ContextProvider
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.Update

@Service
class InMemoryContextProvider: HashMap<String, UserContext>(), ContextProvider {
    override fun getContext(update: Update?): UserContext {
        val chatId = update?.message?.chatId.toString()
        return this.getOrPut(chatId) { UserContext() }
    }

    override fun saveContext(update: Update?, ctx: ChatContext?) {
        val chatId = update?.message?.chatId.toString()
        this[chatId] = ctx as UserContext
    }
}