package com.coderGtm.yantra.activities

import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import com.android.volley.NoConnectionError
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.coderGtm.yantra.AppSortMode
import com.coderGtm.yantra.R
import com.coderGtm.yantra.databinding.ActivitySettingsBinding
import com.coderGtm.yantra.getUserNamePrefix
import com.coderGtm.yantra.misc.changedSettingsCallback
import com.coderGtm.yantra.misc.openAiApiKeySetter
import com.coderGtm.yantra.misc.openAiApiProviderSetter
import com.coderGtm.yantra.misc.openAiSystemPromptSetter
import com.coderGtm.yantra.misc.openAppSugOrderingSetter
import com.coderGtm.yantra.misc.openArrowSizeSetter
import com.coderGtm.yantra.misc.openDoubleTapActionSetter
import com.coderGtm.yantra.misc.openFontSizeSetter
import com.coderGtm.yantra.misc.openNewsWebsiteSetter
import com.coderGtm.yantra.misc.openOrientationSetter
import com.coderGtm.yantra.misc.openSwipeLeftActionSetter
import com.coderGtm.yantra.misc.openSwipeRightActionSetter
import com.coderGtm.yantra.misc.openTermuxCmdPathSelector
import com.coderGtm.yantra.misc.openTermuxCmdSessionActionSelector
import com.coderGtm.yantra.misc.openTermuxCmdWorkingDirSelector
import com.coderGtm.yantra.misc.openUsernamePrefixSetter
import com.coderGtm.yantra.misc.setAppSugOrderTvText
import com.coderGtm.yantra.misc.setOrientationTvText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject


class SettingsActivity : AppCompatActivity() {

    private var getPrimarySuggestions = true
    private var getSecondarySuggestions = true
    private var fullscreenLauncher = false
    private var vibrationPermission = true  // permission to vibrate on error
    private var showArrowKeys = true
    private var showLastFolder = false
    private var oneTapKeyboardActivation = true
    private var hideKeyboardOnEnter = true
    private var actOnSuggestionTap = false
    private var initCmdLog = false
    private var fontSize = 16
    private var arrowSize = 65
    private var orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    private var appSugOrderingMode = AppSortMode.A_TO_Z.value
    private var fontName = "Source Code Pro"

    private lateinit var binding: ActivitySettingsBinding

    private val prefFile = "yantraSP"
    private val preferenceObject: SharedPreferences
        get() = applicationContext.getSharedPreferences(prefFile,0)

