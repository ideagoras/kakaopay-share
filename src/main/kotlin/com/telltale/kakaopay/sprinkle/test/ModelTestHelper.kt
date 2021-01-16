package com.telltale.kakaopay.sprinkle

import com.telltale.kakaopay.sprinkle.model.DetailedSprinkleMoneyResponse
import com.telltale.kakaopay.sprinkle.model.ReceivedUserInfo
import com.telltale.kakaopay.sprinkle.model.SprinkleRequest
import com.telltale.kakaopay.sprinkle.test.randomMoney
import com.telltale.kakaopay.sprinkle.test.randomReceiverCount
import com.telltale.kakaopay.sprinkle.test.randomShortString
import com.telltale.kakaopay.sprinkle.test.randomSmallNumber
import org.apache.commons.lang3.RandomStringUtils
import kotlin.random.Random.Default.nextInt
import kotlin.random.Random.Default.nextLong

fun randomDetailedSprinkleMoneyResponse(
    createdAtMillis: Long = nextLong(),
    amount: Int = randomMoney(),
    totalReceivedMoney: Int = randomReceiverCount(),
    receivedUsers: List<ReceivedUserInfo> = generateSequence { randomReceivedUserInfo() }.take(randomSmallNumber()).toList()
) = DetailedSprinkleMoneyResponse(createdAtMillis, amount, totalReceivedMoney, receivedUsers)

fun randomReceivedUserInfo(
    userId: String = randomShortString(),
    money: Int = randomMoney()
) = ReceivedUserInfo(userId, money)

fun randomSprinkleRequest(
    amount: Int = randomMoney(),
    receiverCount: Int = randomReceiverCount()
) = SprinkleRequest(amount, receiverCount)
