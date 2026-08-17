package com.payda.iptv.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uRepositoryTest {
    @Test
    fun returnsFailureWhenUrlDoesNotRespond() = runBlocking {
        val result = runCatching {
            M3uRepository().loadChannels("http://127.0.0.1:9/not-found.m3u")
        }

        assertTrue(result.isFailure)
    }
}
