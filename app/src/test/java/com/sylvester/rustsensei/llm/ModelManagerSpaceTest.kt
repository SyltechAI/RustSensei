package com.sylvester.rustsensei.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A ~1.2 GB download on a nearly full device used to die mid-write with a raw
 * "No space left on device". These cover the pre-flight arithmetic that now
 * stops it before the first byte.
 */
class ModelManagerSpaceTest {

    private val oneGb = 1_000_000_000L

    @Test
    fun `fresh download needs the model size plus five percent`() {
        assertEquals(
            1_050_000_000L,
            ModelManager.requiredFreeBytes(expectedSizeBytes = oneGb, alreadyDownloadedBytes = 0)
        )
    }

    @Test
    fun `a partial download only needs the remainder`() {
        assertEquals(
            420_000_000L,
            ModelManager.requiredFreeBytes(
                expectedSizeBytes = oneGb,
                alreadyDownloadedBytes = 600_000_000L
            )
        )
    }

    @Test
    fun `a temp file larger than the advertised size needs nothing more`() {
        assertEquals(
            0L,
            ModelManager.requiredFreeBytes(
                expectedSizeBytes = oneGb,
                alreadyDownloadedBytes = oneGb + 1
            )
        )
    }

    @Test
    fun `space is insufficient when free bytes fall short`() {
        assertTrue(ModelManager.isInsufficientSpace(usableBytes = 900_000_000L, requiredBytes = oneGb))
    }

    @Test
    fun `exactly enough space is sufficient`() {
        assertFalse(ModelManager.isInsufficientSpace(usableBytes = oneGb, requiredBytes = oneGb))
    }

    @Test
    fun `an unreadable free space reading does not block the download`() {
        // usableSpace() returning -1 must not be read as "zero bytes free".
        assertFalse(ModelManager.isInsufficientSpace(usableBytes = -1L, requiredBytes = oneGb))
    }

    @Test
    fun `a full device is insufficient`() {
        assertTrue(ModelManager.isInsufficientSpace(usableBytes = 0L, requiredBytes = 1L))
    }
}
