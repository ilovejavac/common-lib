package com.dev.lib.ai.config

import com.dev.lib.util.Dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

@ComponentScan(basePackages = ["com.dev.lib.ai"])
@Configuration
class CommonAiAutoConfig {
}

val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
fun main() {
    runBlocking {
        println(0)
        test()
        println(1)

        delay(5000)
    }
}

suspend fun test() {
    println(2)

    scope.launch {
        delay(2000)
        println(3)
    }
    println(".")
    scope.launch {
        delay(1000)
        println(4)
    }
    println(".")
    println(5)

}