package com.telltale.kakaopay.sprinkle.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.telltale.kakaopay.sprinkle.TestSprinkleApplication
import com.telltale.kakaopay.sprinkle.test.container.JDBCMySqlContainerConfigurationInitializer
import com.telltale.kakaopay.sprinkle.test.container.mySqlContainer
import com.telltale.kakaopay.sprinkle.test.randomShortString
import com.telltale.kakaopay.sprinkle.test.randomSmallList
import com.telltale.kakaopay.sprinkle.test.randomSprinkleMoneyBasketEntity
import com.telltale.kakaopay.sprinkle.test.randomTokenId
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import javax.inject.Inject

@SpringBootTest(classes = [TestSprinkleApplication::class])
@ContextConfiguration(initializers = [SprinkleMoneyBasketRepositoryIT.JDBCInitializer::class])
@ActiveProfiles("testbed")
@Testcontainers
class SprinkleMoneyBasketRepositoryIT {
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
    private lateinit var sprinkleMoneyBasketRepository: SprinkleMoneyBasketRepository

    @Test
    fun save() {
        // given
        val entities = generateSequence { randomSprinkleMoneyBasketEntity() }.randomSmallList()

        // when
        sprinkleMoneyBasketRepository.saveAll(entities)

        // then
        val actual = sprinkleMoneyBasketRepository.findAll()

        val expected = entities
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `takeOneSprinkleMoney succeeds`() {
        // given
        val tokenId = randomTokenId()
        val entities = generateSequence { randomSprinkleMoneyBasketEntity(tokenId = tokenId) }.randomSmallList()
        sprinkleMoneyBasketRepository.saveAll(entities)

        // when
        val receivedUserId = randomShortString()
        sprinkleMoneyBasketRepository.takeOneSprinkleMoney(tokenId = tokenId, receivedUserId = receivedUserId)

        // then
        val actual = sprinkleMoneyBasketRepository.findByTokenIdAndReceivedUserId(tokenId, receivedUserId)
        assertThat(actual!!.receivedUserId).isEqualTo(receivedUserId)
    }

    @Test
    fun `takeOneSprinkleMoney fails due to duplicate`() {
        // given
        val tokenId = randomTokenId()
        val entities = generateSequence { randomSprinkleMoneyBasketEntity(tokenId = tokenId) }.randomSmallList()
        sprinkleMoneyBasketRepository.saveAll(entities)

        // when
        val receivedUserId = randomShortString()
        sprinkleMoneyBasketRepository.takeOneSprinkleMoney(tokenId = tokenId, receivedUserId = receivedUserId)

        val actual = assertThrows<Exception> {
            sprinkleMoneyBasketRepository.takeOneSprinkleMoney(tokenId = tokenId, receivedUserId = receivedUserId)
        }
        // then
        assertThat(actual).isInstanceOf(DataIntegrityViolationException::class)
    }
}