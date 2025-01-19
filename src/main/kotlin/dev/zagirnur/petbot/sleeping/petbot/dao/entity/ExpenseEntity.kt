package dev.zagirnur.petbot.sleeping.petbot.dao.entity

import dev.zagirnur.petbot.sleeping.petbot.model.SplitType
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
    @Enumerated(EnumType.STRING)
    var splitType: SplitType = SplitType.EQUALLY,
    @Type(JsonBinaryType::class)
    val paidBy: MutableMap<Long, BigDecimal> = mutableMapOf(),
    @Type(JsonBinaryType::class)
    val splitBy: MutableMap<Long, BigDecimal> = mutableMapOf(),
)