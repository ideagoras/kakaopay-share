package com.telltale.kakaopay.sprinkle.rest.service

import com.telltale.kakaopay.sprinkle.repository.model.TokenId
import org.apache.commons.lang3.RandomStringUtils

fun generateTokenId(): TokenId = RandomStringUtils.randomAlphabetic(3)

@Throws(IllegalArgumentException::class)
fun makeBasketMoneyList(amount: Int, receiverCount: Int): List<Int> {
    if (receiverCount <= 0)
        throw IllegalArgumentException("receiverCount should be greater than zero")
    if (receiverCount > amount)
        throw IllegalArgumentException("receiverCount($receiverCount) should be less than amount($amount)")
    val money = amount / receiverCount
    val firstMemberIncentive = amount % receiverCount

    return (0 until receiverCount).map { index ->
        when (index) {
            0 -> money + firstMemberIncentive
            else -> money
        }
    }
}
