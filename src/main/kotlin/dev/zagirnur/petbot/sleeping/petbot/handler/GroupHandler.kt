package dev.zagirnur.petbot.sleeping.petbot.handler

import dev.zagirnur.petbot.sdk.BotSender
import dev.zagirnur.petbot.sdk.BotUtils.getFrom
import dev.zagirnur.petbot.sdk.ReplyBuilder.*
import dev.zagirnur.petbot.sdk.TableBuilder
import dev.zagirnur.petbot.sdk.annotations.OnCallback
import dev.zagirnur.petbot.sdk.annotations.OnInlineQuery
import dev.zagirnur.petbot.sdk.annotations.OnMessage
import dev.zagirnur.petbot.sleeping.petbot.context.UserContext
import dev.zagirnur.petbot.sleeping.petbot.exceptions.UserNotFoundException
import dev.zagirnur.petbot.sleeping.petbot.model.Group
import dev.zagirnur.petbot.sleeping.petbot.service.GroupService
import dev.zagirnur.petbot.sleeping.petbot.service.UserService
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup

@Component
class GroupHandler(
    var bot: BotSender,
    var groupService: GroupService,
    private val userService: UserService,
) {

    companion object {
        // button prefixes
        const val BTN_ENTER_NEW_GROUP_NAME = "BTN_ENTER_NEW_GROUP_NAME:"
        const val BTN_VIEW_ONE_GROUP = "BTN_VIEW_ONE_GROUP:"
        const val BTN_VIEW_ALL_GROUPS = "BTN_VIEW_ALL_GROUPS:"
        const val BTN_SET_MAIN_GROUP = "BTN_SET_MAIN_GROUP:"
        const val BTN_JOIN_GROUP = "BTN_JOIN_GROUP:"

        // chat states
        const val STT_WAITING_NEW_GROUP_NAME = "WAITING_NEW_GROUP_NAME:"

    }

    @OnMessage(command = "/group")
    @OnCallback(prefix = BTN_VIEW_ALL_GROUPS)
    fun onGroups(update: Update, ctx: UserContext) {
        val user = userService.findByTelegramId(getFrom(update).id)
            ?: throw UserNotFoundException(getFrom(update).id)

        val groups = groupService.findAllByUserId(user.id!!)
            .sortedBy { it.name }
        if (groups.isEmpty()) {
            return enterNewGroupName(update, ctx, "Кажется у вас нет групп. \n\n")
        }

        bot.reply(update)
            .text(
                """
                |Ваши группы:
                |
                |${
                    TableBuilder('-', " | ")
                        .column("Название") { idx -> groups[idx].name }
                        .column("Участников") { idx -> groups[idx].members.size.toString() }
                }
                """.trimMargin()
            )
            .inlineKeyboard(
                row("➕ Создать новую группу", BTN_ENTER_NEW_GROUP_NAME),
                *groups.map {
                    btn(it.name, BTN_VIEW_ONE_GROUP + it.id)
                }.chunked(3).toTypedArray()
            )
            .editIfCallbackMessageOrSend()
    }

    @OnCallback(prefix = BTN_ENTER_NEW_GROUP_NAME)
    fun enterNewGroupName(update: Update, ctx: UserContext, prefix: String? = null) {
        ctx.state = STT_WAITING_NEW_GROUP_NAME
        bot.reply(update)
            .text((prefix ?: "") + "Введите название группы:")
            .deleteAfterUpdateMessage(ctx)
            .editIfCallbackMessageOrSend()
    }

    @OnMessage(state = STT_WAITING_NEW_GROUP_NAME)
    fun onNewGroupName(update: Update, ctx: UserContext) {
        val groupName = update.message.text
        val user = userService.findByTelegramId(getFrom(update).id)
            ?: throw UserNotFoundException(getFrom(update).id)

        val group: Group = groupService.create(
            groupName = groupName,
            members = listOf(user.id!!),
        )
        ctx.state = ""

        viewOneGroup(update, ctx, group)
    }

    @OnCallback(prefix = BTN_VIEW_ONE_GROUP)
    fun viewGroup(update: Update, ctx: UserContext) {
        val groupId = update.callbackQuery.data.removePrefix(BTN_VIEW_ONE_GROUP).toLong()
        val group = groupService.getById(groupId)

        viewOneGroup(update, ctx, group)
    }

    private fun viewOneGroup(update: Update, ctx: UserContext, group: Group) {
        bot.reply(update)
            .text(
                """
                    | Группа ${group.name}
                    ${if (ctx.defaultGroup == group.id) "| Установлена как основная" else ""}
                    | Участники:
                    | ${group.members.joinToString("\n") { userService.getById(it).getViewName() }}
                    """.trimMargin()
            )
            .inlineKeyboard(
                row("Сделать основной", BTN_SET_MAIN_GROUP + group.id)
                    .filter { ctx.defaultGroup != group.id },
                row(btnSwitch("Пригласить участника", group.name)),
                row("Назад", BTN_VIEW_ALL_GROUPS)
            )
            .editIfCallbackMessageOrSend()
    }

    @OnCallback(prefix = BTN_SET_MAIN_GROUP)
    fun setMainGroup(update: Update, ctx: UserContext) {
        val groupId = update.callbackQuery.data.removePrefix(BTN_SET_MAIN_GROUP).toLong()
        val group = groupService.getById(groupId)

        ctx.defaultGroup = group.id

        bot.reply(update)
            .text("Группа ${group.name} теперь ваша основная")
            .sendPopup()

        viewOneGroup(update, ctx, group)
    }

    @OnInlineQuery
    fun onInlineQuery(update: Update, ctx: UserContext) {
        val query = update.inlineQuery.query.trim()
        val user = userService.findByTelegramId(getFrom(update).id)
            ?: throw UserNotFoundException(getFrom(update).id)

        val groups = groupService.findAllByUserId(user.id!!)
        val results = groups
            .filter { it.name.contains(query, ignoreCase = true) }
            .map {
                InlineQueryResultArticle()
                    .apply {
                        id = it.id.toString()
                        title = it.name
                        inputMessageContent = InputTextMessageContent().apply { messageText = it.name }
                        replyMarkup = InlineKeyboardMarkup(
                            listOf(
                                row("Участвовать", BTN_JOIN_GROUP + it.id)
                            )
                        )

                    }
            }

        bot.sendAnswerInlineQuery(update.inlineQuery.id, results)
    }

    @OnCallback(prefix = BTN_JOIN_GROUP)
    fun joinGroup(update: Update, ctx: UserContext) {
        val groupId = update.callbackQuery.data.removePrefix(BTN_JOIN_GROUP).toLong()
        val group = groupService.getById(groupId)

        val user = userService.findByTelegramId(getFrom(update).id)
            ?: throw UserNotFoundException(getFrom(update).id)

        groupService.addMember(groupId, user.id!!)

        bot.reply(update)
            .text("Вы присоединились к группе ${group.name}")
            .sendPopup()

        viewOneGroup(update, ctx, group)
    }
}