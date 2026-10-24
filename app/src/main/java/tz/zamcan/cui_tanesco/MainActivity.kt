package tz.zamcan.cui_tanesco

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val ink = Color.rgb(42,40,34)
    private val muted = Color.rgb(112,105,92)
    private val paper = Color.rgb(247,244,237)
    private val amber = Color.rgb(166,108,43)
    private val green = Color.rgb(65,111,83)
    private lateinit var content: LinearLayout
    private var meterId = ""
    private var profile = "My meter"
    private var token = ""
    private val history = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val p = getSharedPreferences("ciu_local", MODE_PRIVATE)
        meterId = p.getString("meter_id", "") ?: ""
        profile = p.getString("profile", "My meter") ?: "My meter"
        history.addAll((p.getString("history", "") ?: "").split("\n").filter { it.isNotBlank() })
        window.statusBarColor = paper
        window.navigationBarColor = paper
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(paper) }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20),dp(22),dp(20),dp(22)) }
        root.addView(ScrollView(this).apply { isFillViewport = true; addView(content) }, LinearLayout.LayoutParams(-1,0,1f))
        root.addView(navigation())
        setContentView(root)
        home()
    }

    private fun home() {
        content.removeAllViews()
        content.addView(label("TANESCO  /  DIGITAL CIU", amber, 11))
        content.addView(space(12))
        content.addView(txt("Your electricity,\nclearly in view.",31,ink,true))
        content.addView(space(8))
        content.addView(txt("An independent companion concept for prepaid-meter workflows.",15,muted,false))
        content.addView(space(20))
        content.addView(card().apply {
            addView(row().apply { addView(label("CONNECTION STATUS",muted,10),LinearLayout.LayoutParams(0,-2,1f)); addView(pill("OFFLINE")) })
            addView(space(12)); addView(txt("No meter connected",21,ink,true)); addView(space(5))
            addView(txt("The meter communication adapter is not implemented. Phone connectivity alone cannot confirm meter compatibility.",13,muted,false))
        })
        content.addView(space(16))
        content.addView(card().apply {
            addView(label("METER PROFILE",muted,10)); addView(space(12))
            val r = row()
            r.addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(dp(10),dp(15),dp(10),dp(15))
                background=rounded(Color.rgb(233,230,220),16)
                addView(txt("METER",10,muted,true)); addView(space(5)); addView(txt("— — — —",16,green,true)); addView(txt("LCD DEMO",8,muted,true))
            },LinearLayout.LayoutParams(dp(100),dp(92)))
            r.addView(gapW(12))
            r.addView(LinearLayout(this@MainActivity).apply {
                orientation=LinearLayout.VERTICAL
                addView(txt(if(meterId.isBlank()) "No meter saved" else profile,18,ink,true)); addView(space(5))
                addView(txt(if(meterId.isBlank()) "Add a local profile to organise your workspace." else "Meter ID: "+meterId,13,muted,false))
                addView(space(8)); addView(label("BALANCE — NOT AVAILABLE",amber,10))
            },LinearLayout.LayoutParams(0,-2,1f))
            addView(r); addView(space(12)); addView(outline("Set up meter profile"){ setupMeter() })
        })
        content.addView(space(14))
        val tiles = row()
        tiles.addView(tile("Token keypad","Practice entry only"){ tokenScreen() },LinearLayout.LayoutParams(0,-2,1f))
        tiles.addView(gapW(10))
        tiles.addView(tile("Activity log","Local actions"){ activityScreen() },LinearLayout.LayoutParams(0,-2,1f))
        content.addView(tiles); content.addView(space(18))
        content.addView(label("RECENT ACTIVITY",muted,10)); content.addView(space(8))
        if(history.isEmpty()) content.addView(txt("Nothing recorded yet.",14,muted,false))
        else history.take(3).forEach { content.addView(txt("•  "+it,13,ink,false)); content.addView(space(6)) }
        content.addView(space(14))
        content.addView(note("DEVELOPER PREVIEW","Simulator only · Not connected to a physical meter. No token is transmitted and no live balance is available."))
    }

    private fun setupMeter() {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(6),dp(18),0) }
        val name=EditText(this).apply { hint="Profile name"; setText(profile); singleLine=true }
        val id=EditText(this).apply { hint="Meter identifier"; setText(meterId); singleLine=true; inputType=InputType.TYPE_CLASS_TEXT }
        box.addView(name); box.addView(id)
        AlertDialog.Builder(this).setTitle("Local meter profile").setMessage("Stored on this phone only. Saving does not connect to a meter.")
            .setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->
                val newId=id.text.toString().trim()
                if(newId.isBlank()) AlertDialog.Builder(this).setMessage("Enter a meter identifier.").setPositiveButton("OK",null).show()
                else { meterId=newId; profile=name.text.toString().trim().ifBlank{"My meter"}
                    getSharedPreferences("ciu_local",MODE_PRIVATE).edit().putString("meter_id",meterId).putString("profile",profile).apply()
                    addLog("Saved local meter profile: "+profile); home() }
            }.show()
    }

    private fun tokenScreen() {
        content.removeAllViews()
        content.addView(label("TOKEN CENTER",amber,11)); content.addView(space(10))
        content.addView(txt("Enter a token",28,ink,true)); content.addView(space(7))
        content.addView(txt("Practice keypad for UI testing. Nothing is validated or sent to a meter.",14,muted,false)); content.addView(space(16))
        val panel=card()
        val display=TextView(this).apply { text=token.ifBlank{"ENTER TOKEN"}; textSize=20f; typeface=Typeface.MONOSPACE; gravity=Gravity.CENTER; setTextColor(green); setPadding(dp(8),dp(18),dp(8),dp(18)); background=rounded(Color.rgb(232,235,224),10) }
        panel.addView(display,LinearLayout.LayoutParams(-1,dp(68))); panel.addView(space(10))
        listOf("1","2","3","4","5","6","7","8","9","⌫","0","CLEAR").chunked(3).forEach { keys ->
            val line=row()
            keys.forEachIndexed { i,k ->
                line.addView(Button(this).apply { text=k; isAllCaps=false; setTextColor(ink); background=rounded(Color.rgb(242,239,231),12)
                    setOnClickListener { token=when(k){"CLEAR"->"";"⌫"->token.dropLast(1);else->if(token.length<20)token+k else token}; display.text=token.ifBlank{"ENTER TOKEN"} }
                },LinearLayout.LayoutParams(0,dp(52),1f))
                if(i<2) line.addView(gapW(7))
            }
            panel.addView(line); panel.addView(space(7))
        }
        content.addView(panel); content.addView(space(12))
        content.addView(primary("Record demo entry"){
            if(token.length<6) AlertDialog.Builder(this).setMessage("Enter at least 6 digits for this local UI test.").setPositiveButton("OK",null).show()
            else { addLog("Demo token entry: "+token.length+" digits; not transmitted"); token=""
                AlertDialog.Builder(this).setTitle("Demo entry recorded").setMessage("Local interface test only. The token was not validated or sent to any meter.").setPositiveButton("OK"){_,_->tokenScreen()}.show() }
        })
        content.addView(space(8)); content.addView(outline("Back to overview"){home()}); content.addView(space(14))
        content.addView(note("IMPORTANT","Never treat a demo record as a successful recharge. Verify token acceptance on the physical meter display."))
    }

    private fun activityScreen() {
        content.removeAllViews(); content.addView(txt("Activity",28,ink,true)); content.addView(space(8))
        content.addView(txt("Local actions stored on this phone.",14,muted,false)); content.addView(space(14))
        if(history.isEmpty()) content.addView(note("NO ACTIVITY YET","Your local actions will appear here."))
        history.forEach { content.addView(card().apply{addView(txt(it,14,ink,false))}); content.addView(space(7)) }
        if(history.isNotEmpty()) content.addView(outline("Clear local history"){
            AlertDialog.Builder(this).setMessage("Clear local activity history?").setNegativeButton("Cancel",null).setPositiveButton("Clear"){_,_->history.clear();saveHistory();activityScreen()}.show()
        })
    }

    private fun settings() {
        content.removeAllViews(); content.addView(txt("Settings & about",28,ink,true)); content.addView(space(14))
        content.addView(note("BRANDING","Independent developer concept. Text label and generic illustration only; no official logo asset or endorsement is implied."))
        content.addView(space(10)); content.addView(note("PRIVACY","Profile and activity are stored locally. This build requests no network, Bluetooth, NFC or location permissions."))
        content.addView(space(10)); content.addView(note("CONNECTION ENGINE","Not implemented: pairing, RF, protocol, live readings and token transmission remain disabled pending verified hardware and authorised protocol evidence."))
        content.addView(space(14)); content.addView(label("VERSION 0.2.0 · ANDROID DEVELOPER PREVIEW",muted,10))
    }

    private fun navigation(): View {
        val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(dp(6),dp(5),dp(6),dp(5));setBackgroundColor(Color.WHITE);elevation=dp(7).toFloat()}
        listOf("Home","Token","Activity","Settings").forEach { item ->
            nav.addView(TextView(this).apply { text=item; textSize=12f; gravity=Gravity.CENTER; setPadding(dp(2),dp(13),dp(2),dp(13)); setTextColor(if(item=="Home")amber else muted); setTypeface(typeface,Typeface.BOLD)
                setOnClickListener{when(item){"Home"->home();"Token"->tokenScreen();"Activity"->activityScreen();else->settings()}}
            },LinearLayout.LayoutParams(0,-2,1f))
        }
        return nav
    }

    private fun addLog(value:String){history.add(0,value);while(history.size>50)history.removeAt(history.lastIndex);saveHistory()}
    private fun saveHistory(){getSharedPreferences("ciu_local",MODE_PRIVATE).edit().putString("history",history.joinToString("\n")).apply()}
    private fun tile(title:String,sub:String,click:()->Unit)=card().apply{addView(txt(title,15,ink,true));addView(space(5));addView(txt(sub,12,muted,false));setOnClickListener{click()}}
    private fun note(title:String,body:String)=card().apply{addView(label(title,amber,10));addView(space(7));addView(txt(body,13,muted,false))}
    private fun card()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(16),dp(16),dp(16));background=rounded(Color.WHITE,19);elevation=dp(2).toFloat()}
    private fun txt(v:String,s:Int,c:Int,b:Boolean)=TextView(this).apply{text=v;textSize=s.toFloat();setTextColor(c);if(b)setTypeface(typeface,Typeface.BOLD);gravity=Gravity.START;setLineSpacing(dp(2).toFloat(),1f)}
    private fun label(v:String,c:Int,s:Int)=txt(v,s,c,true)
    private fun pill(v:String)=TextView(this).apply{text=v;textSize=10f;setTextColor(amber);setTypeface(typeface,Typeface.BOLD);setPadding(dp(10),dp(6),dp(10),dp(6));background=rounded(Color.rgb(250,241,225),30)}
    private fun primary(v:String,click:()->Unit)=Button(this).apply{text=v;isAllCaps=false;setTextColor(Color.WHITE);background=rounded(amber,14);setOnClickListener{click()}}
    private fun outline(v:String,click:()->Unit)=Button(this).apply{text=v;isAllCaps=false;setTextColor(amber);background=rounded(Color.rgb(250,247,241),14);setOnClickListener{click()}}
    private fun row()=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
    private fun space(h:Int)=View(this).apply{layoutParams=LinearLayout.LayoutParams(1,dp(h))}
    private fun gapW(w:Int)=View(this).apply{layoutParams=LinearLayout.LayoutParams(dp(w),1)}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun rounded(c:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat()}
}
