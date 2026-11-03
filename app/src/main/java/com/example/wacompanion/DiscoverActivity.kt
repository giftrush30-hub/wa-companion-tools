package com.example.wacompanion

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
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
        val fields = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(8))
        }
        fun addField(label: String, type: Int = android.text.InputType.TYPE_CLASS_TEXT): EditText {
            val input = EditText(this).apply {
                hint = label
                textSize = 15f
                isSingleLine = true
                inputType = type
            }
            fields.addView(input, LinearLayout.LayoutParams(-1, dp(52)))
            return input
        }
        val business = addField("Business name")
        val category = addField("Category (e.g. Fashion, Food, Phones)")
        val state = addField("State")
        val city = addField("City or town")
        val phone = addField("WhatsApp number with country code", android.text.InputType.TYPE_CLASS_PHONE)
        val catalogue = addField("WhatsApp Business catalogue link (optional)")
        val about = EditText(this).apply {
            hint = "Short description of your business"
            textSize = 15f
            minLines = 2
        }
        fields.addView(about, LinearLayout.LayoutParams(-1, dp(82)))
        val content = ScrollView(this).apply { addView(fields) }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Prepare your business profile")
            .setMessage("This is a private draft on this phone for now. It will NOT publish your business in the public directory yet.")
            .setView(content)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Save draft", null)
            .setPositiveButton("Share details", null)
            .create()

        fun valid(): Boolean {
            if (business.text.isBlank() || category.text.isBlank() || state.text.isBlank() ||
                city.text.isBlank() || phone.text.isBlank()) {
                AlertDialog.Builder(this)
                    .setMessage("Please fill in business name, category, state, city and WhatsApp number.")
                    .setPositiveButton("OK", null).show()
                return false
            }
            if (phone.text.toString().filter { it.isDigit() }.length < 10) {
                phone.error = "Enter a valid number with country code"
                return false
            }
            val link = catalogue.text.toString().trim()
            if (link.isNotEmpty() && !(link.startsWith("https://") || link.startsWith("http://"))) {
                catalogue.error = "Enter a full link beginning with https://"
                return false
            }
            return true
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                if (!valid()) return@setOnClickListener
                getSharedPreferences("wa_discover_seller_draft", MODE_PRIVATE).edit()
                    .putString("business", business.text.toString().trim())
                    .putString("category", category.text.toString().trim())
                    .putString("state", state.text.toString().trim())
                    .putString("city", city.text.toString().trim())
                    .putString("phone", phone.text.toString().trim())
                    .putString("catalogue", catalogue.text.toString().trim())
                    .putString("about", about.text.toString().trim())
                    .apply()
                AlertDialog.Builder(this).setTitle("Draft saved")
                    .setMessage("Your business details are saved privately on this phone. They have not been uploaded or published.")
                    .setPositiveButton("OK", null).show()
                dialog.dismiss()
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (!valid()) return@setOnClickListener
                val details = buildString {
                    appendLine("WA DISCOVER — BUSINESS PROFILE")
                    appendLine("Business: ${business.text.toString().trim()}")
                    appendLine("Category: ${category.text.toString().trim()}")
                    appendLine("Location: ${city.text.toString().trim()}, ${state.text.toString().trim()}, Nigeria")
                    appendLine("WhatsApp: ${phone.text.toString().trim()}")
                    if (catalogue.text.isNotBlank()) appendLine("Catalogue: ${catalogue.text.toString().trim()}")
                    if (about.text.isNotBlank()) appendLine("About: ${about.text.toString().trim()}")
                    appendLine()
                    append("Owner-submitted draft; not yet a verified WA Discover listing.")
                }
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "WA Discover business profile")
                    putExtra(Intent.EXTRA_TEXT, details)
                }
                startActivity(Intent.createChooser(shareIntent, "Share business details"))
                dialog.dismiss()
            }
        }
        dialog.show()
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
