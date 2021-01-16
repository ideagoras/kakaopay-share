package com.telltale.kakaopay.sprinkle.rest

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.telltale.kakaopay.sprinkle.TestSprinkleApplication
import com.telltale.kakaopay.sprinkle.model.DetailedSprinkleMoneyResponse
import com.telltale.kakaopay.sprinkle.model.HTTP_HEADER_ROOM_ID
import com.telltale.kakaopay.sprinkle.model.HTTP_HEADER_USER_ID
import com.telltale.kakaopay.sprinkle.model.ReceivedUserInfo
import com.telltale.kakaopay.sprinkle.model.SprinkleMoneyResponse
import com.telltale.kakaopay.sprinkle.model.SprinkleResponse
import com.telltale.kakaopay.sprinkle.randomSprinkleRequest
import com.telltale.kakaopay.sprinkle.repository.SprinkleMoneyBasketRepository
import com.telltale.kakaopay.sprinkle.repository.SprinkleMoneyHistoryRepository
import com.telltale.kakaopay.sprinkle.repository.model.SprinkleMoneyBasketEntity
import com.telltale.kakaopay.sprinkle.repository.model.SprinkleMoneyHistoryEntity
import com.telltale.kakaopay.sprinkle.rest.service.makeBasketMoneyList
import com.telltale.kakaopay.sprinkle.service.SprinkleMoneyBasketService
import com.telltale.kakaopay.sprinkle.service.SprinkleMoneyService
import com.telltale.kakaopay.sprinkle.test.container.JDBCMySqlContainerConfigurationInitializer
import com.telltale.kakaopay.sprinkle.test.container.mySqlContainer
import com.telltale.kakaopay.sprinkle.test.randomMoney
import com.telltale.kakaopay.sprinkle.test.randomReceiverCount
import com.telltale.kakaopay.sprinkle.test.randomShortString
import com.telltale.kakaopay.sprinkle.test.randomSprinkleMoneyBasketEntity
import com.telltale.kakaopay.sprinkle.test.randomSprinkleMoneyHistoryEntity
import com.telltale.kakaopay.sprinkle.test.randomTokenId
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@SpringBootTest(classes = [TestSprinkleApplication::class])
@ContextConfiguration(initializers = [SprinkleControllerIT.JDBCInitializer::class])
@ActiveProfiles("testbed")
@Testcontainers
@AutoConfigureWebTestClient(timeout = "10000")
class SprinkleControllerIT {
    companion object {
        private val DATABASE_NAME: String = RandomStringUtils.randomAlphabetic(8).toLowerCase()

        @Container
        val MYSQL_CONTAINER = mySqlContainer(databaseName = DATABASE_NAME).apply {
            withInitScript("sql/schema-mysql.sql")
        }
    }

    class JDBCInitializer : JDBCMySqlContainerConfigurationInitializer() {
        override val jdbcUrlKey = "spring.datasource.url"
        override val userNameKey = "spring.datasource.username"
        override val passwordKey = "spring.datasource.password"
        override val mySqlContainer = MYSQL_CONTAINER
    }

    @Inject
    private lateinit var webTestClient: WebTestClient

    @Inject
    private lateinit var sprinkleMoneyHistoryRepository: SprinkleMoneyHistoryRepository

    @Inject
    private lateinit var sprinkleMoneyBasketRepository: SprinkleMoneyBasketRepository

    @Inject
    private lateinit var sprinkleMoneyService: SprinkleMoneyService

    @Inject
    private lateinit var sprinkleMoneyBasketService: SprinkleMoneyBasketService

    private lateinit var clock: Clock

    @BeforeEach
    fun setUp() {
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
        sprinkleMoneyService.clock = clock
        sprinkleMoneyBasketService.clock = clock
    }

    @AfterEach
    fun tearDown() {
        sprinkleMoneyHistoryRepository.deleteAllInBatch()
        sprinkleMoneyBasketRepository.deleteAllInBatch()
    }

