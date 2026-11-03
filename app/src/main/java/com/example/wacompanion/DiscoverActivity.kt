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
import java.util.concurrent.Executors

/**
 * WA Discover link-first directory.
 *
 * Sellers keep products, photos and videos on WhatsApp Business.
 * WA Discover is intended to index business details and seller-supplied
 * catalogue/contact links only. Live publishing waits for a shared backend.
 */
class DiscoverActivity : Activity() {
    private val backend by lazy { DiscoverBackend(this) }
    private val io = Executors.newSingleThreadExecutor()
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

        page.addView(text("FIND A BUSINESS", 13, green, true))
        val searchTerm = EditText(this).apply { hint = "Business, product or service"; textSize = 15f }
        val searchState = EditText(this).apply { hint = "State (optional)"; textSize = 14f }
        val searchCity = EditText(this).apply { hint = "City or town (optional)"; textSize = 14f }
        val searchResults = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(searchTerm)
        page.addView(searchState)
        page.addView(searchCity)
        page.addView(Button(this).apply { text = "Search live businesses"; isAllCaps = false; setOnClickListener { searchLive(searchTerm.text.toString(), searchState.text.toString(), searchCity.text.toString(), searchResults) } })
        page.addView(searchResults)
        page.addView(space(14))
        page.addView(text("START DISCOVERING", 13, green, true))
        page.addView(space(8))
        page.addView(Button(this).apply {
            text = "Open a WhatsApp catalogue link"
            isAllCaps = false
            setOnClickListener { promptCatalogueLink() }
        })
        page.addView(Button(this).apply {
            text = "Chat with a business on WhatsApp"
            isAllCaps = false
            setOnClickListener { promptWhatsAppNumber() }
        })
        page.addView(space(12))
        page.addView(text("Search approved WA Discover businesses by name, product, service or location. New seller listings remain private until reviewed and approved.", 13, muted))
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
                setOnClickListener { searchLive("", "", "", searchResults) }
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
        status.addView(text("A growing directory of approved Nigerian businesses", 17, green, true))
        status.addView(space(7))
        status.addView(text("WA Discover uses a shared online directory. Sellers submit business details for review; approved listings appear in search. Product photos and videos stay in WhatsApp Business catalogues.", 14, muted))
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
        page.addView(space(10))

        val promoteButton = Button(this).apply {
            text = "Advertise my business"
            isAllCaps = false
            setOnClickListener { showPromotionPackages() }
        }
        page.addView(promoteButton)
        page.addView(space(8))
        page.addView(text("Free basic listing. Optional paid boosts will be clearly marked Sponsored and promote placement inside WA Discover only. Payment is not active yet.", 12, muted))

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

