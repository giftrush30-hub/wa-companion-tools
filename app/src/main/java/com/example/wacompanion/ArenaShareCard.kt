package com.example.wacompanion

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID

/**
 * Creates a real, ready-to-share 4:5 visual card and shares it as an image.
 * Optional remote photo is embedded into the card (used by Meme War).
 */
object ArenaShareCard {
    fun share(
        activity: Activity,
        title: String,
        body: String,
        footer: String = "WA COMPANION  •  DAILY ARENA",
        imageUrl: String? = null,
        chooserTitle: String = "Share Daily Arena image"
    ) {
        Thread {
            try {
                val photo = imageUrl?.takeIf { it.startsWith("https://") }?.let { url ->
                    runCatching {
                        URL(url).openConnection().apply {
                            connectTimeout = 8000
                            readTimeout = 10000
                        }.getInputStream().use { BitmapFactory.decodeStream(it) }
                    }.getOrNull()
                }
                val bitmap = createCard(title, body, footer, photo)
                photo?.recycle()
                val directory = File(activity.cacheDir, "shared_memes").apply { mkdirs() }
                val file = File(directory, "arena_${UUID.randomUUID()}.png")
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
                val uri: Uri = FileProvider.getUriForFile(
                    activity, "${activity.packageName}.fileprovider", file
                )
                activity.runOnUiThread {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, footer)
                        clipData = ClipData.newUri(activity.contentResolver, "Daily Arena share", uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    activity.startActivity(Intent.createChooser(send, chooserTitle))
                }
            } catch (error: Exception) {
                activity.runOnUiThread {
                    android.widget.Toast.makeText(
                        activity,
                        "Could not prepare the share image. Please try again.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun createCard(title: String, body: String, footer: String, photo: Bitmap?): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = Paint(Paint.ANTI_ALIAS_FLAG)
        if (photo != null) {
            val scale = maxOf(width.toFloat() / photo.width, height.toFloat() / photo.height)
            val scaledWidth = photo.width * scale
            val scaledHeight = photo.height * scale
            val left = (width - scaledWidth) / 2f
            val top = (height - scaledHeight) / 2f
            canvas.drawBitmap(photo, null, RectF(left, top, left + scaledWidth, top + scaledHeight), background)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply {
                color = Color.argb(178, 4, 20, 26)
            })
        } else {
            background.shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(Color.rgb(5, 71, 58), Color.rgb(14, 39, 62), Color.rgb(33, 35, 69)),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)
        }

        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 205, 83)
            strokeWidth = 8f
        }
        canvas.drawRoundRect(RectF(64f, 68f, 310f, 82f), 7f, 7f, accent)

        val brand = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 222, 142)
            textSize = 34f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        canvas.drawText("WA COMPANION  /  DAILY ARENA", 66f, 145f, brand)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 76f
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            setShadowLayer(8f, 0f, 3f, Color.BLACK)
        }
        val titleLayout = StaticLayout(
            title.uppercase(), titlePaint, 930, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0f, false
        )
        canvas.save()
        canvas.translate(66f, 215f)
        titleLayout.draw(canvas)
        canvas.restore()

        val bodyTop = (300 + titleLayout.height + 35).toFloat().coerceAtMost(660f)
        val safeBody = body.take(420).let { if (body.length > 420) it + "…" else it }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setShadowLayer(5f, 0f, 2f, Color.BLACK)
        }
        var bodyLayout = StaticLayout(
            safeBody, bodyPaint, 930, Layout.Alignment.ALIGN_NORMAL, 1.22f, 0f, false
        )
        val availableBodyHeight = (1160f - bodyTop).toInt()
        while (bodyLayout.height > availableBodyHeight && bodyPaint.textSize > 28f) {
            bodyPaint.textSize -= 2f
            bodyLayout = StaticLayout(
                safeBody, bodyPaint, 930, Layout.Alignment.ALIGN_NORMAL, 1.18f, 0f, false
            )
        }
        canvas.save()
        canvas.translate(66f, bodyTop)
        bodyLayout.draw(canvas)
        canvas.restore()

        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 222, 142)
            textSize = 32f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        canvas.drawRoundRect(RectF(55f, 1205f, 1025f, 1295f), 26f, 26f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(190, 0, 0, 0) })
        canvas.drawText(footer.take(58), 82f, 1262f, footerPaint)
        return bitmap
    }
}
