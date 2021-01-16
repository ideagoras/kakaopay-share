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
@Table(name = SprinkleMoneyHistoryEntity.TABLE_NAME)
data class SprinkleMoneyHistoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    val id: Long = 0L,

    @Column
    val userId: String,

    @Column
    val roomId: String,

    @Column
    val receiverCount: Int,

    @Column
    val amount: Int,

    @Column
    val tokenId: String,

    @Column
    val createdAt: Timestamp = Timestamp.from(Instant.now())
) : Serializable {
    companion object {
        const val TABLE_NAME = "sprinkle_money_history"
    }
}
