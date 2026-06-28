package com.eventpurchase.project.user

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
internal class UserPersistenceAdapter(
    private val userJpaRepository: UserJpaRepository
) : UserRepository {
    override fun findById(id: Long) = userJpaRepository.findByIdOrNull(id)?.toDomain()
    override fun existsById(id: Long) = userJpaRepository.existsById(id)
    override fun save(user: User) = userJpaRepository.save(UserEntity.from(user)).toDomain()
}
