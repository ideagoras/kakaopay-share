package com.telltale.kakaopay.sprinkle.service

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.telltale.kakaopay.sprinkle.exception.SprinkleException
import com.telltale.kakaopay.sprinkle.model.DetailedSprinkleMoneyResponse
import com.telltale.kakaopay.sprinkle.model.ErrorResponse
import com.telltale.kakaopay.sprinkle.repository.SprinkleMoneyBasketRepository
import com.telltale.kakaopay.sprinkle.repository.SprinkleMoneyHistoryRepository
import com.telltale.kakaopay.sprinkle.test.randomShortString
import com.telltale.kakaopay.sprinkle.test.randomSmallList
import com.telltale.kakaopay.sprinkle.test.randomSprinkleMoneyBasketEntity
import com.telltale.kakaopay.sprinkle.test.randomSprinkleMoneyHistoryEntity
import com.telltale.kakaopay.sprinkle.test.randomTokenId
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class SprinkleMoneyQueryServiceTest {

    @MockK
    private lateinit var sprinkleMoneyHistoryRepository: SprinkleMoneyHistoryRepository

    @MockK
    private lateinit var sprinkleMoneyBasketRepository: SprinkleMoneyBasketRepository

    @InjectMockKs
    private lateinit var sut: SprinkleMoneyQueryService

    @Test
    fun `getSprinkleMoneyInfo succeeds`() {
        // given
        val tokenId = randomTokenId()
        val userId = randomShortString()
        val roomId = randomShortString()
        val historyEntity = randomSprinkleMoneyHistoryEntity(tokenId = tokenId)
        every { sprinkleMoneyHistoryRepository.findByTokenId(any()) } returns historyEntity

        val basketEntity = generateSequence { randomSprinkleMoneyBasketEntity(tokenId = tokenId) }.randomSmallList()
        every { sprinkleMoneyBasketRepository.findByTokenId(any()) } returns basketEntity

        // when
        val actual = runBlocking { sut.getSprinkleMoneyInfo(tokenId, userId, roomId) }

        // then
        val expected = DetailedSprinkleMoneyResponse(
            createdAt = historyEntity.createdAt.time,
            amount = historyEntity.amount,
            totalReceivedMoney = 0,
            receivedUsers = emptyList()
        )
        assertThat(actual).isEqualTo(expected)

        verify { sprinkleMoneyHistoryRepository.findByTokenId(tokenId) }

        verify { sprinkleMoneyBasketRepository.findByTokenId(tokenId) }

        confirmVerified(sprinkleMoneyHistoryRepository, sprinkleMoneyBasketRepository)
    }

    @Test
    fun `getSprinkleMoneyHistoryOrException succeeds`() {
        // given
        val tokenId = randomTokenId()
        val historyEntity = randomSprinkleMoneyHistoryEntity(tokenId = tokenId)
        every { sprinkleMoneyHistoryRepository.findByTokenId(any()) } returns historyEntity

        // when
        val actual = runBlocking { sut.getSprinkleMoneyHistoryOrException(tokenId) }

        // then
        assertThat(actual).isEqualTo(historyEntity)

        verify { sprinkleMoneyHistoryRepository.findByTokenId(tokenId) }

        confirmVerified(sprinkleMoneyHistoryRepository, sprinkleMoneyBasketRepository)
    }

    @Test
    fun `getSprinkleMoneyHistoryOrException fails due to not found tokenId`() {
        // given
        every { sprinkleMoneyHistoryRepository.findByTokenId(any()) } returns null

        // when
        val tokenId = randomTokenId()
        val actual = assertThrows<SprinkleException> {
            runBlocking { sut.getSprinkleMoneyHistoryOrException(tokenId) }
        }

        // then
        assertThat(actual.errorResponse).isEqualTo(ErrorResponse.NOT_FOUND_TOKEN_ID)

        verify { sprinkleMoneyHistoryRepository.findByTokenId(tokenId) }

        confirmVerified(sprinkleMoneyHistoryRepository, sprinkleMoneyBasketRepository)
    }
}