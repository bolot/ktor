package org.jetbrains.ktor.host

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.logging.*

interface ApplicationHostFactory<out THost : ApplicationHost> {
    fun create(environment: ApplicationHostEnvironment): THost
}

fun <THost : ApplicationHost> embeddedServer(factory: ApplicationHostFactory<THost>,
                                             port: Int = 80,
                                             host: String = "0.0.0.0",
                                             watchPaths: List<String> = emptyList(),
                                             module: Application.() -> Unit): THost {
    val environment = applicationHostEnvironment {
        this.log = SLF4JApplicationLog("ktor.application")
        this.module(module)

        connector {
            this.port = port
            this.host = host
        }
    }

    return embeddedServer(factory, environment)
}

fun <THost : ApplicationHost> embeddedServer(factory: ApplicationHostFactory<THost>, environment: ApplicationHostEnvironment): THost {
    return factory.create(environment)
}

