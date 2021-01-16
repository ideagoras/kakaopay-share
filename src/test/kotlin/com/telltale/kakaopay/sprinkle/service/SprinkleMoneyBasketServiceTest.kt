package com.telltale.kakaopay.sprinkle.service

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.telltale.kakaopay.sprinkle.repository.SprinkleMoneyBasketRepository
import com.telltale.kakaopay.sprinkle.test.randomShortString
import com.telltale.kakaopay.sprinkle.test.randomSprinkleMoneyBasketEntity
import com.telltale.kakaopay.sprinkle.test.randomTokenId
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class SprinkleMoneyBasketServiceTest {

    @MockK
    private lateinit var sprinkleMoneyBasketRepository: SprinkleMoneyBasketRepository

    @InjectMockKs
    private lateinit var sut: SprinkleMoneyBasketService

    @Test
    fun `takeSprinkleMoney succeeds`() {
        // given
        val basketEntity = randomSprinkleMoneyBasketEntity()
        every { sprinkleMoneyBasketRepository.findByTokenIdAndReceivedUserId(any(), any()) } returnsMany listOf(null, basketEntity)

        justRun { sprinkleMoneyBasketRepository.takeOneSprinkleMoney(any(), any()) }

        // when
        val tokenId = randomTokenId()
        val userId = randomShortString()

        val actual = runBlocking { sut.takeSprinkleMoney(tokenId, userId) }

        // then
        val expected = basketEntity.money
        assertThat(actual).isEqualTo(expected)

        verify(exactly = 2) { sprinkleMoneyBasketRepository.findByTokenIdAndReceivedUserId(tokenId, userId) }

        verify { sprinkleMoneyBasketRepository.takeOneSprinkleMoney(tokenId, userId) }

        confirmVerified(sprinkleMoneyBasketRepository, sprinkleMoneyBasketRepository, sprinkleMoneyBasketRepository)
    }
}