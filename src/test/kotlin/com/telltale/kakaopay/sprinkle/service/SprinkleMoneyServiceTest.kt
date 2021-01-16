package com.telltale.kakaopay.sprinkle.service

import com.telltale.kakaopay.sprinkle.randomSprinkleRequest
import com.telltale.kakaopay.sprinkle.repository.SprinkleMoneyBasketRepository
import com.telltale.kakaopay.sprinkle.repository.SprinkleMoneyHistoryRepository
import com.telltale.kakaopay.sprinkle.repository.model.SprinkleMoneyBasketEntity
import com.telltale.kakaopay.sprinkle.repository.model.SprinkleMoneyHistoryEntity
import com.telltale.kakaopay.sprinkle.test.randomMoney
import com.telltale.kakaopay.sprinkle.test.randomShortString
import com.telltale.kakaopay.sprinkle.test.randomSmallList
import com.telltale.kakaopay.sprinkle.test.randomSprinkleMoneyBasketEntity
import com.telltale.kakaopay.sprinkle.test.randomTokenId
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@ExtendWith(MockKExtension::class)
internal class SprinkleMoneyServiceTest {

    @MockK
    private lateinit var sprinkleMoneyHistoryRepository: SprinkleMoneyHistoryRepository

    @MockK
    private lateinit var sprinkleMoneyBasketRepository: SprinkleMoneyBasketRepository

    @MockK
    private lateinit var transactionTemplate: TransactionTemplate

    @InjectMockKs
    private lateinit var sut: SprinkleMoneyService

    private lateinit var clock: Clock

    @BeforeEach
    fun setUp() {
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
        sut.clock = clock
    }

    @Test
    fun `registerSprinkleMoney succeeds`() {
        // given

        every {
            transactionTemplate.execute(any<TransactionCallback<Any>>())
        } answers {
            val status = mockk<TransactionStatus>()
            (it.invocation.args[0] as TransactionCallback<Any>).doInTransaction(status)
        }

        every { sprinkleMoneyHistoryRepository.save(any<SprinkleMoneyHistoryEntity>()) } answers { firstArg() }

        val tokenId = randomTokenId()
        val basketMoneys = generateSequence { randomMoney() }.randomSmallList()
        val basketEntities = basketMoneys.map {
            randomSprinkleMoneyBasketEntity(
                tokenId = tokenId,
                money = it,
                createdAt = Timestamp.from(clock.instant())
            )
        }
        every { sprinkleMoneyBasketRepository.saveAll(any<Iterable<SprinkleMoneyBasketEntity>>()) } returns basketEntities

        // when
        val userId = randomShortString()
        val roomId = randomShortString()
        val request = randomSprinkleRequest()

        runBlocking { sut.registerSprinkleMoney(tokenId, basketMoneys, userId, roomId, request) }

        // then
        verify { transactionTemplate.execute(any<TransactionCallback<Any>>()) }

        val historyEntity = SprinkleMoneyHistoryEntity(
            userId = userId,
            roomId = roomId,
            tokenId = tokenId,
            receiverCount = request.receiverCount,
            amount = request.amount,
            createdAt = Timestamp.from(clock.instant())
        )
        verify { sprinkleMoneyHistoryRepository.save(historyEntity) }

        verify { sprinkleMoneyBasketRepository.saveAll(basketEntities) }

        confirmVerified(transactionTemplate, sprinkleMoneyHistoryRepository, sprinkleMoneyBasketRepository)
    }
}
