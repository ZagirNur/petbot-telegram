package dev.zagirnur.petbot.sleeping.petbot.dao.entity

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.math.BigDecimal


@Entity
@Table(name = "expense")
data class ExpenseEntity (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var groupId: Long,
    var amount: BigDecimal,
    var description: String,
    @Type(JsonBinaryType::class)
    val paidBy: MutableMap<Long, BigDecimal> = mutableMapOf(),
    @Type(JsonBinaryType::class)
    val splitBy: MutableMap<Long, BigDecimal> = mutableMapOf(),
)