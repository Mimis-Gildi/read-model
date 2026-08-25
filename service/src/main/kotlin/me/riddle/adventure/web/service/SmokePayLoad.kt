package me.riddle.adventure.web.service

class SmokePayLoad (override val description: String): PayLoad by Companion {
    companion object : PayLoad {
        override val description: String = "Smoke"
    }
}
