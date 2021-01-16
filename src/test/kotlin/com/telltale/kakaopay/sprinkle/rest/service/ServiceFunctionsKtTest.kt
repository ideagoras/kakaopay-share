package com.telltale.kakaopay.sprinkle.rest.service

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream

internal class ServiceFunctionsKtTest {

    @Test
    fun `generateTokenId() succeeds`() {
        assertThat(generateTokenId().length).isEqualTo(3)
    }

    class MakeBasketMoneyListTestArguments : ArgumentsProvider {
        private fun argumentOf(
            amount: Int,
            receiverCount: Int,
            expected: List<Int>
        ) = Arguments.of(amount, receiverCount, expected)

        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> =
            Stream.of(
                argumentOf(
                    amount = 20,
                    receiverCount = 1,
                    expected = listOf(20)
                ),
                argumentOf(
                    amount = 20,
                    receiverCount = 2,
                    expected = listOf(10, 10)
                ),
                argumentOf(
                    amount = 21,
                    receiverCount = 2,
                    expected = listOf(11, 10)
                )
            )
    }

    @ParameterizedTest
    @ArgumentsSource(MakeBasketMoneyListTestArguments::class)
    fun `makeBasketMoneyList succeeds`(
        amount: Int,
        receiverCount: Int,
        expected: List<Int>
    ) {
        // given

        // when
        val actual = makeBasketMoneyList(amount, receiverCount)

        // then
        assertThat(actual).isEqualTo(expected)
    }

    class FailedMakeBasketMoneyListTestArguments : ArgumentsProvider {
        private fun argumentOf(
            amount: Int,
            receiverCount: Int,
            expected: Exception
        ) = Arguments.of(amount, receiverCount, expected)

        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> =
            Stream.of(
                argumentOf(
                    amount = 20,
                    receiverCount = 0,
                    expected = IllegalArgumentException("receiverCount should be greater than zero")
                ),
                argumentOf(
                    amount = 20,
                    receiverCount = 21,
                    expected = IllegalArgumentException("receiverCount(21) should be less than amount(20)")
                )
            )
    }

    @ParameterizedTest
    @ArgumentsSource(FailedMakeBasketMoneyListTestArguments::class)
    fun `makeBasketMoneyList fails`(
        amount: Int,
        receiverCount: Int,
        expected: Exception
    ) {
        // given

        // when
        val actual = assertThrows<IllegalArgumentException> { makeBasketMoneyList(amount, receiverCount) }

        // then
        assertThat(actual.message).isEqualTo(expected.message)
    }
}