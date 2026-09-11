package com.sylvester.rustsensei.llm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(
        val progress: Float,
        val downloadedMB: Long,
        val totalMB: Long,
        val speedMBps: Float = 0f,
        val estimatedSecondsLeft: Long = 0
    ) : DownloadState()
    data object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}

data class ModelInfo(
    val id: String,
    val displayName: String,
    val parameterSize: String,
    val filename: String,
    val downloadUrl: String,
    val expectedSizeBytes: Long,
    val description: String,
    val ramRequired: String,
    val minDeviceMemoryGb: Float = 0f,
    val sha256: String = ""
)

class ModelManager(private val context: Context) {

    companion object {
        private const val MODEL_DIR = "models"
        private const val HTTP_RANGE_NOT_SATISFIABLE = 416

        /** Slack on top of the advertised model size, as a percentage. */
        private const val HEADROOM_PERCENT = 5

        /**
         * ENOSPC surfaces as a plain IOException whose message varies by device,
         * so match on the message rather than on a type.
         */
        private fun isOutOfSpace(e: Throwable): Boolean {
            var cause: Throwable? = e
            while (cause != null) {
                val msg = cause.message?.lowercase().orEmpty()
                if ("enospc" in msg || "no space left" in msg) return true
                cause = cause.cause
            }
            return false
        }

        val AVAILABLE_MODELS = listOf(
            ModelInfo(
                id = "litert-1b-gemma",
                displayName = "Rust Mentor 1B",
                parameterSize = "1B",
                filename = "rust-mentor-1b-mobile_q8_ekv2053.litertlm",
                downloadUrl = "https://huggingface.co/sylvester-francis/rust-mentor-1b-mobile-LiteRT/resolve/main/rust-mentor-1b-mobile_q8_ekv2053.litertlm",
                expectedSizeBytes = 1_200_000_000L,
                description = "Best balance of speed and quality. Recommended.",
                ramRequired = "~3 GB RAM",
                minDeviceMemoryGb = 3f
            )
            // NOTE: 0.6B and 1.7B models removed — they have ops that can't fully
            // delegate to GPU (1203/1255 nodes), causing LiteRT's LLM engine to fail
            // with INTERNAL error at llm_litert_compiled_model_executor.cc:2143.
            // Re-add once the models are re-converted with full GPU-compatible ops.
        )

        fun getModelById(id: String): ModelInfo? = AVAILABLE_MODELS.find { it.id == id }

        /**
         * Bytes still needed to finish a download of [expectedSizeBytes] given
         * [alreadyDownloadedBytes] already on disk, plus [HEADROOM_PERCENT] slack
         * so the model being slightly larger than advertised does not wedge the
         * download at 99 percent.
         */
        fun requiredFreeBytes(expectedSizeBytes: Long, alreadyDownloadedBytes: Long): Long {
            val remaining = (expectedSizeBytes - alreadyDownloadedBytes).coerceAtLeast(0L)
            return remaining / 100 * HEADROOM_PERCENT + remaining
        }

        /**
         * A negative [usableBytes] means the free space could not be read; treat
         * that as "go ahead and try" rather than blocking the download on a
         * failed stat call.
         */
        fun isInsufficientSpace(usableBytes: Long, requiredBytes: Long): Boolean =
            usableBytes in 0 until requiredBytes
    }

    private val modelsDir: File
        get() = File(context.filesDir, MODEL_DIR).also { it.mkdirs() }

    fun getModelFile(modelInfo: ModelInfo): File = File(modelsDir, modelInfo.filename)

    private fun getTempFile(modelInfo: ModelInfo): File = File(modelsDir, "${modelInfo.filename}.tmp")

    // Legacy accessors for backward compatibility
    val modelFile: File
        get() = File(modelsDir, AVAILABLE_MODELS[0].filename)

    fun isModelDownloaded(modelInfo: ModelInfo): Boolean {
        val file = getModelFile(modelInfo)
        return file.exists() && file.length() > 1_000_000
    }

    /** Bug 12: Check if a partial .tmp file exists from an interrupted download. */
    fun hasTempFile(modelInfo: ModelInfo): Boolean {
        val tempFile = getTempFile(modelInfo)
        return tempFile.exists() && tempFile.length() > 0
    }

    fun isModelDownloaded(): Boolean = isModelDownloaded(AVAILABLE_MODELS[0])

    fun getModelSizeMB(modelInfo: ModelInfo): Long {
        val file = getModelFile(modelInfo)
        return if (file.exists()) file.length() / (1024 * 1024) else 0
    }

