package com.telltale.kakaopay.sprinkle.service

import com.telltale.kakaopay.sprinkle.exception.SprinkleException
import com.telltale.kakaopay.sprinkle.model.ErrorResponse
import com.telltale.kakaopay.sprinkle.repository.SprinkleMoneyBasketRepository
import com.telltale.kakaopay.sprinkle.repository.model.TokenId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.testcontainers.shaded.com.google.common.annotations.VisibleForTesting
import java.time.Clock

@Service
class SprinkleMoneyBasketService(
    private val sprinkleMoneyBasketRepository: SprinkleMoneyBasketRepository
) {
    @VisibleForTesting
    var clock = Clock.systemUTC()

    @Throws(SprinkleException::class)
    suspend fun takeSprinkleMoney(tokenId: TokenId, userId: String): Int? {
        val basketEntity = withContext(Dispatchers.IO) {
            checkDuplicatedAllocated(tokenId, userId)
            sprinkleMoneyBasketRepository.takeOneSprinkleMoney(tokenId = tokenId, receivedUserId = userId)
            sprinkleMoneyBasketRepository.findByTokenIdAndReceivedUserId(tokenId = tokenId, receivedUserId = userId)
        }
        return basketEntity?.money
    }

    @Throws(SprinkleException::class)
    private fun checkDuplicatedAllocated(tokenId: TokenId, userId: String) {
        val basketEntity = sprinkleMoneyBasketRepository.findByTokenIdAndReceivedUserId(
            tokenId = tokenId,
            receivedUserId = userId
        )
        if (basketEntity != null) {
            throw SprinkleException(ErrorResponse.ALREADY_RECEIVED_MONEY)
        }
    }
}