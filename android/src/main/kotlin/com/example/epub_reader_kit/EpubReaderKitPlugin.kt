package com.example.epub_reader_kit

import android.app.Activity
import android.net.Uri
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.example.epub_reader_kit.reader.EpubReaderKitApp
import com.example.epub_reader_kit.reader.data.db.AppDatabase
import com.example.epub_reader_kit.reader.domain.Bookshelf
import com.example.epub_reader_kit.reader.reader.ReaderActivityContract
import org.readium.r2.shared.util.toAbsoluteUrl
import java.io.File

class EpubReaderKitPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    private var channel: MethodChannel? = null
    private var activity: Activity? = null
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME).also {
            it.setMethodCallHandler(this)
        }
        flutterChannel = channel
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "openBook" -> {
                val bookPath = call.argument<String>("bookPath")
                val sourceKey = call.argument<String>("sourceKey")
                if (bookPath.isNullOrBlank()) {
                    result.error("INVALID_ARGUMENT", "bookPath is required", null)
                    return
                }
                openBook(bookPath, sourceKey ?: "local:$bookPath", result)
            }

            "openBookFromUrl" -> {
                val epubUrl = call.argument<String>("epubUrl")
                val sourceKey = call.argument<String>("sourceKey")
                if (epubUrl.isNullOrBlank()) {
                    result.error("INVALID_ARGUMENT", "epubUrl is required", null)
                    return
                }
                openBookFromUrl(epubUrl, sourceKey ?: "remote:$epubUrl", result)
            }

            else -> result.notImplemented()
        }
    }

    private fun openBook(bookPath: String, sourceKey: String, result: MethodChannel.Result) {
        val hostActivity = activity
        if (hostActivity == null) {
            result.error("NO_ACTIVITY", "Plugin requires foreground activity", null)
            return
        }

        scope.launch {
            try {
                val sourceFile = File(bookPath)
                if (!sourceFile.exists()) {
                    result.error("FILE_NOT_FOUND", "EPUB file not found at path", bookPath)
                    return@launch
                }

                val app = hostActivity.application as? EpubReaderKitApp
                if (app == null) {
                    result.error(
                        "INVALID_APPLICATION",
                        "Application must be com.example.epub_reader_kit.reader.EpubReaderKitApp for native reader runtime.",
                        hostActivity.application.javaClass.name
                    )
                    return@launch
                }

                val existingBookId = findBookIdByIdentifier(hostActivity, sourceKey)
                if (existingBookId != null) {
                    openReaderById(hostActivity, app, existingBookId, result)
                    return@launch
                }

                app.bookshelf.importPublicationFromStorage(
                    uri = Uri.fromFile(sourceFile),
                    wdIdentifier = sourceKey,
                )
                handleImportAndOpen(hostActivity, app, result)
            } catch (e: Exception) {
                result.error("OPEN_BOOK_FAILED", "Failed to import/open local book", e.message)
            }
        }
    }

    private fun openBookFromUrl(epubUrl: String, sourceKey: String, result: MethodChannel.Result) {
        val hostActivity = activity
        if (hostActivity == null) {
            result.error("NO_ACTIVITY", "Plugin requires foreground activity", null)
            return
        }

        scope.launch {
            try {
                val app = hostActivity.application as? EpubReaderKitApp
                if (app == null) {
                    result.error(
                        "INVALID_APPLICATION",
                        "Application must be com.example.epub_reader_kit.reader.EpubReaderKitApp for native reader runtime.",
                        hostActivity.application.javaClass.name
                    )
                    return@launch
                }

                val existingBookId = findBookIdByIdentifier(hostActivity, sourceKey)
                if (existingBookId != null) {
                    openReaderById(hostActivity, app, existingBookId, result)
                    return@launch
                }

                val absoluteUrl = Uri.parse(epubUrl).toAbsoluteUrl()
                if (absoluteUrl == null) {
                    result.error("INVALID_URL", "Invalid URL format", epubUrl)
                    return@launch
                }

                app.bookshelf.importPublicationFromHttp(
                    url = absoluteUrl,
                    wdIdentifier = sourceKey,
                )
                handleImportAndOpen(hostActivity, app, result)
            } catch (e: Exception) {
                result.error("OPEN_BOOK_URL_FAILED", "Failed to import/open remote book", e.message)
            }
        }
    }

    private suspend fun handleImportAndOpen(
        hostActivity: Activity,
        app: EpubReaderKitApp,
        result: MethodChannel.Result,
    ) {
        val event = withTimeoutOrNull(180000) {
            app.bookshelf.channel.receive()
        }

        when (event) {
            is Bookshelf.Event.ImportPublicationSuccess -> {
                val latestBookId = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(hostActivity.applicationContext)
                        .booksDao()
                        .getLatestBookId()
                }

                if (latestBookId == null) {
                    result.error("OPEN_FAILED", "Book imported but no database id found", null)
                    return
                }

                openReaderById(hostActivity, app, latestBookId, result)
            }

            is Bookshelf.Event.ImportPublicationError -> {
                result.error("IMPORT_FAILED", "Failed to import publication", event.error.message)
            }

            null -> {
                result.error("TIMEOUT", "Import timed out", null)
            }
        }
    }

    private suspend fun findBookIdByIdentifier(hostActivity: Activity, sourceKey: String): Long? {
        return withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(hostActivity.applicationContext)
                .booksDao()
                .getBookIdByIdentifier(sourceKey)
        }
    }

    private suspend fun openReaderById(
        hostActivity: Activity,
        app: EpubReaderKitApp,
        bookId: Long,
        result: MethodChannel.Result,
    ) {
        app.readerRepository
            .open(bookId)
            .onSuccess {
                val intent = ReaderActivityContract().createIntent(
                    hostActivity,
                    ReaderActivityContract.Arguments(bookId)
                )
                hostActivity.startActivity(intent)
                result.success(true)
            }
            .onFailure {
                result.error("OPEN_FAILED", "Failed to open reader", it.message)
            }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        channel = null
        flutterChannel = null
        scope.cancel()
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    companion object {
        private const val CHANNEL_NAME = "com.example.epub_reader_kit/reader"
        private val mainHandler = Handler(Looper.getMainLooper())

        @Volatile
        private var flutterChannel: MethodChannel? = null

        @JvmStatic
        fun emitEpubPageChanged(percentage: Int) {
            val bounded = percentage.coerceIn(0, 100)
            mainHandler.post {
                flutterChannel?.invokeMethod(
                    "onEpubPageChanged",
                    mapOf("percentage" to bounded)
                )
            }
        }
    }
}
