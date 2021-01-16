package com.telltale.kakaopay.sprinkle.test

import org.apache.commons.lang3.RandomStringUtils
import kotlin.random.Random
import kotlin.random.Random.Default.nextInt

fun randomShortString() = RandomStringUtils.randomAlphabetic(10)

fun randomSmallNumber() = Random.nextInt(2, 5)

fun <T> Sequence<T>.randomSmallList() = this.take(randomSmallNumber()).toList()

fun randomTokenId() = RandomStringUtils.randomAlphabetic(3)

fun randomReceiverCount() = nextInt(1, 100)

fun randomMoney() = nextInt(1, 1_000_000)
