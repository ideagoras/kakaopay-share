package com.telltale.kakaopay.sprinkle.model

data class SprinkleRequest(
    val amount: Int,
    val receiverCount: Int
)