    fun getModelSizeMB(): Long = getModelSizeMB(AVAILABLE_MODELS[0])

    fun getDownloadedModels(): List<ModelInfo> {
        return AVAILABLE_MODELS.filter { isModelDownloaded(it) }
    }

    fun deleteModel(modelInfo: ModelInfo): Boolean {
        getTempFile(modelInfo).delete()
        return getModelFile(modelInfo).delete()
    }

    fun deleteModel(): Boolean = deleteModel(AVAILABLE_MODELS[0])

    /**
     * Deletes partial downloads that no longer belong to any available model.
     *
     * Models get retired (the 0.6B and 1.7B builds were), and their .tmp files
     * are then unreachable: nothing can resume them and no UI offers to delete
     * them, so up to ~1.2 GB stays stranded in app storage forever. Temp files
     * for models that are still listed are kept — those are resumable.
     */
    fun cleanupOrphanedTempFiles() {
        val resumable = AVAILABLE_MODELS.map { "${it.filename}.tmp" }.toSet()
        modelsDir.listFiles()
            ?.filter { it.name.endsWith(".tmp") && it.name !in resumable }
            ?.forEach { tmpFile ->
                Log.i(
                    "ModelManager",
                    "Removing stranded temp file: ${tmpFile.name} (${tmpFile.length() / (1024 * 1024)} MB)"
                )
                tmpFile.delete()
            }
    }

    /**
     * Free bytes usable by this app in the data partition, or -1 if unknown.
     * Uses usableSpace (quota-aware) rather than freeSpace.
     */
    fun usableSpaceBytes(): Long = try {
        modelsDir.usableSpace
    } catch (e: Exception) {
        Log.w("ModelManager", "Could not read free space: ${e.message}")
        -1L
    }

    /**
     * Bytes still needed for [modelInfo], accounting for an existing partial
     * download.
     */
    fun requiredFreeBytes(modelInfo: ModelInfo): Long {
        val alreadyHave = getTempFile(modelInfo).let { if (it.exists()) it.length() else 0L }
        return requiredFreeBytes(modelInfo.expectedSizeBytes, alreadyHave)
    }

    /** True when there is demonstrably not enough room to finish the download. */
    fun hasInsufficientSpace(modelInfo: ModelInfo): Boolean =
        isInsufficientSpace(usableSpaceBytes(), requiredFreeBytes(modelInfo))

