package com.telltale.kakaopay.sprinkle.repository.model

import java.io.Serializable
import java.sql.Timestamp
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = SprinkleMoneyBasketEntity.TABLE_NAME)
data class SprinkleMoneyBasketEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    val id: Long = 0L,

    @Column
    val tokenId: String,

    @Column
    val money: Int,

    @Column
    var receivedUserId: String? = null,

    @Column
    val createdAt: Timestamp = Timestamp.from(Instant.now()),

    @Column
    var updatedAt: Timestamp = createdAt
): Serializable {
    companion object {
        const val TABLE_NAME = "sprinkle_money_basket"
    }
}
