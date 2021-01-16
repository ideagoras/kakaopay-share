package com.telltale.kakaopay.sprinkle.test

import com.telltale.kakaopay.sprinkle.repository.model.SprinkleMoneyBasketEntity
import com.telltale.kakaopay.sprinkle.repository.model.SprinkleMoneyHistoryEntity
import java.sql.Timestamp
import java.time.Instant
import kotlin.random.Random.Default.nextInt

fun randomSprinkleMoneyHistoryEntity(
    id: Long = 0L,
    userId: String = randomShortString(),
    roomId: String = randomShortString(),
    receiverCount: Int = nextInt(),
    amount: Int = nextInt(),
    tokenId: String = randomTokenId(),
    createdAt: Timestamp = Timestamp.from(Instant.now())
) = SprinkleMoneyHistoryEntity(id, userId, roomId, receiverCount, amount, tokenId, createdAt)

fun randomSprinkleMoneyBasketEntity(
    id: Long = 0L,
    tokenId: String = randomTokenId(),
    money: Int = nextInt(),
    receivedUserId: String? = null,
    createdAt: Timestamp = Timestamp.from(Instant.now()),
    updatedAt: Timestamp = createdAt
) = SprinkleMoneyBasketEntity(id, tokenId, money, receivedUserId, createdAt, updatedAt)
