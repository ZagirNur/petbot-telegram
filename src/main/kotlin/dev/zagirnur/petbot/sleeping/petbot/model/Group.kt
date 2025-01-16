package dev.zagirnur.petbot.sleeping.petbot.model

data class Group(
    var id: Long,
    val name: String,
    val members: MutableSet<Long>,
)