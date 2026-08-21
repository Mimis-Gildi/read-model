import ControlPlaneBrowserLauncher.Companion.CONTROL_PLANE_BROWSE_TASK_NAME
import kotlinx.coroutines.*
import java.awt.Desktop
import java.io.IOException
import java.net.Socket
import java.net.URI
import kotlin.time.Duration.Companion.milliseconds
import java.awt.Desktop.getDesktop as desktop

/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(ktor_libs.plugins.ktor)
}

group = "me.riddle.adventure.web.service"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

tasks.test {
    useJUnitPlatform()
}

/* The control plane page ships compiled.*/
tasks.processResources {
    from(project(":control").tasks.matching { it.name == "jsBrowserProductionLibraryDistribution" }) {
        into("control")
    }
    from(project(":harness").tasks.matching { it.name == "jsBrowserProductionLibraryDistribution" }) {
        into("harness")
    }
    from(project(":pure").tasks.matching { it.name == "build" }) {
        into("pure")
    }
    from(project(":react").tasks.matching { it.name == "build" }) {
        into("react")
    }
    from(project(":kobweb").tasks.matching { it.name == "kobwebExport" }) {
        into("kobweb")
        exclude("index.html")
    }
    from(project(":compose").tasks.matching { it.name == "jsBrowserDistribution" }) {
        into("compose")
    }
    /* React itself, bundled from npm rather than fetched by hand. */
    from(project(":react").tasks.matching { it.name == "bundleVendor" }) {
        into("vendor/react")
    }
}


dependencies {
    implementation(project(":model"))

    implementation(ktor_libs.serialization.kotlinx.json)
    implementation(ktor_libs.server.cachingHeaders)
    implementation(ktor_libs.server.callLogging)
    implementation(ktor_libs.server.compression)
    implementation(ktor_libs.server.config.yaml)
    implementation(ktor_libs.server.contentNegotiation)
    implementation(ktor_libs.server.core)
    implementation(ktor_libs.server.cors)
    implementation(ktor_libs.server.metrics)
    implementation(ktor_libs.server.metrics.micrometer)
    implementation(ktor_libs.server.netty)
    implementation(ktor_libs.server.statusPages)
    implementation(ktor_libs.server.websockets)
    implementation(ktor_libs.server.resources)

    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)

    implementation(libs.micrometer.registry.prometheus)

    testImplementation(kotlin("test"))
    testImplementation(ktor_libs.server.testHost)
    testImplementation(ktor_libs.client.websockets)
}


tasks.named<JavaExec>("run") { dependsOn(browseControlPlane) }

/*

======================================================================================================================

                    Bootstrap Browser for demoscene style open from commandline.

            Browser can be launched from JS and JVM already present in the project.


======================================================================================================================

*/

val browseControlPlane = tasks.register<ControlPlaneBrowserLauncher>(CONTROL_PLANE_BROWSE_TASK_NAME) {
    description = "This task launches the Control Plane page server by Ktor :service module as a convenience on :service:run."

    onlyIf {
        isLaunchBrowserCondition.get().also {
            if (!it) logger.warn(ControlPlaneBrowserLauncher.MSG_NO_BROWSER.trimIndent())
        }
    }
}


abstract class ControlPlaneBrowserLauncher : DefaultTask() {

    @get:Inject
    abstract val providers: ProviderFactory

    @get:Inject
    abstract val execOps: ExecOperations

    class OsMatcher(val nameSubstring: String) {
        operator fun contains(text: CharSequence) = text.contains(nameSubstring)
    }


    @get:Internal
    val browserLauncherScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun Job.convergeBrowserLauncher() = invokeOnCompletion { cause ->
        browserLauncherScope.cancel()
        logger.lifecycle(MSG_COROUTINE_CONVERGED, cause?.message ?: "opened")
    }

    val isLaunchBrowserByEnvironmentKey: Provider<Boolean> by lazy {
        providers
            .environmentVariablesPrefixedBy(LAUNCH_BROWSER_BY_ENV)
            .map { env -> true.takeIf { env.containsKey(LAUNCH_BROWSER_BY_ENV) } }
    }

    val isLaunchBrowserByPropertiesValue: Provider<Boolean> by lazy {
        providers.gradleProperty(LAUNCH_BROWSER_PROP_NAME).map { it.toBoolean() }
    }

    val isLaunchBrowserCondition: Provider<Boolean> by lazy {
        isLaunchBrowserByEnvironmentKey
            .orElse(isLaunchBrowserByPropertiesValue)
            .orElse(false)
    }


