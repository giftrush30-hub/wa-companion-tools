package com.example.wacompanion

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * WA Discover link-first directory.
 *
 * Sellers keep products, photos and videos on WhatsApp Business.
 * WA Discover is intended to index business details and seller-supplied
 * catalogue/contact links only. Live publishing waits for a shared backend.
 */
class DiscoverActivity : Activity() {
    private val green = Color.rgb(6, 63, 50)
    private val muted = Color.rgb(93, 105, 100)

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
        page.addView(text("Find businesses beyond your WhatsApp contacts.", 15, muted))
        page.addView(space(18))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(18))
            setBackgroundColor(green)
        }
        hero.addView(text("WhatsApp is their shop. We help people find it.", 20, Color.WHITE, true))
        hero.addView(space(8))
        hero.addView(text("Discover Nigerian sellers, open their WhatsApp catalogues and chat with them directly. Sellers keep posting on WhatsApp as usual.", 14, Color.WHITE))
        hero.addView(space(12))
        hero.addView(text("LINK-FIRST DISCOVERY  •  NIGERIA", 11, Color.rgb(205, 235, 219), true))
        page.addView(hero)
        page.addView(space(16))

        val search = EditText(this).apply {
            hint = "Search businesses, products or services"
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

        page.addView(text("EXPLORE BUSINESSES", 13, green, true))
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

        val explainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(Color.WHITE)
        }
        explainer.addView(text("How WA Discover works", 18, green, true))
        explainer.addView(space(9))
        explainer.addView(text("1. Sellers post products and updates on WhatsApp Status as usual.", 14, muted))
        explainer.addView(space(6))
        explainer.addView(text("2. Sellers share their WhatsApp Business catalogue link and business details with WA Discover.", 14, muted))
        explainer.addView(space(6))
        explainer.addView(text("3. Buyers find the business here, then open its catalogue or start a WhatsApp chat.", 14, muted))
        explainer.addView(space(12))
        explainer.addView(text("We plan to store business details and links — not copies of every seller photo or video. No WhatsApp Status is read automatically.", 12, muted, true))
        page.addView(explainer)
        page.addView(space(16))

        val status = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(Color.WHITE)
        }
        status.addView(text("Business discovery is being connected", 17, green, true))
        status.addView(space(7))
        status.addView(text("The directory needs a small shared online database before real businesses can register and appear in search. We are avoiding our own product-photo and video storage in this first version.", 14, muted))
        status.addView(space(10))
        status.addView(text("No sample sellers, fake products or invented view counts are displayed.", 12, muted, true))
        page.addView(status)
        page.addView(space(16))

        val sellerButton = Button(this).apply {
            text = "I'm a seller — add my WhatsApp business"
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
        page.addView(text("Basic business discovery is planned to be free. Optional sponsored promotion may come later and will be clearly labelled. We do not promise WhatsApp Status views or sales.", 12, muted))

        setContentView(scroll)
    }

    private fun showBackendNotice() {
        AlertDialog.Builder(this)
            .setTitle("Business search is coming")
            .setMessage("Search and category filters will become live after the shared business directory is connected. We are not displaying pretend businesses.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSellerNotice() {
        AlertDialog.Builder(this)
            .setTitle("Seller links are coming")
            .setMessage("The seller form will ask for your business name, category, state/city, WhatsApp contact link and WhatsApp Business catalogue link. You will keep your photos, videos and product updates on WhatsApp. We will only collect details after the secure directory is ready.")
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
