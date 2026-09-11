package com.sylvester.rustsensei.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OkHttpPlaygroundService @Inject constructor() : RustPlaygroundService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun execute(request: PlaygroundRequest): PlaygroundResponse =
        withContext(Dispatchers.IO) {
            val jsonBody = JSONObject().apply {
                put("channel", request.channel)
                put("mode", request.mode)
                put("edition", request.edition)
                put("crateType", request.crateType)
                put("tests", request.tests)
                put("code", request.code)
                put("backtrace", request.backtrace)
            }

            val httpRequest = Request.Builder()
                .url(PLAYGROUND_URL)
                .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            // use{} matters here: returning or throwing without closing the
            // response leaks the connection out of OkHttp's pool.
            val body = client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    throw PlaygroundApiException("Playground API error: HTTP ${response.code}")
                }
                response.body?.string()
                    ?: throw PlaygroundApiException("Empty response from Playground API")
            }

            parseResponse(body)
        }

    companion object {
        private const val PLAYGROUND_URL = "https://play.rust-lang.org/execute"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        /**
         * Maps a Playground reply body onto [PlaygroundResponse].
         *
         * Deliberately tolerant: the service can answer with an HTML error page
         * behind a captive portal, or a JSON error envelope during an outage.
         * getBoolean("success") turned both of those into a bare
         * "No value for success" surfaced to the user as the compile result.
         */
        fun parseResponse(body: String): PlaygroundResponse {
            val json = try {
                JSONObject(body)
            } catch (e: Exception) {
                throw PlaygroundApiException("Unexpected response from the Rust Playground", e)
            }
            return PlaygroundResponse(
                success = json.optBoolean("success", false),
                stdout = json.optString("stdout", ""),
                stderr = json.optString("stderr", "").ifEmpty { json.optString("error", "") }
            )
        }
    }
}
