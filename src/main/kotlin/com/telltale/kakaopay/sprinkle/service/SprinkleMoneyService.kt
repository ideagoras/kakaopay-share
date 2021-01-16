package com.telltale.kakaopay.sprinkle.service

import com.telltale.kakaopay.sprinkle.model.SprinkleRequest
import com.telltale.kakaopay.sprinkle.repository.SprinkleMoneyBasketRepository
import com.telltale.kakaopay.sprinkle.repository.SprinkleMoneyHistoryRepository
import com.telltale.kakaopay.sprinkle.repository.model.SprinkleMoneyBasketEntity
import com.telltale.kakaopay.sprinkle.repository.model.SprinkleMoneyHistoryEntity
import com.telltale.kakaopay.sprinkle.repository.model.TokenId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.shaded.com.google.common.annotations.VisibleForTesting
import java.sql.Timestamp
import java.time.Clock

@Service
class SprinkleMoneyService(
    private val sprinkleMoneyHistoryRepository: SprinkleMoneyHistoryRepository,
    private val sprinkleMoneyBasketRepository: SprinkleMoneyBasketRepository,
    private val transactionTemplate: TransactionTemplate
) {
    @VisibleForTesting
    var clock = Clock.systemUTC()

    suspend fun registerSprinkleMoney(
        tokenId: TokenId, basketMoneys: List<Int>,
        userId: String, roomId: String, request: SprinkleRequest
    ) {
        val historyEntity = SprinkleMoneyHistoryEntity(
            userId = userId,
            roomId = roomId,
            tokenId = tokenId,
            receiverCount = request.receiverCount,
            amount = request.amount,
            createdAt = Timestamp.from(clock.instant())
        )

        val basketEntities = basketMoneys.map {
            SprinkleMoneyBasketEntity(
                tokenId = tokenId,
                money = it,
                createdAt = Timestamp.from(clock.instant())
            )
        }

        withContext(Dispatchers.IO) {
            transactionTemplate.execute {
                sprinkleMoneyHistoryRepository.save(historyEntity)
                sprinkleMoneyBasketRepository.saveAll(basketEntities)
            }
        }
    }
}