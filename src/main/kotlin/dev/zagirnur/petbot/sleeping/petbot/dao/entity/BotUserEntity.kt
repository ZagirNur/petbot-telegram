package dev.zagirnur.petbot.sleeping.petbot.dao.entity

import dev.zagirnur.petbot.sleeping.petbot.context.UserContext
import dev.zagirnur.petbot.sleeping.petbot.model.Lang
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import org.hibernate.annotations.Type


@Entity
@Table(name = "bot_user")
data class BotUserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var telegramId: Long,
    var telegramUserName: String?,
    var firstName: String?,
    var lastName: String?,
    @Enumerated(EnumType.STRING)
    var languageCode: Lang,
    @Type(JsonBinaryType::class)
    var context: UserContext = UserContext(),
) {
    fun getViewName(): String {
        if (firstName.isNullOrBlank() && lastName.isNullOrBlank()) {
            return telegramUserName ?: "id$telegramId"
        }
        return "${firstName ?: ""} ${lastName ?: ""}".trim()
    }
}