    private fun showPromotionPackages() {
        val message = """
            FREE BASIC LISTING — ₦0
            Keep an approved business profile discoverable.

            LOCAL BOOST — ₦50
            A low-cost sponsored placement for local discovery.

            WIDER DISCOVERY — ₦100
            A sponsored placement with wider discovery across WA Discover.

            FEATURED LISTING — ₦300
            A more prominent Sponsored listing in discovery.

            BUSINESS SPOTLIGHT — ₦1,000
            The highest-visibility Sponsored placement in WA Discover.

            Promotions do not guarantee views, enquiries or sales. They do not buy WhatsApp Status views or send messages to anyone.
            
            Paystack checkout is not connected yet. No money will be collected from this screen.
        """.trimIndent()
        AlertDialog.Builder(this)
            .setTitle("Promote your business")
            .setMessage(message)
            .setPositiveButton("Continue") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Payments coming soon")
                    .setMessage("Your promotion options are now recorded in the app plan. Paystack will be connected after the shared database and secure server-side payment verification are ready. Do not send money manually.")
                    .setPositiveButton("OK", null)
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun promptCatalogueLink() {
        val input = EditText(this).apply {
            hint = "Paste the seller's catalogue or product link"
            textSize = 15f
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        AlertDialog.Builder(this)
            .setTitle("Open WhatsApp catalogue")
            .setMessage("Paste a catalogue or product link the seller shared with you.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Open") { _, _ ->
                val link = input.text.toString().trim()
                if (link.startsWith("https://", ignoreCase = true)) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(link)))
                    } catch (_: Exception) {
                        AlertDialog.Builder(this).setMessage("No app or browser could open that link.")
                            .setPositiveButton("OK", null).show()
                    }
                } else {
                    AlertDialog.Builder(this).setMessage("Please paste a secure link beginning with https://.")
                        .setPositiveButton("OK", null).show()
                }
            }
            .show()
    }

    private fun promptWhatsAppNumber() {
        val input = EditText(this).apply {
            hint = "Number with country code, e.g. 2348012345678"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            textSize = 15f
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        AlertDialog.Builder(this)
            .setTitle("Chat with a seller")
            .setMessage("For Nigeria, use 234 followed by the number, without the first zero.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue") { _, _ ->
                val number = input.text.toString().filter { it.isDigit() }
                if (number.length in 10..15) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/$number")))
                    } catch (_: Exception) {
                        AlertDialog.Builder(this).setMessage("WhatsApp or a browser could not open this number.")
                            .setPositiveButton("OK", null).show()
                    }
                } else {
                    AlertDialog.Builder(this).setMessage("Please enter a valid international number.")
                        .setPositiveButton("OK", null).show()
                }
            }
            .show()
    }

    private fun showBackendNotice() {
        AlertDialog.Builder(this)
            .setTitle("Choose a search above")
            .setMessage("Use the search fields to find approved businesses. Category-specific filters are the next improvement.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSellerNotice() {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(8)) }
        fun field(hintText: String, inputType: Int = android.text.InputType.TYPE_CLASS_TEXT): EditText {
            val e = EditText(this).apply { hint = hintText; textSize = 14f; setInputType(inputType) }
            panel.addView(e, LinearLayout.LayoutParams(-1, dp(50)))
            return e
        }
        val name = field("Business name")
        val category = field("Category: Fashion, Food, Phones, Beauty…")
        val state = field("State")
        val city = field("City or town")
        val phone = field("WhatsApp number e.g. 2348012345678", android.text.InputType.TYPE_CLASS_PHONE)
        val catalogue = field("WhatsApp catalogue link (optional)")
        val description = field("Short business description")
        val dialog = AlertDialog.Builder(this).setTitle("Submit your business")
            .setMessage("Your listing is reviewed before it becomes public. Use your Nigerian number with country code 234 and no leading zero.")
            .setView(ScrollView(this).apply { addView(panel) })
            .setNegativeButton("Cancel", null).setPositiveButton("Continue", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val n = phone.text.toString().filter { it.isDigit() }
                if (name.text.isBlank() || category.text.isBlank() || state.text.isBlank() || city.text.isBlank()) {
                    name.error = "Complete the required business details"; return@setOnClickListener
                }
                if (!n.matches(Regex("^[1-9][0-9]{9,14}$"))) {
                    phone.error = "Use international format, e.g. 2348012345678"; return@setOnClickListener
                }
                val link = catalogue.text.toString().trim()
                if (link.isNotEmpty() && !link.startsWith("https://")) {
                    catalogue.error = "Catalogue link must begin with https://"; return@setOnClickListener
                }
                val cat = category.text.toString().lowercase()
                val categoryId = when {
                    "fashion" in cat || "cloth" in cat -> "fashion"
                    "food" in cat || "grocery" in cat -> "food"
                    "phone" in cat -> "phones"
                    "beaut" in cat -> "beauty"
                    "elect" in cat -> "electronics"
                    "home" in cat -> "home"
                    "farm" in cat -> "farming"
                    "service" in cat -> "services"
                    else -> "other"
                }
                val payload = org.json.JSONObject()
                    .put("name", name.text.toString().trim())
                    .put("category_id", categoryId)
                    .put("description", description.text.toString().trim())
                    .put("whatsapp_number", n)
                    .put("catalogue_url", if (link.isBlank()) org.json.JSONObject.NULL else link)
                    .put("state", state.text.toString().trim())
                    .put("city", city.text.toString().trim())
                getSharedPreferences("wa_discover_seller_draft", MODE_PRIVATE).edit()
                    .putString("business", name.text.toString()).putString("category", category.text.toString())
                    .putString("state", state.text.toString()).putString("city", city.text.toString())
                    .putString("phone", n).putString("catalogue", link).putString("about", description.text.toString()).apply()
                dialog.dismiss()
                val session = backend.currentSession()
                if (session != null) submitSellerListing(session, payload)
                else sellerLoginDialog(payload)
            }
        }
        dialog.show()
    }

    private fun sellerLoginDialog(payload: org.json.JSONObject) {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(4), dp(16), dp(4)) }
        val email = EditText(this).apply { hint = "Email address"; inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        val password = EditText(this).apply { hint = "Password (minimum 8 characters)"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        panel.addView(email); panel.addView(password)
        val dialog = AlertDialog.Builder(this).setTitle("Seller account")
            .setMessage("Create a free seller account or sign in. Buyers do not need an account.")
            .setView(panel).setNegativeButton("Cancel", null)
            .setNeutralButton("Create account", null).setPositiveButton("Sign in", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val e = email.text.toString().trim(); val p = password.text.toString()
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches() || p.length < 8) {
                    password.error = "Enter a valid email and password"; return@setOnClickListener
                }
                dialog.dismiss()
                io.execute {
                    try {
                        val session = backend.signIn(e, p)
                        runOnUiThread { submitSellerListing(session, payload) }
                    } catch (ex: Exception) {
                        runOnUiThread { AlertDialog.Builder(this).setMessage(ex.message ?: "Sign-in failed").setPositiveButton("OK", null).show() }
                    }
                }
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                val e = email.text.toString().trim(); val p = password.text.toString()
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches() || p.length < 8) {
                    password.error = "Enter a valid email and password"; return@setOnClickListener
                }
                dialog.dismiss()
                io.execute {
                    try {
                        backend.signUp(e, p)
                        runOnUiThread {
                            AlertDialog.Builder(this).setTitle("Account request received")
                                .setMessage("Check your email if confirmation is enabled. Then choose Add my business again and sign in to submit your listing.")
                                .setPositiveButton("OK", null).show()
                        }
                    } catch (ex: Exception) {
                        runOnUiThread { AlertDialog.Builder(this).setMessage(ex.message ?: "Account creation failed").setPositiveButton("OK", null).show() }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun submitSellerListing(session: DiscoverBackend.SellerSession, payload: org.json.JSONObject) {
        val progress = AlertDialog.Builder(this).setMessage("Submitting your business for review…").setCancelable(false).create()
        progress.show()
        io.execute {
            try {
                backend.submitBusiness(session, payload)
                runOnUiThread {
                    progress.dismiss()
                    AlertDialog.Builder(this).setTitle("Submitted for review")
                        .setMessage("Your business details have been received. Your listing stays private until an administrator approves it.")
                        .setPositiveButton("OK", null).show()
                }
            } catch (ex: Exception) {
                runOnUiThread {
                    progress.dismiss()
                    AlertDialog.Builder(this).setTitle("Submission failed")
                        .setMessage(ex.message ?: "Please check your internet connection and try again.")
                        .setPositiveButton("OK", null).show()
                }
            }
        }
    }

    private fun searchLive(term: String, state: String, city: String, output: LinearLayout) {
        output.removeAllViews()
        output.addView(text("Searching WA Discover…", 13, muted))
        io.execute {
            try {
                val rows = backend.searchBusinesses(term, state, city)
                runOnUiThread {
                    output.removeAllViews()
                    if (rows.length() == 0) output.addView(text("No approved businesses found yet. Try another search.", 13, muted))
                    for (i in 0 until rows.length()) {
                        val item = rows.getJSONObject(i)
                        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12)); setBackgroundColor(Color.WHITE) }
                        card.addView(text(item.optString("name"), 17, green, true))
                        card.addView(text(item.optString("city") + ", " + item.optString("state") + " • " + item.optString("category_id"), 12, muted))
                        if (item.optString("description").isNotBlank()) card.addView(text(item.optString("description"), 13, muted))
                        card.addView(Button(this).apply { text = "Chat on WhatsApp"; isAllCaps = false; setOnClickListener { val n = item.optString("whatsapp_number"); if (n.isNotBlank()) startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/" + n))) } })
                        val link = item.optString("catalogue_url")
                        if (link.startsWith("https://")) card.addView(Button(this).apply { text = "Open catalogue"; isAllCaps = false; setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(link))) } })
                        output.addView(card)
                        output.addView(space(8))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { output.removeAllViews(); output.addView(text("Search failed: " + (e.message ?: "Please try again."), 13, Color.RED)) }
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); io.shutdown() }

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
