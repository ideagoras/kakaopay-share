package com.telltale.kakaopay.sprinkle.test.container

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.MySQLContainer

abstract class JDBCMySqlContainerConfigurationInitializer :
    ApplicationContextInitializer<ConfigurableApplicationContext> {

    abstract val jdbcUrlKey: String
    abstract val userNameKey: String
    abstract val passwordKey: String
    abstract val mySqlContainer: MySQLContainer<Nothing>

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        TestPropertyValues.of(
            "$jdbcUrlKey=${mySqlContainer.jdbcUrl}",
            "$userNameKey=${mySqlContainer.username}",
            "$passwordKey=${mySqlContainer.password}"
        ).applyTo(applicationContext.environment)
    }
}

fun mySqlContainer(
    databaseName: String = "database",
    userName: String = "user",
    password: String = "password"
): MySQLContainer<Nothing> {
    val mySqlContainer = MySQLContainer<Nothing>()
    mySqlContainer.withDatabaseName(databaseName)
    mySqlContainer.withUsername(userName)
    mySqlContainer.withPassword(password)
    return mySqlContainer
}