package com.example.drinkder.network

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.HttpException

internal class ApiRequestRunner(
    private val pause: suspend (Long) -> Unit = { delay(it) }
) {
    private val requests = Semaphore(2)

    suspend fun <T> run(request: suspend () -> T): T = requests.withPermit {
        executeWithRetries(request)
    }

    private suspend fun <T> executeWithRetries(request: suspend () -> T): T {
        var attempt = 0
        while (true) {
            try {
                return request()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                val status = (error as? HttpException)?.code()
                val temporary = error is IOException || status == 429 || status in 500..599
                if (!temporary || attempt >= 2) throw error
                val retryAfter = (error as? HttpException)?.response()?.headers()
                    ?.get("Retry-After")?.toLongOrNull()?.coerceIn(0, 10)?.times(1_000)
                pause(retryAfter ?: (600L shl attempt))
                attempt++
            }
        }
    }
}
