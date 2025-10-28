package com.minifocus.launcher.manager

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InboxLogger(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val onRotation: (Long) -> Unit = {}
) {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mutex = Mutex()
    private val retentionDays = MutableStateFlow(DEFAULT_LOG_RETENTION_DAYS)
    private val logDir: File by lazy { context.getDir(LOG_DIR_NAME, Context.MODE_PRIVATE) }
    private val timestampFormat = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun updateRetention(days: Int) {
        retentionDays.value = days.coerceAtLeast(1)
    }

    fun log(event: String, message: String, metadata: Map<String, Any?> = emptyMap()) {
        scope.launch {
            mutex.withLock {
                val now = System.currentTimeMillis()
                val logFile = File(logDir, LOG_FILE_NAME)
                if (!logFile.exists()) {
                    logDir.mkdirs()
                    logFile.createNewFile()
                }
                rotateIfNeeded(logFile, now)
                val payload = buildJsonLine(now, event, message, metadata)
                FileOutputStream(logFile, true).use { stream ->
                    stream.write(payload.toByteArray(Charsets.UTF_8))
                    stream.write('\n'.code)
                }
            }
        }
    }

    private fun buildJsonLine(
        timestampMillis: Long,
        event: String,
        message: String,
        metadata: Map<String, Any?>
    ): String {
        val builder = StringBuilder()
        builder.append('{')
        builder.append("\"timestamp\":\"")
        builder.append(timestampFormat.format(Date(timestampMillis)))
        builder.append("\",")
        builder.append("\"event\":\"")
        builder.append(event)
        builder.append("\",")
        builder.append("\"message\":\"")
        builder.append(message.replace("\"", "\\\""))
        builder.append("\",")
        builder.append("\"metadata\":{")
        metadata.entries.joinToString(separator = ",") { (key, value) ->
            val escapedKey = key.replace("\"", "\\\"")
            val escapedValue = value?.toString()?.replace("\"", "\\\"") ?: "null"
            "\"$escapedKey\":\"$escapedValue\""
        }.let(builder::append)
        builder.append("}}");
        return builder.toString()
    }

    private fun rotateIfNeeded(file: File, now: Long) {
        val needsRotation = file.length() >= MAX_LOG_BYTES || now - file.lastModified() >= MAX_LOG_AGE_MS
        if (!needsRotation) {
            return
        }
        rotateFiles(file, now)
    }

    private fun rotateFiles(file: File, now: Long) {
        for (index in MAX_ARCHIVES downTo 1) {
            val current = File(logDir, "$LOG_FILE_NAME.$index.gz")
            val next = File(logDir, "$LOG_FILE_NAME.${index + 1}.gz")
            if (current.exists()) {
                if (index == MAX_ARCHIVES) {
                    current.delete()
                } else {
                    current.renameTo(next)
                }
            }
        }

        val archive = File(logDir, "$LOG_FILE_NAME.1.gz")
        FileOutputStream(archive).use { fos ->
            GZIPOutputStream(fos).use { gzip ->
                FileInputStream(file).use { input ->
                    input.copyTo(gzip)
                }
            }
        }
        archive.setLastModified(now)
        file.writeText("")
        purgeOldArchives(now)
        onRotation(now)
    }

    private fun purgeOldArchives(now: Long) {
        val retentionMillis = TimeUnit.DAYS.toMillis(retentionDays.value.toLong())
        logDir.listFiles()?.forEach { candidate ->
            if (candidate.name.startsWith(LOG_FILE_NAME) && candidate.name.endsWith(".gz")) {
                if (now - candidate.lastModified() > retentionMillis) {
                    candidate.delete()
                }
            }
        }
    }

    companion object {
        private const val LOG_DIR_NAME = "inbox-logs"
        private const val LOG_FILE_NAME = "inbox.log"
        private const val MAX_LOG_BYTES = 512 * 1024L
        private val MAX_LOG_AGE_MS = TimeUnit.HOURS.toMillis(24)
        private const val MAX_ARCHIVES = 5
        private const val DEFAULT_LOG_RETENTION_DAYS = 30
        private const val TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    }
}
