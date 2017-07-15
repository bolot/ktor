package org.jetbrains.ktor.tests

import com.jdiazcano.cfg4k.loaders.*
import com.jdiazcano.cfg4k.providers.*
import com.typesafe.config.*
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.config.*
import org.jetbrains.ktor.host.*
import org.jetbrains.ktor.util.*
import org.junit.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.test.*

class ApplicationHostEnvironmentReloadingTests {
    fun configProvider(vararg pairs: Pair<String, String>): ConfigProvider {
        val map = pairs.toMap()
        val loader = object : ConfigLoader {
            override fun get(key: String): String? = map[key]
            override fun reload() {}
        }
        return Providers.proxy(loader)
    }

    @Test fun `top level extension function as module function`() {
        val environment = applicationHostEnvironment {
            configProvider = configProvider("ktor.application.modules" to Application::topLevelExtensionFunction.fqName)
        }

        environment.start()
        val application = environment.application
        assertNotNull(application)
        assertEquals("topLevelExtensionFunction", application.attributes[TestKey])
        environment.stop()
    }

    @Test fun `top level extension function as module function reloading stress`() {
        val environment = applicationHostEnvironment {
            configProvider = configProvider(
                    "ktor.deployment.watch" to "ktor-hosts-common",
                    "ktor.application.modules" to Application::topLevelExtensionFunction.fqName
            )
        }

/*
        fun collectGC() {
            System.gc()
            Thread.sleep(500)
            System.gc()
        }

        collectGC()
        val memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        println("Before: ${memoryBefore / 1024} Kb")
*/

        environment.start()
        repeat(100) {
            (environment as ApplicationHostEnvironmentReloading).reload()
        }
        environment.stop()

/*
        collectGC()
        val memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        println("After: ${memoryAfter / 1024} Kb")
        println("Diff: ${(memoryAfter - memoryBefore) / 1024} Kb")
*/
    }


    @Test fun `top level non-extension function as module function`() {
        val environment = applicationHostEnvironment {
            configProvider = configProvider("ktor.application.modules" to ::topLevelFunction.fqName)
        }
        environment.start()
        val application = environment.application
        assertNotNull(application)
        assertEquals("topLevelFunction", application.attributes[TestKey])
        environment.stop()
    }

    @Test fun `companion object extension function as module function`() {
        val environment = applicationHostEnvironment {
            configProvider = configProvider("ktor.application.modules" to Companion::class.jvmName + "." + "companionObjectExtensionFunction")
        }
        environment.start()
        val application = environment.application
        assertNotNull(application)
        assertEquals("companionObjectExtensionFunction", application.attributes[TestKey])
        environment.stop()
    }

    @Test fun `companion object non-extension function as module function`() {
        val environment = applicationHostEnvironment {
            configProvider = configProvider("ktor.application.modules" to Companion::class.functionFqName("companionObjectFunction"))
        }
        environment.start()
        val application = environment.application
        assertNotNull(application)
        assertEquals("companionObjectFunction", application.attributes[TestKey])
        environment.stop()
    }

    @Test fun `companion object jvmstatic extension function as module function`() {
        val environment = applicationHostEnvironment {
            configProvider = configProvider("ktor.application.modules" to Companion::class.jvmName + "." + "companionObjectJvmStaticExtensionFunction")
        }

        environment.start()
        val application = environment.application
        assertNotNull(application)
        assertEquals("companionObjectJvmStaticExtensionFunction", application.attributes[TestKey])
        environment.stop()
    }

    @Test fun `companion object jvmstatic non-extension function as module function`() {
        val environment = applicationHostEnvironment {
            configProvider = configProvider("ktor.application.modules" to Companion::class.functionFqName("companionObjectJvmStaticFunction"))
        }
        environment.start()
        val application = environment.application
        assertNotNull(application)
        assertEquals("companionObjectJvmStaticFunction", application.attributes[TestKey])
        environment.stop()
    }

    @Test fun `object holder extension function as module function`() {
        val environment = applicationHostEnvironment {
            configProvider = configProvider("ktor.application.modules" to ObjectModuleFunctionHolder::class.jvmName + "." + "objectExtensionFunction")
        }
        environment.start()
        val application = environment.application
        assertNotNull(application)
        assertEquals("objectExtensionFunction", application.attributes[TestKey])
        environment.stop()
    }

