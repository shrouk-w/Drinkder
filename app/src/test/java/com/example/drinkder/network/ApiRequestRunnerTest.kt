package com.example.drinkder.network

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

internal fun httpFailure(status: Int) = HttpException(
    Response.error<Any>(status, ResponseBody.create(null, "Service error"))
)

class ApiRequestRunnerTest {
    @Test fun retriesTemporaryErrorsWithBackoff() = runBlocking {
        for (status in listOf(429, 500, 503)) {
            val pauses = mutableListOf<Long>()
            val runner = ApiRequestRunner { pauses += it }
            var calls = 0
            val result = runner.run {
                if (++calls < 3) throw httpFailure(status)
                "ok"
            }
            assertEquals("ok", result)
            assertEquals(listOf(600L, 1_200L), pauses)
        }
    }

    @Test fun doesNotRetryAuthorizationErrors() = runBlocking {
        var calls = 0
        val runner = ApiRequestRunner { fail("Should not wait for a permanent error") }
        try {
            runner.run { calls++; throw httpFailure(403) }
            fail("Expected HTTP error")
        } catch (error: HttpException) {
            assertEquals(403, error.code())
        }
        assertEquals(1, calls)
    }

    @Test fun cancellationIsNeverRetried() = runBlocking {
        var calls = 0
        try {
            ApiRequestRunner { fail("Should not retry cancellation") }.run {
                calls++
                throw CancellationException()
            }
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
        assertEquals(1, calls)
    }

    @Test fun boundsConcurrentRequests() = runBlocking {
        val active = AtomicInteger()
        val maximum = AtomicInteger()
        val runner = ApiRequestRunner()
        (1..10).map { async {
            runner.run {
                val count = active.incrementAndGet()
                maximum.updateAndGet { maxOf(it, count) }
                delay(1)
                active.decrementAndGet()
            }
        } }.awaitAll()
        assertEquals(2, maximum.get())
    }
}
