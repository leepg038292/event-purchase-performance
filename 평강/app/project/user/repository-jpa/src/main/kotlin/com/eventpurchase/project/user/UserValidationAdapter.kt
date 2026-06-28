package com.eventpurchase.project.user

import org.springframework.stereotype.Component

@Component
internal class UserValidationAdapter(
    private val userRepository: UserRepository
) : UserValidationPort {
    override fun existsById(id: Long) = userRepository.existsById(id)
}