    @Test fun `object holder non-extension function as module function`() {
        val environment = applicationHostEnvironment {
            configProvider = configProvider("ktor.application.modules" to ObjectModuleFunctionHolder::class.functionFqName("objectFunction"))
        }
        environment.start()
        val application = environment.application
        assertNotNull(application)
        assertEquals("objectFunction", application.attributes[TestKey])
        environment.stop()
    }

    @Test fun `class holder extension function as module function`() {
        val environment = applicationHostEnvironment {
            configProvider = configProvider("ktor.application.modules" to ClassModuleFunctionHolder::class.jvmName + "." + "classExtensionFunction")
        }
        environment.start()
        val application = environment.application
        assertNotNull(application)
        assertEquals("classExtensionFunction", application.attributes[TestKey])
        environment.stop()
    }

    @Test fun `class holder non-extension function as module function`() {
        val environment = applicationHostEnvironment {
            configProvider = configProvider("ktor.application.modules" to ClassModuleFunctionHolder::classFunction.fqName)
        }
        environment.start()
        val application = environment.application
        assertNotNull(application)
        assertEquals("classFunction", application.attributes[TestKey])
        environment.stop()
    }

    @Test fun `no-arg module function`() {
        val environment = applicationHostEnvironment {
            configProvider = configProvider("ktor.application.modules" to NoArgModuleFunction::class.functionFqName("main"))
        }
        environment.start()
        val application = environment.application
        assertNotNull(application)
        assertEquals(1, NoArgModuleFunction.result)
        environment.stop()
    }

    @Test fun `multiple module functions`() {
        val environment = applicationHostEnvironment {
            configProvider = configProvider("ktor.application.modules" to MultipleModuleFunctions::class.jvmName + ".main")
        }
        environment.start()
        val application = environment.application
        assertNotNull(application)
        assertEquals("best function called", application.attributes[TestKey])
        environment.stop()
    }

    object NoArgModuleFunction {
        var result = 0

        fun main() {
            result++
        }
    }

    object MultipleModuleFunctions {
        fun main() {
        }

        fun main(app: Application) {
            app.attributes.put(ApplicationHostEnvironmentReloadingTests.TestKey, "best function called")
        }
    }

    class ClassModuleFunctionHolder {
        @Suppress("UNUSED")
        fun Application.classExtensionFunction() {
            attributes.put(ApplicationHostEnvironmentReloadingTests.TestKey, "classExtensionFunction")
        }

        fun classFunction(app: Application) {
            app.attributes.put(ApplicationHostEnvironmentReloadingTests.TestKey, "classFunction")
        }
    }

    object ObjectModuleFunctionHolder {
        @Suppress("UNUSED")
        fun Application.objectExtensionFunction() {
            attributes.put(ApplicationHostEnvironmentReloadingTests.TestKey, "objectExtensionFunction")
        }

        fun objectFunction(app: Application) {
            app.attributes.put(ApplicationHostEnvironmentReloadingTests.TestKey, "objectFunction")
        }
    }

    companion object {
        val TestKey = AttributeKey<String>("test-key")

        private val KFunction<*>.fqName: String
            get() = javaMethod!!.declaringClass.name + "." + name

        private fun KClass<*>.functionFqName(name: String) = "$jvmName.$name"

        @Suppress("UNUSED")
        fun Application.companionObjectExtensionFunction() {
            attributes.put(ApplicationHostEnvironmentReloadingTests.TestKey, "companionObjectExtensionFunction")
        }

        fun companionObjectFunction(app: Application) {
            app.attributes.put(ApplicationHostEnvironmentReloadingTests.TestKey, "companionObjectFunction")
        }

        @Suppress("UNUSED")
        @JvmStatic
        fun Application.companionObjectJvmStaticExtensionFunction() {
            attributes.put(ApplicationHostEnvironmentReloadingTests.TestKey, "companionObjectJvmStaticExtensionFunction")
        }

        @JvmStatic
        fun companionObjectJvmStaticFunction(app: Application) {
            app.attributes.put(ApplicationHostEnvironmentReloadingTests.TestKey, "companionObjectJvmStaticFunction")
        }
    }
}

fun Application.topLevelExtensionFunction() {
    attributes.put(ApplicationHostEnvironmentReloadingTests.TestKey, "topLevelExtensionFunction")
}

fun topLevelFunction(app: Application) {
    app.attributes.put(ApplicationHostEnvironmentReloadingTests.TestKey, "topLevelFunction")
}

