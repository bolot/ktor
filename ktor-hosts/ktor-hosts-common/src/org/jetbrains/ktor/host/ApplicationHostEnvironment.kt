package org.jetbrains.ktor.host

import com.jdiazcano.cfg4k.loaders.*
import com.jdiazcano.cfg4k.providers.*
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.logging.*

/**
 * Represents an environment in which host runs
 */
interface ApplicationHostEnvironment : ApplicationEnvironment {
    /**
     * Connectors that describers where and how server should listen
     */
    val connectors: List<HostConnectorConfig>

    /**
     * Running [Application]
     *
     * Throws an exception if environment has not been started
     */
    val application: Application

    /**
     * Starts [ApplicationHostEnvironment] and creates an application
     */
    fun start()

    /**
     * Stops [ApplicationHostEnvironment] and destroys any running application
     */
    fun stop()
}

/**
 * Creates [ApplicationHostEnvironment] using [ApplicationHostEnvironmentBuilder]
 */
fun applicationHostEnvironment(builder: ApplicationHostEnvironmentBuilder.() -> Unit): ApplicationHostEnvironment {
    return ApplicationHostEnvironmentBuilder().build(builder)
}

class ApplicationHostEnvironmentBuilder {
    var classLoader: ClassLoader = ApplicationHostEnvironment::class.java.classLoader
    var log: ApplicationLog = NullApplicationLog()

    var configProvider: ConfigProvider = Providers.proxy(object : ConfigLoader {
        override fun get(key: String): String? = null
        override fun reload() {}
    })

    val connectors = mutableListOf<HostConnectorConfig>()
    val modules = mutableListOf<Application.() -> Unit>()

    fun module(body: Application.() -> Unit) {
        modules.add(body)
    }

    fun build(builder: ApplicationHostEnvironmentBuilder.() -> Unit): ApplicationHostEnvironment {
        builder(this)
        val applicationConfig = configProvider.bind<ApplicationConfiguration>("ktor.application")
        return ApplicationHostEnvironmentReloading(classLoader, log, configProvider, connectors, modules, applicationConfig)
    }
}