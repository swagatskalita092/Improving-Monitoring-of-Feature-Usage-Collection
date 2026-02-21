/*
 * Author: Swagat Subhash Kalita
 * Date: 2026-02-20
 */

package downloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

class ParallelFileDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val chunkSizeBytes: Int = 512 * 1024,
    private val maxParallel: Int = 4,
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 100
) {
    data class DownloadResult(val success: Boolean, val bytesWritten: Long, val message: String?)

    fun download(url: String, outputFile: File): DownloadResult = runBlocking {
        withContext(Dispatchers.IO) {
            runDownload(url, outputFile)
        }
    }

    private suspend fun runDownload(url: String, outputFile: File): DownloadResult {
        client.newCall(Request.Builder().url(url).head().build()).execute().use { headResponse ->
            if (!headResponse.isSuccessful) {
                return DownloadResult(false, 0, "HEAD failed: ${headResponse.code}")
            }
            val contentLength = headResponse.header("Content-Length")?.toLongOrNull()
            val acceptRanges = headResponse.header("Accept-Ranges") ?: ""
            val supportsRange = acceptRanges.equals("bytes", ignoreCase = true)

            return when {
                contentLength == null -> downloadSingle(url, outputFile, expectedLength = null)
                supportsRange && contentLength > 0 -> downloadChunked(url, outputFile, contentLength)
                else -> downloadSingle(url, outputFile, expectedLength = contentLength)
            }
        }
    }

    private suspend fun downloadChunked(url: String, outputFile: File, contentLength: Long): DownloadResult = coroutineScope {
        outputFile.parentFile?.mkdirs()
        val raf = RandomAccessFile(outputFile, "rw")
        try {
            raf.setLength(contentLength)
            val chunks = buildChunks(contentLength)
            chunks.chunked(maxParallel).forEach { batch ->
                batch.map { chunk ->
                    async {
                        downloadChunkWithRetry(url, chunk.first, chunk.second, raf)
                    }
                }.awaitAll()
            }
            val result = if (raf.length() != contentLength) {
                DownloadResult(false, raf.length(), "Size mismatch: expected $contentLength, got ${raf.length()}")
            } else {
                DownloadResult(true, contentLength, null)
            }
            result
        } finally {
            raf.close()
        }
    }

    /** Chunks as (start, end) inclusive; last chunk ends at contentLength - 1. */
    private fun buildChunks(contentLength: Long): List<Pair<Long, Long>> {
        val chunks = mutableListOf<Pair<Long, Long>>()
        var start = 0L
        while (start < contentLength) {
            val end = (start + chunkSizeBytes - 1).coerceAtMost(contentLength - 1)
            chunks.add(start to end)
            start = end + 1
        }
        return chunks
    }

    private fun downloadChunkWithRetry(url: String, start: Long, end: Long, raf: RandomAccessFile) {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Range", "bytes=$start-$end")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) {
                        throw RuntimeException("Chunk request failed: ${response.code}")
                    }
                    val data = response.body?.byteStream()?.readBytes() ?: throw RuntimeException("Empty body")
                    synchronized(raf) {
                        raf.seek(start)
                        raf.write(data)
                    }
                }
                return
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    Thread.sleep(retryDelayMs * (attempt + 1))
                }
            }
        }
        throw lastException ?: RuntimeException("Chunk download failed")
    }

    private suspend fun downloadSingle(url: String, outputFile: File, expectedLength: Long?): DownloadResult {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) {
                return DownloadResult(false, 0, "GET failed: ${response.code}")
            }
            val body = response.body ?: return DownloadResult(false, 0, "No response body")
            outputFile.parentFile?.mkdirs()
            body.byteStream().use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        val actual = outputFile.length()
        return when {
            expectedLength != null && actual != expectedLength ->
                DownloadResult(false, actual, "Size mismatch: expected $expectedLength, got $actual")
            else -> DownloadResult(true, actual, null)
        }
    }
}
