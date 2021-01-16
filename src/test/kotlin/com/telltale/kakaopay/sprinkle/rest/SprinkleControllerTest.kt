package com.telltale.kakaopay.sprinkle.rest

import com.telltale.kakaopay.sprinkle.exception.SprinkleException
import com.telltale.kakaopay.sprinkle.model.ErrorResponse
import com.telltale.kakaopay.sprinkle.model.SprinkleMoneyResponse
import com.telltale.kakaopay.sprinkle.model.SprinkleResponse
import com.telltale.kakaopay.sprinkle.randomDetailedSprinkleMoneyResponse
import com.telltale.kakaopay.sprinkle.randomSprinkleRequest
import com.telltale.kakaopay.sprinkle.service.SprinkleMoneyBasketService
import com.telltale.kakaopay.sprinkle.service.SprinkleMoneyQueryService
import com.telltale.kakaopay.sprinkle.service.SprinkleMoneyService
import com.telltale.kakaopay.sprinkle.test.randomMoney
import com.telltale.kakaopay.sprinkle.test.randomShortString
import com.telltale.kakaopay.sprinkle.test.randomSprinkleMoneyHistoryEntity
import com.telltale.kakaopay.sprinkle.test.randomTokenId
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import reactor.kotlin.test.test
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@ExtendWith(MockKExtension::class)
internal class SprinkleControllerTest {

    @MockK
    private lateinit var sprinkleMoneyService: SprinkleMoneyService

    @MockK
    private lateinit var sprinkleMoneyBasketService: SprinkleMoneyBasketService

    @MockK
    private lateinit var sprinkleMoneyQueryService: SprinkleMoneyQueryService

    @InjectMockKs
    private lateinit var sut: SprinkleController

    private lateinit var clock: Clock

    @BeforeEach
    fun setUp() {
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
        sut.clock = clock
    }

    @Test
    fun `registerSprinkleMoney succeeds`() {
        // given
        coJustRun { sprinkleMoneyService.registerSprinkleMoney(any(), any(), any(), any(), any()) }

        // when
        val userId = randomShortString()
        val roomId = randomShortString()
        val request = randomSprinkleRequest()
        val mono = sut.registerSprinkleMoney(userId, roomId, request).test()

        // then
        mono.expectNextCount(1).verifyComplete()

        coVerify { sprinkleMoneyService.registerSprinkleMoney(any(), any(), userId, roomId, request) }

        confirmVerified(sprinkleMoneyBasketService, sprinkleMoneyQueryService, sprinkleMoneyService)
    }

    @Test
    fun `takeSprinkleMoney succeeds`() {
        // given
        val roomId = randomShortString()
        val tokenId = randomTokenId()
        val historyEntity = randomSprinkleMoneyHistoryEntity(roomId = roomId, tokenId = tokenId)
        coEvery { sprinkleMoneyQueryService.getSprinkleMoneyHistoryOrException(any()) } returns historyEntity

        val money = randomMoney()
        coEvery { sprinkleMoneyBasketService.takeSprinkleMoney(any(), any()) } returns money

        // when
        val userId = randomShortString()
        val mono = sut.takeSprinkleMoney(userId, roomId, tokenId).test()

        // then
        val expected = SprinkleMoneyResponse(money)
        mono.expectNext(expected).verifyComplete()

        coVerify { sprinkleMoneyQueryService.getSprinkleMoneyHistoryOrException(tokenId) }

        coVerify { sprinkleMoneyBasketService.takeSprinkleMoney(tokenId, userId) }

        confirmVerified(sprinkleMoneyBasketService, sprinkleMoneyQueryService, sprinkleMoneyService)
    }

    @Test
    fun `takeSprinkleMoney fails due to cannot take money`() {
        // given
        val roomId = randomShortString()
        val tokenId = randomTokenId()
        val historyEntity = randomSprinkleMoneyHistoryEntity(roomId = roomId, tokenId = tokenId)
        coEvery { sprinkleMoneyQueryService.getSprinkleMoneyHistoryOrException(any()) } returns historyEntity

        val money = null
        coEvery { sprinkleMoneyBasketService.takeSprinkleMoney(any(), any()) } returns money

        // when
        val userId = randomShortString()
        val mono = sut.takeSprinkleMoney(userId, roomId, tokenId).test()

        // then
        mono.expectError(SprinkleException::class.java).verify()

        coVerify { sprinkleMoneyQueryService.getSprinkleMoneyHistoryOrException(tokenId) }

        coVerify { sprinkleMoneyBasketService.takeSprinkleMoney(tokenId, userId) }

        confirmVerified(sprinkleMoneyBasketService, sprinkleMoneyQueryService, sprinkleMoneyService)
    }

