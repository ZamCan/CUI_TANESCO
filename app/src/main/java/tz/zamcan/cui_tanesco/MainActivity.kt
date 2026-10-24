package tz.zamcan.cui_tanesco

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private var simulatorMode = true
    private lateinit var modeLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(245, 241, 232)
        window.navigationBarColor = Color.rgb(245, 241, 232)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(24))
            setBackgroundColor(Color.rgb(245, 241, 232))
        }
        val scroll = ScrollView(this).apply { addView(page) }
        setContentView(scroll)

        page.addView(text("TANESCO DIGITAL CIU", 12, Color.rgb(145, 99, 42), true))
        page.addView(space(10))
        page.addView(text("Electricity,\nmade clearer.", 31, Color.rgb(45, 42, 36), true))
        page.addView(space(8))
        page.addView(text("A phone-first companion interface for prepaid meter workflows.", 15, Color.rgb(105, 99, 88), false))
        page.addView(space(24))

        val statusCard = card()
        statusCard.addView(text("DEVICE STATUS", 11, Color.rgb(126, 112, 91), true))
        statusCard.addView(space(10))
        modeLabel = text("SIMULATOR MODE · NOT CONNECTED", 16, Color.rgb(135, 79, 27), true)
        statusCard.addView(modeLabel)
        statusCard.addView(space(7))
        statusCard.addView(text("No physical meter is connected. Values and actions in this baseline are not real meter operations.", 13, Color.rgb(94, 88, 78), false))
        page.addView(statusCard)
        page.addView(space(16))

        val meterCard = card()
        meterCard.addView(text("METER WORKSPACE", 11, Color.rgb(126, 112, 91), true))
        meterCard.addView(space(12))
        meterCard.addView(text("Ready for your meter", 20, Color.rgb(45, 42, 36), true))
        meterCard.addView(space(6))
        meterCard.addView(text("Meter model, CIU pairing, balance readings and token workflows will only be enabled after compatible hardware and protocol behaviour are verified.", 14, Color.rgb(94, 88, 78), false))
        page.addView(meterCard)
        page.addView(space(16))

        val modeButton = Button(this).apply {
            text = "SIMULATOR MODE ACTIVE"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(151, 99, 39), 14)
            setOnClickListener {
                simulatorMode = !simulatorMode
                simulatorMode = true // Live mode is deliberately unavailable until hardware validation.
                modeLabel.text = "SIMULATOR MODE · NOT CONNECTED"
                text = "SIMULATOR MODE ACTIVE"
            }
        }
        page.addView(modeButton, LinearLayout.LayoutParams(-1, dp(54)))
        page.addView(space(12))
        page.addView(text("ENGINEERING NOTE", 11, Color.rgb(126, 112, 91), true))
        page.addView(space(5))
        page.addView(text("This is an initial native UI baseline. It does not contain TANESCO proprietary protocols, meter credentials, live token submission, or verified official logos.", 13, Color.rgb(105, 99, 88), false))
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(Color.WHITE, 20)
        elevation = dp(2).toFloat()
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.START
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
}
