package com.telltale.kakaopay.sprinkle

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class TestSprinkleApplication

fun main(args: Array<String>) {
    val app = SpringApplication(TestSprinkleApplication::class.java)
    app.run(*args)
}