    private val preferenceEditObject: SharedPreferences.Editor
        get() {
            val pref: SharedPreferences = applicationContext.getSharedPreferences(prefFile,0)
            return pref.edit()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getPrimarySuggestions = preferenceObject.getBoolean("getPrimarySuggestions",true)
        getSecondarySuggestions = preferenceObject.getBoolean("getSecondarySuggestions",true)
        fullscreenLauncher = preferenceObject.getBoolean("fullScreen",false)
        vibrationPermission = preferenceObject.getBoolean("vibrationPermission",true)
        showArrowKeys = preferenceObject.getBoolean("showArrowKeys",true)
        showLastFolder = preferenceObject.getBoolean("showLastFolder", false)
        oneTapKeyboardActivation = preferenceObject.getBoolean("oneTapKeyboardActivation",true)
        hideKeyboardOnEnter = preferenceObject.getBoolean("hideKeyboardOnEnter", true)
        actOnSuggestionTap = preferenceObject.getBoolean("actOnSuggestionTap", false)
        initCmdLog = preferenceObject.getBoolean("initCmdLog", false)
        fontSize = preferenceObject.getInt("fontSize",16)
        arrowSize = preferenceObject.getInt("arrowSize", 65)
        orientation = preferenceObject.getInt("orientation", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        appSugOrderingMode = preferenceObject.getInt("appSortMode", AppSortMode.A_TO_Z.value)
        fontName = preferenceObject.getString("font","Source Code Pro") ?: "Source Code Pro"


        binding.usernamePrefix.text = getUserNamePrefix(preferenceObject)
        binding.fontSizeBtn.text = fontSize.toString()
        binding.arrowSizeBtn.text = arrowSize.toString()
        setOrientationTvText(binding, orientation)
        setAppSugOrderTvText(binding, appSugOrderingMode)
        binding.tvFontName.text = fontName
        binding.prefixLayout.setOnClickListener { openUsernamePrefixSetter(this@SettingsActivity, binding, preferenceObject, preferenceEditObject) }
        binding.doubleTapActionLayout.setOnClickListener { openDoubleTapActionSetter(this@SettingsActivity, preferenceObject, preferenceEditObject) }
        binding.rightSwipeActionLayout.setOnClickListener { openSwipeRightActionSetter(this@SettingsActivity, preferenceObject, preferenceEditObject) }
        binding.leftSwipeActionLayout.setOnClickListener { openSwipeLeftActionSetter(this@SettingsActivity, preferenceObject, preferenceEditObject) }
        binding.newsWebsiteLayout.setOnClickListener { openNewsWebsiteSetter(this@SettingsActivity, preferenceObject, preferenceEditObject) }
        binding.fontSizeBtn.setOnClickListener { openFontSizeSetter(this@SettingsActivity, binding, preferenceObject, preferenceEditObject) }
        binding.arrowSizeBtn.setOnClickListener { openArrowSizeSetter(this@SettingsActivity, binding, preferenceObject, preferenceEditObject) }
        binding.orientationLay.setOnClickListener { openOrientationSetter(this@SettingsActivity, binding, preferenceEditObject) }
        binding.appSugOrderingLay.setOnClickListener { openAppSugOrderingSetter(this@SettingsActivity, binding, preferenceEditObject) }
        binding.termuxCmdPathLayout.setOnClickListener { openTermuxCmdPathSelector(this@SettingsActivity, preferenceObject, preferenceEditObject) }
        binding.termuxCmdWorkDirLayout.setOnClickListener { openTermuxCmdWorkingDirSelector(this@SettingsActivity, preferenceObject, preferenceEditObject) }
        binding.termuxCmdSessionActionLayout.setOnClickListener { openTermuxCmdSessionActionSelector(this@SettingsActivity, preferenceObject, preferenceEditObject) }
        binding.aiProviderLayout.setOnClickListener { openAiApiProviderSetter(this@SettingsActivity, preferenceObject, preferenceEditObject) }
        binding.aiApiKeyLayout.setOnClickListener { openAiApiKeySetter(this@SettingsActivity, preferenceObject, preferenceEditObject) }
        binding.aiSystemPromptLayout.setOnClickListener { openAiSystemPromptSetter(this@SettingsActivity, preferenceObject, preferenceEditObject) }

        binding.fontLay.setOnClickListener {
            if (preferenceObject.getBoolean("fontpack___purchased",false)) {
                Toast.makeText(this, "Loading Fonts...", Toast.LENGTH_SHORT).show()
                val url = "https://www.googleapis.com/webfonts/v1/webfonts?key=AIzaSyBFFPy6DsYRRQVlADHdCgKk5qd62CJxjqo"
                val queue = Volley.newRequestQueue(this)
                val stringRequest = StringRequest(
                    Request.Method.GET, url,
                    { response ->
                        val jsonArray = JSONObject(response).getJSONArray("items")
                        val names = ArrayList<String>()
                        for (i in 0 until jsonArray.length()) {
                            names.add(jsonArray.getJSONObject(i).getString("family"))
                        }
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Select a Font")
                            .setItems(names.toTypedArray()) { dialog, which ->
                                downloadFont(names[which])
                            }
                            .show()
                    },
                    { error ->
                        if (error is NoConnectionError) {
                            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
                        }
                        else {
                            Toast.makeText(this, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    })
                queue.add(stringRequest)
            }
            else {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Font Pack is not purchased!")
                    .setMessage("You need to purchase Font Pack to access this setting.\n\nFont Pack is an add-on for Yantra Launcher that lets you use any font from the entire collection of Google Fonts of more than 1550 fonts for your Yantra Launcher Terminal.\n\nYou can purchase Font Pack using the 'fontpack' command.")
                    .setPositiveButton("Cool") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        binding.primarySugSwitch.isChecked = getPrimarySuggestions
        binding.primarySugSwitch.setOnCheckedChangeListener { _, isChecked ->
            getPrimarySuggestions = isChecked
            preferenceEditObject.putBoolean("getPrimarySuggestions",isChecked).apply()
            changedSettingsCallback(this@SettingsActivity)
        }
        binding.secondarySugSwitch.isChecked = getSecondarySuggestions
        binding.secondarySugSwitch.setOnCheckedChangeListener { _, isChecked ->
            getSecondarySuggestions = isChecked
            preferenceEditObject.putBoolean("getSecondarySuggestions",isChecked).apply()
            changedSettingsCallback(this@SettingsActivity)
        }
        binding.fullscreenSwitch.isChecked = fullscreenLauncher
        binding.fullscreenSwitch.setOnCheckedChangeListener { _, isChecked ->
            fullscreenLauncher = isChecked
            preferenceEditObject.putBoolean("fullScreen", isChecked).apply()
            changedSettingsCallback(this@SettingsActivity)
        }
        binding.vibrationSwitch.isChecked = vibrationPermission
        binding.vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            vibrationPermission = isChecked
            preferenceEditObject.putBoolean("vibrationPermission",isChecked).apply()
            changedSettingsCallback(this@SettingsActivity)
        }
        binding.showArrowSwitch.isChecked = showArrowKeys
        binding.showArrowSwitch.setOnCheckedChangeListener { _, isChecked ->
            showArrowKeys = isChecked
            preferenceEditObject.putBoolean("showArrowKeys",isChecked).apply()
            changedSettingsCallback(this@SettingsActivity)
        }
        binding.showLastFolder.isChecked = showLastFolder
        binding.showLastFolder.setOnCheckedChangeListener { _, isChecked ->
            showLastFolder = isChecked
            preferenceEditObject.putBoolean("showLastFolder",isChecked).apply()
            changedSettingsCallback(this@SettingsActivity)
        }
        binding.oneTapKeyboardActivationSwitch.isChecked = oneTapKeyboardActivation
        binding.oneTapKeyboardActivationSwitch.setOnCheckedChangeListener { _, isChecked ->
            oneTapKeyboardActivation = isChecked
            preferenceEditObject.putBoolean("oneTapKeyboardActivation",isChecked).apply()
            changedSettingsCallback(this@SettingsActivity)
        }
        binding.hideKeyboardOnEnterSwitch.isChecked = hideKeyboardOnEnter
        binding.hideKeyboardOnEnterSwitch.setOnCheckedChangeListener { _, isChecked ->
            hideKeyboardOnEnter = isChecked
            preferenceEditObject.putBoolean("hideKeyboardOnEnter",isChecked).apply()
            changedSettingsCallback(this@SettingsActivity)
        }
        binding.actOnSuggestionTapSwitch.isChecked = actOnSuggestionTap
        binding.actOnSuggestionTapSwitch.setOnCheckedChangeListener { _, isChecked ->
            actOnSuggestionTap = isChecked
            preferenceEditObject.putBoolean("actOnSuggestionTap", isChecked).apply()
            changedSettingsCallback(this@SettingsActivity)
        }
        binding.initCmdLogSwitch.isChecked = initCmdLog
        binding.initCmdLogSwitch.setOnCheckedChangeListener { _, isChecked ->
            initCmdLog = isChecked
            preferenceEditObject.putBoolean("initCmdLog", isChecked).apply()
            changedSettingsCallback(this@SettingsActivity)
        }
    }

    private fun downloadFont(name: String) {
        val request = FontRequest(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            name,
            R.array.com_google_android_gms_fonts_certs
        )
        val callback = object : FontsContractCompat.FontRequestCallback() {

            override fun onTypefaceRetrieved(typeface: Typeface) {
                // font downloaded
                preferenceEditObject.putString("font",name).apply()
                binding.tvFontName.text = name
                Toast.makeText(this@SettingsActivity, "Terminal Font updated to $name!", Toast.LENGTH_SHORT).show()
                changedSettingsCallback(this@SettingsActivity)
            }

            override fun onTypefaceRequestFailed(reason: Int) {
                //error. not yet downloaded
                Toast.makeText(this@SettingsActivity, "Error Downloading Font! Please check you internet connection and try again.", Toast.LENGTH_LONG).show()
            }
        }
        //make handler to fetch font in background
        val handler = Handler()
        FontsContractCompat.requestFont(this, request, callback, handler)
    }
}