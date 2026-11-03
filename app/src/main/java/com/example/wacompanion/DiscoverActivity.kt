package com.example.wacompanion

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * First WA Discover milestone.
 *
 * This is deliberately an honest marketplace shell: no fake listings or invented
 * view counts are shown. Live browsing and seller publishing will be enabled after
 * the shared backend and its security policies are configured.
 */
class DiscoverActivity : Activity() {
    private val green = Color.rgb(6, 63, 50)
    private val muted = Color.rgb(93, 105, 100)
    private val pale = Color.rgb(238, 245, 241)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 249, 247))
            setPadding(dp(18), dp(20), dp(18), dp(24))
        }
        val scroll = ScrollView(this).apply { addView(page) }

        page.addView(text("WA DISCOVER", 25, green, true))
        page.addView(text("Your business deserves to be discovered.", 15, muted))
        page.addView(space(18))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(18))
            setBackgroundColor(green)
        }
        hero.addView(text("Discover Nigerian businesses", 20, Color.WHITE, true))
        hero.addView(text("Find products, services and local sellers from across Nigeria — not only people already in your contacts.", 14, Color.WHITE))
        hero.addView(space(12))
        hero.addView(text("LAUNCH PLAN  •  NIGERIA", 11, Color.rgb(205, 235, 219), true))
        page.addView(hero)
        page.addView(space(16))

        val search = EditText(this).apply {
            hint = "Search products, shops and services"
            textSize = 15f
            isSingleLine = true
            setPadding(dp(13), dp(12), dp(13), dp(12))
            setBackgroundColor(Color.WHITE)
            isFocusable = false
            isClickable = true
            setOnClickListener { showBackendNotice() }
        }
        page.addView(search)
        page.addView(space(14))

        page.addView(text("EXPLORE CATEGORIES", 13, green, true))
        val categories = listOf("Fashion", "Food", "Phones", "Beauty", "Electronics", "Home", "Farming", "Services", "Other")
        val categoryScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val categoryRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        categories.forEach { name ->
            val chip = TextView(this).apply {
                text = name
                textSize = 13f
                setTextColor(green)
                setPadding(dp(13), dp(9), dp(13), dp(9))
                setBackgroundColor(Color.WHITE)
                setOnClickListener { showBackendNotice() }
            }
            val params = LinearLayout.LayoutParams(-2, -2).apply {
                rightMargin = dp(8)
                topMargin = dp(8)
                bottomMargin = dp(8)
            }
            categoryRow.addView(chip, params)
        }
        categoryScroll.addView(categoryRow)
        page.addView(categoryScroll)
        page.addView(space(10))

        val status = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(Color.WHITE)
        }
        status.addView(text("The marketplace is being connected", 18, green, true))
        status.addView(space(7))
        status.addView(text("We are preparing the shared online catalogue and seller accounts. Real businesses and products will appear here after the secure backend is connected and listings are approved.", 14, muted))
        status.addView(space(12))
        status.addView(text("No sample sellers, fake products or made-up view counts are displayed.", 12, muted, true))
        page.addView(status)
        page.addView(space(16))

        val sellerButton = Button(this).apply {
            text = "I'm a seller — prepare my business page"
            isAllCaps = false
            setOnClickListener { showSellerNotice() }
        }
        page.addView(sellerButton)

        val back = Button(this).apply {
            text = "Back to WA Companion"
            isAllCaps = false
            setOnClickListener { finish() }
        }
        page.addView(back)
        page.addView(space(12))
        page.addView(text("Free basic listings are planned. Paid promotion will be optional and clearly marked as Sponsored. Buyers will be able to contact sellers through WhatsApp after live listings are available.", 12, muted))

        setContentView(scroll)
    }

    private fun showBackendNotice() {
        AlertDialog.Builder(this)
            .setTitle("Search is coming")
            .setMessage("Search and category filters will work when the shared marketplace database is connected. We are not showing pretend business results.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSellerNotice() {
        AlertDialog.Builder(this)
            .setTitle("Seller registration is being prepared")
            .setMessage("Seller accounts will be connected to a secure online database. Your business name, WhatsApp number or other details will not be collected until that connection and its privacy protections are ready.")
            .setPositiveButton("Understood", null)
            .show()
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setLineSpacing(dp(2).toFloat(), 1f)
        }

    private fun space(height: Int) = TextView(this).apply { this.height = dp(height) }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
