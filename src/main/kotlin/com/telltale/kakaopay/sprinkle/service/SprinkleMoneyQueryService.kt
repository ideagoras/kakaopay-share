package com.telltale.kakaopay.sprinkle.service

import com.telltale.kakaopay.sprinkle.exception.SprinkleException
import com.telltale.kakaopay.sprinkle.model.DetailedSprinkleMoneyResponse
import com.telltale.kakaopay.sprinkle.model.ErrorResponse
import com.telltale.kakaopay.sprinkle.model.ReceivedUserInfo
import com.telltale.kakaopay.sprinkle.repository.SprinkleMoneyBasketRepository
import com.telltale.kakaopay.sprinkle.repository.SprinkleMoneyHistoryRepository
import com.telltale.kakaopay.sprinkle.repository.model.SprinkleMoneyHistoryEntity
import com.telltale.kakaopay.sprinkle.repository.model.TokenId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class SprinkleMoneyQueryService(
    private val sprinkleMoneyHistoryRepository: SprinkleMoneyHistoryRepository,
    private val sprinkleMoneyBasketRepository: SprinkleMoneyBasketRepository
) {
    suspend fun getSprinkleMoneyInfo(
        tokenId: TokenId,
        userId: String,
        roomId: String
    ): DetailedSprinkleMoneyResponse {
        val historyEntity = getSprinkleMoneyHistoryOrException(tokenId)
        val basketEntities = withContext(Dispatchers.IO) {
            sprinkleMoneyBasketRepository.findByTokenId(tokenId)
        }
        val receivedUserInfos = basketEntities.filter { it.receivedUserId != null }
            .map { ReceivedUserInfo(userId = it.receivedUserId!!, money = it.money) }

        return DetailedSprinkleMoneyResponse(
            createdAt = historyEntity.createdAt.time,
            amount = historyEntity.amount,
            totalReceivedMoney = receivedUserInfos.map { it.money }.sum(),
            receivedUsers = receivedUserInfos
        )
    }

    @Throws(SprinkleException::class)
    suspend fun getSprinkleMoneyHistoryOrException(tokenId: TokenId): SprinkleMoneyHistoryEntity =
        withContext(Dispatchers.IO) {
            sprinkleMoneyHistoryRepository.findByTokenId(tokenId)
        } ?: throw SprinkleException(ErrorResponse.NOT_FOUND_TOKEN_ID)
}