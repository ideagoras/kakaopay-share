package com.telltale.kakaopay.sprinkle.model

enum class ErrorResponse(val httpStatusCode: Int, val code: Int, val message: String) {
    INTERNAL_SERVER_ERROR(500, 500, "Internal server error"),
    NOT_FOUND_TOKEN_ID(404, 404, "Cannot find tokenId"),
    ALREADY_RECEIVED_MONEY(500, 100, "You have already receive money"),
    OWNER_CANNOT_TAKE_MONEY(500, 101, "Owner cannot take money"),
    EXPIRED_TAKING_MONEY(500, 102, "Expired taking money"),
    UNAUTHORIZED_MONEY(401, 401, "You are not owner"),
    EXPIRED_SHOW_MONEY(408, 408, "Expired show token")
}