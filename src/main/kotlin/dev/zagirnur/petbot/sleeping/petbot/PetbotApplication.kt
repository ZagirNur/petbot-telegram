package dev.zagirnur.petbot.sleeping.petbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication(scanBasePackages = ["dev.zagirnur.petbot"])
class PetbotApplication

fun main(args: Array<String>) {
    runApplication<PetbotApplication>(*args)
}
