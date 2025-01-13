package dev.zagirnur.petbot.sleeping.petbot.service

import org.springframework.stereotype.Service

@Service
class GroupService {

    val storage: MutableList<Group> = mutableListOf()

    fun findAllByUserId(userId: Long): List<Group> {
        return storage.filter { it.members.contains(userId) }
    }

    fun createGroup(userId: Long, groupName: String):Group {
        val group = Group(storage.size.toLong(), groupName, mutableListOf(userId))
        storage.add(group)
        return group
    }

    fun findById(groupId: Long): Group? {
        return storage.find { it.id == groupId }
    }

    fun addMember(groupId: Long, userId: Long) {
        val group = storage.find { it.id == groupId } ?: throw IllegalArgumentException("Group not found")
        group.members.add(userId)
    }
}

data class Group(
    var id: Long,
    val name: String,
    val members: MutableList<Long>,
)