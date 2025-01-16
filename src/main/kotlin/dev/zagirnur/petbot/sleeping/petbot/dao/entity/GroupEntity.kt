package dev.zagirnur.petbot.sleeping.petbot.dao.entity

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import org.hibernate.annotations.Type


@Entity
@Table(name = "bot_group")
data class GroupEntity (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var name: String,

    @Type(JsonBinaryType::class)
    var members: MutableSet<Long>,
)