    @Test
    fun `takeSprinkleMoney fails due to userId is owner`() {
        // given
        val userId = randomShortString()
        val roomId = randomShortString()
        val tokenId = randomTokenId()
        val historyEntity = randomSprinkleMoneyHistoryEntity(userId = userId, roomId = roomId, tokenId = tokenId)
        coEvery { sprinkleMoneyQueryService.getSprinkleMoneyHistoryOrException(any()) } returns historyEntity

        // when
        val mono = sut.takeSprinkleMoney(userId, roomId, tokenId).test()

        // then
        mono.expectError(SprinkleException::class.java).verify()

        coVerify { sprinkleMoneyQueryService.getSprinkleMoneyHistoryOrException(tokenId) }

        confirmVerified(sprinkleMoneyBasketService, sprinkleMoneyQueryService, sprinkleMoneyService)
    }

    @Test
    fun `takeSprinkleMoney fails due to tokenId is expired`() {
        // given
        val roomId = randomShortString()
        val tokenId = randomTokenId()
        val historyEntity = randomSprinkleMoneyHistoryEntity(
            roomId = roomId,
            tokenId = tokenId,
            createdAt = Timestamp.from(clock.instant().minusMillis(10 * 60 * 1000 + 1))
        )
        coEvery { sprinkleMoneyQueryService.getSprinkleMoneyHistoryOrException(any()) } returns historyEntity

        // when
        val userId = randomShortString()
        val mono = sut.takeSprinkleMoney(userId, roomId, tokenId).test()

        // then
        mono.expectError(SprinkleException::class.java).verify()

        coVerify { sprinkleMoneyQueryService.getSprinkleMoneyHistoryOrException(tokenId) }

        confirmVerified(sprinkleMoneyBasketService, sprinkleMoneyQueryService, sprinkleMoneyService)
    }

    @Test
    fun `getSprinkleMoney succeeds`() {
        // given
        val userId = randomShortString()
        val roomId = randomShortString()
        val tokenId = randomTokenId()
        val historyEntity = randomSprinkleMoneyHistoryEntity(
            userId = userId, roomId = roomId, tokenId = tokenId, createdAt = Timestamp.from(clock.instant())
        )
        coEvery { sprinkleMoneyQueryService.getSprinkleMoneyHistoryOrException(any()) } returns historyEntity

        val response = randomDetailedSprinkleMoneyResponse()
        coEvery { sprinkleMoneyQueryService.getSprinkleMoneyInfo(any(), any(), any()) } returns response

        // when
        val mono = sut.getSprinkleMoney(userId, roomId, tokenId).test()

        // then
        mono.expectNext(response).verifyComplete()

        coVerify { sprinkleMoneyQueryService.getSprinkleMoneyHistoryOrException(tokenId) }

        coVerify { sprinkleMoneyQueryService.getSprinkleMoneyInfo(tokenId, userId, roomId) }

        confirmVerified(sprinkleMoneyBasketService, sprinkleMoneyQueryService, sprinkleMoneyService)
    }

    @Test
    fun `getSprinkleMoney fails due to user is NOT owner`() {
        // given
        val roomId = randomShortString()
        val tokenId = randomTokenId()
        val historyEntity = randomSprinkleMoneyHistoryEntity(roomId = roomId, tokenId = tokenId)
        coEvery { sprinkleMoneyQueryService.getSprinkleMoneyHistoryOrException(any()) } returns historyEntity

        // when
        val userId = randomShortString()
        val mono = sut.getSprinkleMoney(userId, roomId, tokenId).test()

        // then
        mono.expectError(SprinkleException::class.java).verify()

        coVerify { sprinkleMoneyQueryService.getSprinkleMoneyHistoryOrException(tokenId) }

        confirmVerified(sprinkleMoneyBasketService, sprinkleMoneyQueryService, sprinkleMoneyService)
    }

    @Test
    fun `getSprinkleMoney fails due to tokenId is expired`() {
        // given
        val userId = randomShortString()
        val roomId = randomShortString()
        val tokenId = randomTokenId()
        val historyEntity = randomSprinkleMoneyHistoryEntity(
            userId = userId,
            roomId = roomId,
            tokenId = tokenId,
            createdAt = Timestamp.from(clock.instant().minusMillis(7 * 24 * 60 * 60 * 1000 + 1))
        )
        coEvery { sprinkleMoneyQueryService.getSprinkleMoneyHistoryOrException(any()) } returns historyEntity

        // when
        val mono = sut.getSprinkleMoney(userId, roomId, tokenId).test()

        // then
        mono.expectError(SprinkleException::class.java).verify()

        coVerify { sprinkleMoneyQueryService.getSprinkleMoneyHistoryOrException(tokenId) }

        confirmVerified(sprinkleMoneyBasketService, sprinkleMoneyQueryService, sprinkleMoneyService)
    }
}