package com.telltale.kakaopay.sprinkle.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.telltale.kakaopay.sprinkle.TestSprinkleApplication
import com.telltale.kakaopay.sprinkle.test.container.JDBCMySqlContainerConfigurationInitializer
import com.telltale.kakaopay.sprinkle.test.container.mySqlContainer
import com.telltale.kakaopay.sprinkle.test.randomSmallList
import com.telltale.kakaopay.sprinkle.test.randomSprinkleMoneyHistoryEntity
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import javax.inject.Inject

@SpringBootTest(classes = [TestSprinkleApplication::class])
@ContextConfiguration(initializers = [SprinkleMoneyHistoryRepositoryIT.JDBCInitializer::class])
@ActiveProfiles("testbed")
@Testcontainers
class SprinkleMoneyHistoryRepositoryIT {
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
    private lateinit var sprinkleMoneyHistoryRepository: SprinkleMoneyHistoryRepository

    @Test
    fun save() {
        // given
        val entities = generateSequence { randomSprinkleMoneyHistoryEntity() }.randomSmallList()

        // when
        sprinkleMoneyHistoryRepository.saveAll(entities)

        // then
        val actual = sprinkleMoneyHistoryRepository.findAll()

        val expected = entities
        assertThat(actual).isEqualTo(expected)
    }
}