    @Test
    fun `registerSprinkleMoney succeeds`() {
        // given

        // when
        val userId = randomShortString()
        val roomId = randomShortString()
        val receiverCount = randomReceiverCount()
        val amount = receiverCount * randomMoney()
        val sprinkleRequest = randomSprinkleRequest(receiverCount = receiverCount, amount = amount)
        val actual = webTestClient.post()
            .uri("/v1/kakao-pay/money/sprinkle")
            .headers {
                it.set(HTTP_HEADER_USER_ID, userId)
                it.set(HTTP_HEADER_ROOM_ID, roomId)
            }
            .bodyValue(sprinkleRequest)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(SprinkleResponse::class.java).returnResult().responseBody!!

        // then
        val actualHistory = sprinkleMoneyHistoryRepository.findAll().first()
        val expectedHistory = SprinkleMoneyHistoryEntity(
            id = 0L,
            userId = userId,
            roomId = roomId,
            tokenId = actual.tokenId,
            receiverCount = receiverCount,
            amount = amount,
            createdAt = Timestamp.from(clock.instant())
        )
        val actualHistoryExceptId = actualHistory.copy(id = 0L)
        assertThat(actualHistoryExceptId).isEqualTo(expectedHistory)

        val actualBasketCount = sprinkleMoneyBasketRepository.findAll()
            .filter { it.tokenId == actual.tokenId }
            .size
        assertThat(actualBasketCount).isEqualTo(receiverCount)
    }

    @Test
    fun `takeSprinkleMoney succeeds`() {
        // given
        val userId = randomShortString()
        val roomId = randomShortString()
        val tokenId = randomTokenId()
        val receiverCount = randomReceiverCount()
        val basketEntities = (0 until receiverCount).map {
            SprinkleMoneyBasketEntity(
                tokenId = tokenId,
                money = randomMoney(),
                createdAt = Timestamp.from(clock.instant())
            )
        }
        sprinkleMoneyBasketRepository.saveAll(basketEntities)

        // when
        val actual = webTestClient.post()
            .uri("/v1/kakao-pay/money/sprinkle/$tokenId")
            .headers {
                it.set(HTTP_HEADER_USER_ID, userId)
                it.set(HTTP_HEADER_ROOM_ID, roomId)
            }
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(SprinkleMoneyResponse::class.java).returnResult().responseBody!!

        // then
        val actualBasketEntity = sprinkleMoneyBasketRepository.findAll().first { it.receivedUserId != null }
        val expectedBasketEntity = basketEntities.first().copy(
            receivedUserId = userId,
            updatedAt = Timestamp.from(clock.instant())
        )
        assertThat(actualBasketEntity).isEqualTo(expectedBasketEntity)

        val expected = SprinkleMoneyResponse(money = actualBasketEntity.money)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `getSprinkleMoney succeeds`() {
        // given
        val userId = randomShortString()
        val roomId = randomShortString()
        val tokenId = randomTokenId()
        val receiverCount = randomReceiverCount()
        val amount = randomMoney()

        val historyEntity = randomSprinkleMoneyHistoryEntity(
            userId = userId,
            roomId = roomId,
            tokenId = tokenId,
            receiverCount = receiverCount,
            amount = amount,
            createdAt = Timestamp.from(clock.instant())
        )
        sprinkleMoneyHistoryRepository.save(historyEntity)

        val basketMoneyList = makeBasketMoneyList(amount, receiverCount)
        val basketEntities = basketMoneyList.map {
            randomSprinkleMoneyBasketEntity(
                tokenId = tokenId,
                receivedUserId = randomShortString(),
                money = it,
                updatedAt = Timestamp.from(clock.instant())
            )
        }
        sprinkleMoneyBasketRepository.saveAll(basketEntities)

        // when
        val actual = webTestClient.get()
            .uri("/v1/kakao-pay/money/sprinkle/$tokenId")
            .headers {
                it.set(HTTP_HEADER_USER_ID, userId)
                it.set(HTTP_HEADER_ROOM_ID, roomId)
            }
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(DetailedSprinkleMoneyResponse::class.java).returnResult().responseBody!!

        // then
        val receivedUsers = basketEntities.map {
            ReceivedUserInfo(userId = it.receivedUserId!!, money = it.money)
        }
        val expected = DetailedSprinkleMoneyResponse(
            createdAt = clock.millis(),
            amount = amount,
            totalReceivedMoney = amount,
            receivedUsers = receivedUsers
        )
        assertThat(actual).isEqualTo(expected)
    }
}