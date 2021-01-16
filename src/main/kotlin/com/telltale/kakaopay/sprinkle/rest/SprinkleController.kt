package com.telltale.kakaopay.sprinkle.rest

import com.telltale.kakaopay.sprinkle.exception.SprinkleException
import com.telltale.kakaopay.sprinkle.model.DetailedSprinkleMoneyResponse
import com.telltale.kakaopay.sprinkle.model.ErrorResponse
import com.telltale.kakaopay.sprinkle.model.HTTP_HEADER_ROOM_ID
import com.telltale.kakaopay.sprinkle.model.HTTP_HEADER_USER_ID
import com.telltale.kakaopay.sprinkle.model.SprinkleMoneyResponse
import com.telltale.kakaopay.sprinkle.model.SprinkleRequest
import com.telltale.kakaopay.sprinkle.model.SprinkleResponse
import com.telltale.kakaopay.sprinkle.repository.model.TokenId
import com.telltale.kakaopay.sprinkle.service.SprinkleMoneyBasketService
import com.telltale.kakaopay.sprinkle.rest.service.generateTokenId
import com.telltale.kakaopay.sprinkle.rest.service.makeBasketMoneyList
import com.telltale.kakaopay.sprinkle.service.SprinkleMoneyQueryService
import com.telltale.kakaopay.sprinkle.service.SprinkleMoneyService
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.testcontainers.shaded.com.google.common.annotations.VisibleForTesting
import reactor.core.publisher.Mono
import java.lang.IllegalArgumentException
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant

private const val EXPIRED_TAKE_TIME_MILLIS = 10 * 60 * 1000
private const val EXPIRED_SHOW_TIME_MILLIS = 7 * 24 * 60 * 60 * 1000

@RestController
@RequestMapping("/v1/kakao-pay/money")
class SprinkleController(
    val sprinkleMoneyService: SprinkleMoneyService,
    val sprinkleMoneyBasketService: SprinkleMoneyBasketService,
    val sprinkleMoneyQueryService: SprinkleMoneyQueryService
) {
    @VisibleForTesting
    var clock = Clock.systemUTC()

    @PostMapping("/sprinkle")
    fun registerSprinkleMoney(
        @RequestHeader(value = HTTP_HEADER_USER_ID) userId: String,
        @RequestHeader(value = HTTP_HEADER_ROOM_ID) roomId: String,
        @RequestBody request: SprinkleRequest
    ): Mono<SprinkleResponse> = mono {
        val tokenId = generateTokenId()
        val basketMoneyList = makeBasketMoneyList(
            amount = request.amount,
            receiverCount = request.receiverCount
        )
        sprinkleMoneyService.registerSprinkleMoney(tokenId, basketMoneyList, userId, roomId, request)
        SprinkleResponse(tokenId)
    }

    @PostMapping("/sprinkle/{tokenId}")
    fun takeSprinkleMoney(
        @RequestHeader(value = HTTP_HEADER_USER_ID) userId: String,
        @RequestHeader(value = HTTP_HEADER_ROOM_ID) roomId: String,
        @PathVariable tokenId: TokenId
    ): Mono<SprinkleMoneyResponse> = mono {
        val historyEntity = sprinkleMoneyQueryService.getSprinkleMoneyHistoryOrException(tokenId)
        if (historyEntity.userId == userId) {
            throw SprinkleException(ErrorResponse.OWNER_CANNOT_TAKE_MONEY)
        }
        if (isExpiredTakingMoney(historyEntity.createdAt)) {
            throw SprinkleException(ErrorResponse.EXPIRED_TAKING_MONEY)
        }

        val money = sprinkleMoneyBasketService.takeSprinkleMoney(tokenId, userId)
        money?.let {
            SprinkleMoneyResponse(it)
        } ?: throw SprinkleException(ErrorResponse.INTERNAL_SERVER_ERROR)
    }

    private fun isExpiredTakingMoney(createdAt: Timestamp): Boolean {
        return Instant.ofEpochMilli(createdAt.time + EXPIRED_TAKE_TIME_MILLIS).isBefore(clock.instant())
    }

    @GetMapping("/sprinkle/{tokenId}")
    fun getSprinkleMoney(
        @RequestHeader(value = HTTP_HEADER_USER_ID) userId: String,
        @RequestHeader(value = HTTP_HEADER_ROOM_ID) roomId: String,
        @PathVariable tokenId: TokenId
    ): Mono<DetailedSprinkleMoneyResponse> = mono {
        val historyEntity = sprinkleMoneyQueryService.getSprinkleMoneyHistoryOrException(tokenId)
        if (historyEntity.userId != userId) {
            throw SprinkleException(ErrorResponse.UNAUTHORIZED_MONEY)
        }
        if (isExpiredShowMoney(historyEntity.createdAt)) {
            throw SprinkleException(ErrorResponse.EXPIRED_SHOW_MONEY)
        }
        sprinkleMoneyQueryService.getSprinkleMoneyInfo(tokenId, userId, roomId)
    }

    private fun isExpiredShowMoney(createdAt: Timestamp): Boolean {
        println("### ${createdAt.time + EXPIRED_SHOW_TIME_MILLIS}")
        println("#### ${clock.instant().toEpochMilli()}")
        return Instant.ofEpochMilli(createdAt.time + EXPIRED_SHOW_TIME_MILLIS).isBefore(clock.instant())
    }

    @ExceptionHandler(Exception::class)
    @ResponseBody
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(SprinkleException::class)
    @ResponseBody
    fun handleSprinkleException(e: SprinkleException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(e.errorResponse.code)
            .body(e.errorResponse)
    }
}