    fun downloadModel(modelInfo: ModelInfo): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0f, 0, 0))

        // Runs inside ModelDownloadService (a dataSync foreground service), which
        // keeps the process alive for the ~1.2 GB download — no app-managed
        // PARTIAL_WAKE_LOCK is held here (see the Play battery/wake-lock vital).
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()

            val tempFile = getTempFile(modelInfo)
            val finalFile = getModelFile(modelInfo)

            // Fail fast on a full device. Without this the write dies ~900 MB in
            // with a raw "No space left on device" and the user has no idea how
            // much room to clear.
            if (hasInsufficientSpace(modelInfo)) {
                val neededMB = requiredFreeBytes(modelInfo) / (1024 * 1024)
                val freeMB = usableSpaceBytes() / (1024 * 1024)
                emit(DownloadState.Error(
                    "Not enough storage. ${modelInfo.displayName} needs about $neededMB MB " +
                        "free and this device has $freeMB MB. Free up some space and try again."
                ))
                return@flow
            }

            var existingBytes = 0L
            if (tempFile.exists()) {
                existingBytes = tempFile.length()
            }

            fun openResponse(rangeFrom: Long) = client.newCall(
                Request.Builder().url(modelInfo.downloadUrl).apply {
                    if (rangeFrom > 0) addHeader("Range", "bytes=$rangeFrom-")
                }.build()
            ).execute()

            var response = openResponse(existingBytes)

            // 416 means the partial file is at or past the end of the resource -
            // typically a finished .tmp that never got renamed, or a file the
            // server has since replaced. Retrying the same range forever leaves
            // the download permanently stuck, so discard it and restart clean.
            if (response.code == HTTP_RANGE_NOT_SATISFIABLE && existingBytes > 0) {
                Log.w("ModelManager", "Server rejected resume range; restarting download from scratch")
                response.close()
                tempFile.delete()
                existingBytes = 0
                response = openResponse(0)
            }

            if (!response.isSuccessful && response.code != 206) {
                val code = response.code
                response.close()
                emit(DownloadState.Error("Download failed: HTTP $code"))
                return@flow
            }

            val body = response.body ?: run {
                response.close()
                emit(DownloadState.Error("Empty response body"))
                return@flow
            }

            val isResuming = response.code == 206 && existingBytes > 0
            if (!isResuming) {
                existingBytes = 0
                tempFile.delete()
            }

            val contentLength = body.contentLength().let {
                if (it > 0) it + existingBytes else modelInfo.expectedSizeBytes
            }
            val totalMB = contentLength / (1024 * 1024)

            val downloadStartTime = System.currentTimeMillis()

            body.byteStream().use { input ->
                FileOutputStream(tempFile, isResuming).use { output ->
                    val buffer = ByteArray(32768)
                    var bytesRead = existingBytes
                    val bytesAtStart = existingBytes
                    var lastEmitTime = 0L

                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        bytesRead += read

                        val now = System.currentTimeMillis()
                        if (now - lastEmitTime > 500) {
                            val progress = bytesRead.toFloat() / contentLength
                            val downloadedMB = bytesRead / (1024 * 1024)

                            // Calculate speed and ETA
                            val elapsedMs = now - downloadStartTime
                            val bytesDownloadedThisSession = bytesRead - bytesAtStart
                            val speedBps = if (elapsedMs > 0) (bytesDownloadedThisSession * 1000.0 / elapsedMs) else 0.0
                            val speedMBps = (speedBps / (1024 * 1024)).toFloat()
                            val remainingBytes = contentLength - bytesRead
                            val estimatedSecondsLeft = if (speedBps > 0) (remainingBytes / speedBps).toLong() else 0

                            emit(DownloadState.Downloading(progress, downloadedMB, totalMB, speedMBps, estimatedSecondsLeft))
                            lastEmitTime = now
                        }
                    }
                }
            }

            if (tempFile.length() < 1_000_000) {
                tempFile.delete()
                emit(DownloadState.Error("Download appears incomplete (${tempFile.length()} bytes)"))
                return@flow
            }

            // Verify SHA256 integrity if checksum is provided
            if (modelInfo.sha256.isNotEmpty()) {
                val actualHash = sha256(tempFile)
                if (!actualHash.equals(modelInfo.sha256, ignoreCase = true)) {
                    tempFile.delete()
                    emit(DownloadState.Error(
                        "Integrity check failed. Expected SHA256: ${modelInfo.sha256.take(12)}... " +
                        "Got: ${actualHash.take(12)}... The file may be corrupted."
                    ))
                    return@flow
                }
                Log.i("ModelManager", "SHA256 verified: ${actualHash.take(16)}...")
            }

            // A failed rename used to report success and leave the app looking for
            // a model file that was never there. Fall back to a copy, and only then
            // give up.
            finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                Log.w("ModelManager", "Rename failed, copying ${tempFile.name} into place")
                val copied = try {
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                    true
                } catch (e: Exception) {
                    Log.e("ModelManager", "Copy fallback failed: ${e.message}", e)
                    false
                }
                if (!copied || !finalFile.exists()) {
                    emit(DownloadState.Error(
                        "Downloaded the model but could not save it. Free up storage and try again."
                    ))
                    return@flow
                }
            }
            emit(DownloadState.Completed)

        } catch (e: CancellationException) {
            // User cancelled. Keep the partial file so the next attempt resumes,
            // and let the cancellation propagate instead of reporting an error.
            Log.i("ModelManager", "Download cancelled, keeping partial file for resume")
            throw e
        } catch (e: Exception) {
            // Clean up partial download on non-resumable errors
            val tempFile = getTempFile(modelInfo)
            val message = when {
                e is java.net.UnknownHostException || e is java.net.ConnectException -> {
                    // Network errors: keep temp file for resume
                    Log.w("ModelManager", "Network error, keeping temp file for resume: ${e.message}")
                    "No internet connection. The download will resume where it left off."
                }
                e is java.net.SocketTimeoutException -> {
                    Log.w("ModelManager", "Timeout, keeping temp file for resume")
                    "The connection timed out. Tap Resume to continue where it left off."
                }
                isOutOfSpace(e) -> {
                    // Keep the partial file: the user can free space and resume.
                    Log.w("ModelManager", "Out of storage during download")
                    "Ran out of storage while downloading. Free up space and tap Resume."
                }
                else -> {
                    // Other errors: clean up to avoid orphaned files
                    tempFile.delete()
                    e.message ?: "Unknown download error"
                }
            }
            emit(DownloadState.Error(message))
        }
    }.flowOn(Dispatchers.IO)

    // Legacy: download default model
    fun downloadModel(): Flow<DownloadState> = downloadModel(AVAILABLE_MODELS[0])

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
