package app.config

import io.javalin.Javalin

object JavalinConfig {

    fun create(): Javalin {
        return Javalin.create { config ->
            config.plugins.enableDevLogging()
        }
    }
}