    @get:Input
    val controlPlaneUri: URI by lazy { URI.create(CONTROL_PLANE_DEFAULT_LOCAL_ADDRESS) }

    @get:Input
    val osNameToLower: String by lazy { System.getProperty(OS_NAME_KEY).lowercase() }


    @get:Input
    val osBrowserLaunchCommand by lazy {
        when (osNameToLower) {
            in OsMatcher("win") -> listOf("cmd", "/c", "start", controlPlaneUri.toString())
            in OsMatcher("mac") -> listOf("open", controlPlaneUri.toString())
            else -> listOf("xdg-open", controlPlaneUri.toString())
        }
    }

    suspend fun URI.launchBrowserUsingDesktop() = launchBrowserOnSocketBackoffRetry {
        desktop().browse(this)
    }

    suspend fun URI.launchBrowserUsingOs() = launchBrowserOnSocketBackoffRetry {
        execOps.exec {
            commandLine(osBrowserLaunchCommand)
        }
    }

    suspend fun URI.isSocketClosed(): Boolean = try {
        withContext(Dispatchers.IO) {
            Socket(host, port).use { false }
        }
    } catch (_: IOException) {
        true
    }

    tailrec suspend fun URI.launchBrowserOnSocketBackoffRetry(
        attempt: Int = 1,
        maxAttempts: Int = 67,
        delayMs: Long = 3600,
        minDelayMs: Long = 300,
        maxDelayMs: Long = 13100,
        invokeBrowser: () -> Unit
    ) {
        logger.lifecycle(MSG_AWAIT_PORT_FOR_BROWSER.trimIndent(), delayMs, attempt, maxAttempts)
        delay(delayMs.milliseconds)

        when (isSocketClosed()) {
            false -> withContext(Dispatchers.IO) { invokeBrowser() }.run {
                logger.lifecycle(MSG_BROWSER_CALL_DONE.trimIndent(), this@launchBrowserOnSocketBackoffRetry.toString())
            }

            true -> if (maxAttempts <= attempt) logger.error(MSG_BROWSER_CALL_FAIL.trimIndent(), attempt, toString())
            else launchBrowserOnSocketBackoffRetry(
                attempt = attempt + 1,
                delayMs = maxOf(minDelayMs, (minOf(delayMs, maxDelayMs) * 0.93).toLong()),
                invokeBrowser = invokeBrowser
            )
        }
    }


    @TaskAction
    fun launchControlPlaneBrowserWindow() = controlPlaneUri.run {
        browserLauncherScope.launch {
            (Desktop.isDesktopSupported() &&
                    desktop().isSupported(Desktop.Action.BROWSE))
                .let { isDesktop ->
                    if (isDesktop) launchBrowserUsingDesktop()
                    else launchBrowserUsingOs()

                }
        }.convergeBrowserLauncher()

    }

    companion object {
        const val OS_NAME_KEY = "os.name"
        const val LAUNCH_BROWSER_BY_ENV = "LAUNCH_BROWSER"
        const val LAUNCH_BROWSER_PROP_NAME = "launchBrowserOnRun"
        const val CONTROL_PLANE_DEFAULT_LOCAL_ADDRESS = "http://0.0.0.0:48080/"
        const val CONTROL_PLANE_BROWSE_TASK_NAME = "browseControlPlane"

        const val MSG_COROUTINE_CONVERGED = "\t |==> Browser launcher converged: {}"
        const val MSG_AWAIT_PORT_FOR_BROWSER =
            """
                
                ================================================================================
                    ... Browser will wait {} for Control Plane
                        Attempt:        {} of {}
                ================================================================================
                
            """
        const val MSG_BROWSER_CALL_DONE =
            """
                    
                ================================================================================
                    Launched the browser to {}
                ================================================================================
                
            """
        const val MSG_BROWSER_CALL_FAIL =
            """
                    
                ================================================================================
                    Retries expired at {}
                    
                    Please navigate to {}
                ================================================================================
                
            """
        const val MSG_NO_BROWSER =
            """
                    Lagrdom Reactum Lorum       https://mimis-gildi.github.io/read-model/
                ================================================================================
                    BROWSER WILL NOT BE LAUNCHED!
                    
                    set gradle.properties value $LAUNCH_BROWSER_PROP_NAME to true
                    OR
                    environment variable $LAUNCH_BROWSER_BY_ENV
                    
                ================================================================================
                
                
            """
    }
}
