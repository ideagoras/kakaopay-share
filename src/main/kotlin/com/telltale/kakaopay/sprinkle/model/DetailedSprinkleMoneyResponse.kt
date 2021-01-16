package com.telltale.kakaopay.sprinkle.model

data class DetailedSprinkleMoneyResponse(
    val createdAt: Long,
    val amount: Int,
    val totalReceivedMoney: Int,
    val receivedUsers: List<ReceivedUserInfo>
)

data class ReceivedUserInfo(val userId: String, val money: Int)
