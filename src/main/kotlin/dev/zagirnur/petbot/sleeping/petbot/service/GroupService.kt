package dev.zagirnur.petbot.sleeping.petbot.service

import dev.zagirnur.petbot.sleeping.petbot.dao.entity.GroupEntity
import dev.zagirnur.petbot.sleeping.petbot.dao.repository.GroupRepository
import dev.zagirnur.petbot.sleeping.petbot.dao.repository.UserRepository
import dev.zagirnur.petbot.sleeping.petbot.exceptions.GroupNotFoundException
import dev.zagirnur.petbot.sleeping.petbot.exceptions.UserAlreadyInGroupException
import dev.zagirnur.petbot.sleeping.petbot.exceptions.UserNotFoundException
import dev.zagirnur.petbot.sleeping.petbot.model.Group
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
) {
    fun findAllByUserId(id: Long): List<Group> {
        return groupRepository.findAllByMembersContaining(id)
            .map { Group(it.id!!, it.name, it.members) }
    }

    fun getById(id: Long): Group {
        val entity = groupRepository.findById(id)
            .orElseThrow { GroupNotFoundException(id) }
        return Group(entity.id!!, entity.name, entity.members)
    }

    fun create(
        groupName: String,
        members: List<Long>,
    ): Group {
        val findAllById = userRepository.findAllById(members).map { it.id }.toSet()
        if (findAllById.size != members.size) {
            members.forEach {
                if (!findAllById.contains(it)) {
                    throw UserNotFoundException(it)
                }
            }
        }

        val savedEntity = groupRepository.save(
            GroupEntity(
                name = groupName,
                members = members.toMutableSet(),
            )
        )
        return Group(savedEntity.id!!, savedEntity.name, savedEntity.members)
    }

    @Retryable(backoff = Backoff(delay = 1000))
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    fun addMember(groupId: Long, userId: Long) {
        val group = groupRepository.findById(groupId)
            .orElseThrow { GroupNotFoundException(groupId) }
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException(userId) }
        val add = group.members.add(user.id!!)
        if (!add) {
            throw UserAlreadyInGroupException(userId, groupId)
        }
        groupRepository.save(group)
    }

}
