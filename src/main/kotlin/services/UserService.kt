package com.example.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.model.Users
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update


class UserService {

    fun createUser(name: String, email: String, password: String): UserResponse {
        return transaction {
            val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())

            val userId = Users.insert {
                it[Users.name] = name
                it[Users.email] = email
                it[Users.password] = hashedPassword
            } get Users.id

            UserResponse(
                id = userId,
                name = name,
                email = email,
                creationDate = "now"
            )
        }
    }

    fun getUserByEmail(email: String): UserResponse? {
        return transaction {
            Users.select { Users.email eq email }.singleOrNull()?.let { rowToUserResponse(it) }
        }
    }

    fun getUserById(id: Int): UserResponse? {
        return transaction {
            Users.select { Users.id eq id }.singleOrNull()?.let { rowToUserResponse(it) }
        }
    }

    fun getAllUsers(page: Int = 1, limit: Int = 10): List<UserResponse> {
        return transaction {
            val offset = (page - 1).toLong() * limit
            Users.selectAll()
                .limit(limit, offset)
                .map { rowToUserResponse(it) }
        }
    }

    fun getUsersCount(): Long {
        return transaction {
            Users.selectAll().count()
        }
    }

    fun updateUser(id: Int, name: String?, email: String?): Boolean {
        return transaction {
            Users.update({ Users.id eq id }) {
                if (name != null) it[Users.name] = name
                if (email != null) it[Users.email] = email
            } > 0
        }
    }

    fun deleteUser(id: Int): Boolean {
        return transaction {
            Users.deleteWhere { Users.id eq id } > 0
        }
    }

    fun userExists(id: Int): Boolean {
        return transaction {
            Users.select { Users.id eq id }.singleOrNull() != null
        }
    }

    fun emailExists(email: String): Boolean {
        return transaction {
            Users.select { Users.email eq email }.singleOrNull() != null
        }
    }

    private fun rowToUserResponse(row: ResultRow): UserResponse {
        return UserResponse(
            id = row[Users.id],
            name = row[Users.name],
            email = row[Users.email],
            creationDate = row[Users.creationDate].toString()
        )
    }

    fun emailExists(email: String, excludeUserId: Int? = null): Boolean {
        return transaction {
            val query = if (excludeUserId != null) {
                Users.select { (Users.email eq email) and (Users.id neq excludeUserId) }
            } else {
                Users.select { Users.email eq email }
            }
            query.singleOrNull() != null
        }
    }
}

@Serializable
data class CreateUserRequest(val name: String, val email: String, val password: String)

@Serializable
data class UpdateUserRequest(val name: String?, val email: String?)

@Serializable
data class UserResponse(val id: Int, val name: String, val email: String, val creationDate: String)

@Serializable
data class PaginatedUsersResponse(
    val users: List<UserResponse>,
    val page: Int,
    val limit: Int,
    val total: Int
)