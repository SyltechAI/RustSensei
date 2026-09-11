package com.sylvester.rustsensei.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Playground reply was parsed with getBoolean("success"), which throws on
 * any envelope that lacks the field: outage responses, captive-portal HTML, or
 * a future schema change. The user saw "No value for success" as their compiler
 * output.
 */
class PlaygroundResponseParsingTest {

    @Test
    fun `successful compile maps stdout and stderr`() {
        val parsed = OkHttpPlaygroundService.parseResponse(
            """{"success":true,"stdout":"Hello, world!\n","stderr":"   Compiling playground"}"""
        )
        assertTrue(parsed.success)
        assertEquals("Hello, world!\n", parsed.stdout)
        assertEquals("   Compiling playground", parsed.stderr)
    }

    @Test
    fun `failed compile keeps the rustc diagnostics`() {
        val parsed = OkHttpPlaygroundService.parseResponse(
            """{"success":false,"stdout":"","stderr":"error[E0308]: mismatched types"}"""
        )
        assertFalse(parsed.success)
        assertEquals("error[E0308]: mismatched types", parsed.stderr)
    }

    @Test
    fun `an envelope without success reads as a failed compile`() {
        val parsed = OkHttpPlaygroundService.parseResponse("""{"stdout":"","stderr":""}""")
        assertFalse(parsed.success)
    }

    @Test
    fun `an error envelope surfaces its error field as stderr`() {
        val parsed = OkHttpPlaygroundService.parseResponse("""{"error":"Sandbox is overloaded"}""")
        assertFalse(parsed.success)
        assertEquals("Sandbox is overloaded", parsed.stderr)
    }

    @Test
    fun `a real stderr is preferred over the error field`() {
        val parsed = OkHttpPlaygroundService.parseResponse(
            """{"success":false,"stderr":"error[E0425]","error":"ignored"}"""
        )
        assertEquals("error[E0425]", parsed.stderr)
    }

    @Test
    fun `a non-JSON body raises a typed API error`() {
        assertThrows(PlaygroundApiException::class.java) {
            OkHttpPlaygroundService.parseResponse("<html><body>502 Bad Gateway</body></html>")
        }
    }

    @Test
    fun `missing fields default to empty strings rather than throwing`() {
        val parsed = OkHttpPlaygroundService.parseResponse("""{"success":true}""")
        assertTrue(parsed.success)
        assertEquals("", parsed.stdout)
        assertEquals("", parsed.stderr)
    }
}
