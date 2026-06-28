package com.eventpurchase.project.user

import jakarta.persistence.*

@Entity
@Table(name = "users")
class UserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    @Column(nullable = false, unique = true) val email: String,
    @Column(nullable = false) val password: String
) : BaseTimeEntity() {
    fun toDomain() = User(id, email, password)
    companion object { fun from(u: User) = UserEntity(u.id, u.email, u.password) }
}
