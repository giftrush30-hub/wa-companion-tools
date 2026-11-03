package com.example.wacompanion

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

class NaijaStickerProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "com.example.wacompanion.stickers"
        const val PACK_ID = "naija_reactions_01"
        private const val PACK_NAME = "Naija Reactions"
        private const val PUBLISHER = "WA Companion"
        private const val TRAY = "tray.png"
        private val phrases = listOf(
            Triple("Omo! Wetin be this?", "😳", "Surprised Nigerian reaction"),
            Triple("I no fit shout!", "😂", "Funny speechless reaction"),
            Triple("Abeg o!", "🙏", "Please reaction"),
            Triple("Na wa o!", "😮", "Amazed reaction"),
            Triple("God abeg!", "🙏", "Prayerful reaction"),
            Triple("E choke!", "🔥", "Excited reaction")
        )
        private const val META = 1
        private const val SINGLE_META = 2
        private const val STICKERS = 3
        private const val ASSET = 4
    }

    private val matcher = UriMatcher(UriMatcher.NO_MATCH)
    private lateinit var files: File

    override fun onCreate(): Boolean {
        matcher.addURI(AUTHORITY, "metadata", META)
        matcher.addURI(AUTHORITY, "metadata/*", SINGLE_META)
        matcher.addURI(AUTHORITY, "stickers/*", STICKERS)
        matcher.addURI(AUTHORITY, "stickers_asset/$PACK_ID/*", ASSET)
        files = File(requireNotNull(context).cacheDir, "naija_sticker_pack").apply { mkdirs() }
        createStickerFiles()
        return true
    }

    private fun createStickerFiles() {
        if (File(files, TRAY).exists() && phrases.all { File(files, fileName(phrases.indexOf(it))).exists() }) return
        phrases.forEachIndexed { index, item ->
            val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                textSize = 65f
                strokeJoin = Paint.Join.ROUND
                strokeWidth = 13f
                style = Paint.Style.STROKE
                color = android.graphics.Color.WHITE
            }
            val words = item.first.split(" ")
            val lines = mutableListOf<String>()
            var line = ""
            words.forEach { word ->
                val next = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(next) > 430f && line.isNotEmpty()) { lines.add(line); line = word } else line = next
            }
            if (line.isNotEmpty()) lines.add(line)
            val startY = 250f - (lines.size - 1) * 43f
            lines.forEachIndexed { lineIndex, text ->
                val y = startY + lineIndex * 88f
                canvas.drawText(text, 256f, y, paint)
                paint.style = Paint.Style.FILL
                paint.color = intArrayOf(0xff075e45.toInt(), 0xff8b2445.toInt(), 0xff244c86.toInt())[index % 3]
                canvas.drawText(text, 256f, y, paint)
                paint.style = Paint.Style.STROKE
                paint.color = android.graphics.Color.WHITE
            }
            FileOutputStream(File(files, fileName(index))).use {
                bitmap.compress(Bitmap.CompressFormat.WEBP, 88, it)
            }
            bitmap.recycle()
        }
        val tray = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(tray)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xff075e45.toInt() }
        canvas.drawCircle(48f, 48f, 46f, p)
        p.color = android.graphics.Color.WHITE
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 55f
        canvas.drawText("N", 48f, 67f, p)
        FileOutputStream(File(files, TRAY)).use { tray.compress(Bitmap.CompressFormat.PNG, 100, it) }
        tray.recycle()
    }

    private fun fileName(index: Int) = "reaction_$index.webp"

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        return when (matcher.match(uri)) {
            META, SINGLE_META -> MatrixCursor(arrayOf(
                "sticker_pack_identifier", "sticker_pack_name", "sticker_pack_publisher",
                "sticker_pack_icon", "android_play_store_link", "ios_app_download_link",
                "sticker_pack_publisher_email", "sticker_pack_publisher_website",
                "sticker_pack_privacy_policy_website", "sticker_pack_license_agreement_website",
                "image_data_version", "whatsapp_will_not_cache_stickers", "animated_sticker_pack"
            )).apply {
                addRow(arrayOf(PACK_ID, PACK_NAME, PUBLISHER, TRAY, "", "", "", "", "", "", "1", 0, 0))
            }
            STICKERS -> MatrixCursor(arrayOf("sticker_file_name", "sticker_emoji", "sticker_accessibility_text")).apply {
                phrases.forEachIndexed { i, item -> addRow(arrayOf(fileName(i), item.second, item.third)) }
            }
            else -> throw IllegalArgumentException("Unknown sticker URI: $uri")
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        require(mode == "r") { "Read-only sticker assets" }
        val name = uri.lastPathSegment ?: throw IllegalArgumentException("Missing sticker filename")
        require(name == TRAY || phrases.indices.any { name == fileName(it) }) { "Unknown sticker file" }
        val file = File(files, name)
        require(file.isFile) { "Sticker file unavailable" }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = when (matcher.match(uri)) {
        META -> "vnd.android.cursor.dir/vnd.$AUTHORITY.metadata"
        SINGLE_META -> "vnd.android.cursor.item/vnd.$AUTHORITY.metadata"
        STICKERS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.stickers"
        ASSET -> if (uri.lastPathSegment == TRAY) "image/png" else "image/webp"
        else -> throw IllegalArgumentException("Unknown sticker URI: $uri")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
}
