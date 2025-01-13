package dev.zagirnur.petbot.sleeping.petbot.handler

import dev.zagirnur.petbot.sdk.BotSender
import dev.zagirnur.petbot.sdk.ReplyBuilder.btnSwitch
import dev.zagirnur.petbot.sdk.ReplyBuilder.row
import dev.zagirnur.petbot.sdk.annotations.OnCallback
import dev.zagirnur.petbot.sdk.annotations.OnInlineQuery
import dev.zagirnur.petbot.sdk.annotations.OnMessage
import dev.zagirnur.petbot.sleeping.petbot.context.EditingExpense
import dev.zagirnur.petbot.sleeping.petbot.context.UserContext
import dev.zagirnur.petbot.sleeping.petbot.service.GroupService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputMessageContent
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class GroupHandler(
    var bot: BotSender,
    var groupService: GroupService,
) {


    companion object {
        // button prefixes
        const val JOIN_GROUP = "JOIN_GROUP"

        // chat states
        const val WAITING_FOR_GROUP_NAME = "WAITING_FOR_GROUP_NAME"

    }

    @OnMessage(command = "/group")
    fun onExpense(update: Update, ctx: UserContext) {
        val groups = groupService.findAllByUserId(update.message.from.id)

        if (groups.isEmpty()) {
            ctx.state = WAITING_FOR_GROUP_NAME
            bot.reply(update)
                .text("У тебя нет групп, давай создадим! \nВведите название группы:")
                .send()
            return
        }
    }

    @OnMessage(state = WAITING_FOR_GROUP_NAME)
    fun onGroupName(update: Update, ctx: UserContext) {
        val groupName = update.message.text
        groupService.createGroup(update.message.from.id, groupName)

        ctx.state = WAITING_FOR_GROUP_NAME
        bot.reply(update)
            .text("Группа $groupName создана!")
            .inlineKeyboard(
                row(btnSwitch("Пригласить участника", groupName)),
            )
            .send()
    }

    @OnInlineQuery
    fun onInlineQuery(update: Update, ctx: UserContext) {
        val inlineQuery = update.inlineQuery
        val query = inlineQuery.query.trim()

        val groups = groupService.findAllByUserId(update.inlineQuery.from.id)
        val results = groups
            .filter { it.name.contains(query, ignoreCase = true) }
            .map {
                InlineQueryResultArticle()
                    .apply {
                        id = it.id.toString()
                        title = it.name
                        inputMessageContent = InputTextMessageContent().apply { messageText = it.name }
                        replyMarkup = InlineKeyboardMarkup(listOf(row("Участвовать", "$JOIN_GROUP${it.id}")))
                    }
            }

        bot.sendAnswerInlineQuery(inlineQuery.id, results)
    }

    @OnCallback(prefix = JOIN_GROUP)
    fun onJoinGroup(update: Update, ctx: UserContext) {
        val groupId = update.callbackQuery.data.removePrefix(JOIN_GROUP).toLong()
        val group = groupService.findById(groupId)

        if (group == null) {
            bot.reply(update)
                .text("Группа не найдена")
                .sendPopup()
            return
        }

        groupService.addMember(groupId, update.callbackQuery.from.id)

        bot.reply(update)
            .text("Вы присоединились к группе ${group.name}")
            .sendPopup()
    }


}