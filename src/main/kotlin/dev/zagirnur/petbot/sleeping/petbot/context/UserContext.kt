package dev.zagirnur.petbot.sleeping.petbot.context

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.zagirnur.petbot.sdk.ChatContext
import dev.zagirnur.petbot.sleeping.petbot.model.Expense


data class UserContext(
    val editingExpenses: MutableList<Expense> = mutableListOf(),
    var defaultGroup: Long? = null,
    var tagToMessageId: MutableMap<String, Long> = mutableMapOf(),
    var userChatState: String = "",
) : ChatContext {

    @JsonIgnore
    override fun getState(): String {
        return userChatState
    }

    @JsonIgnore
    override fun setState(state: String?) {
        userChatState = state?:""
    }

    override fun cleanState() {
        userChatState = ""
    }

    override fun getMessageIdByTag(tag: String): Long? {
        return tagToMessageId[tag]
    }

    override fun deleteTag(tag: String?) {
        tagToMessageId.remove(tag)
    }

    override fun tagMessageId(tag: String, messageId: Long) {
        tagToMessageId[tag] = messageId
    }

}


