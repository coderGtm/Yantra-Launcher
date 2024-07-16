package com.coderGtm.yantra.terminal

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.coderGtm.yantra.BuildConfig
import com.coderGtm.yantra.DEFAULT_ALIAS_LIST
import com.coderGtm.yantra.DEFAULT_TERMINAL_FONT_NAME
import com.coderGtm.yantra.NO_LOG_COMMANDS
import com.coderGtm.yantra.R
import com.coderGtm.yantra.activities.MainActivity
import com.coderGtm.yantra.blueprints.BaseCommand
import com.coderGtm.yantra.contactsManager
import com.coderGtm.yantra.databinding.ActivityMainBinding
import com.coderGtm.yantra.findSimilarity
import com.coderGtm.yantra.getCurrentTheme
import com.coderGtm.yantra.getInit
import com.coderGtm.yantra.getUserName
import com.coderGtm.yantra.getUserNamePrefix
import com.coderGtm.yantra.isPro
import com.coderGtm.yantra.marketProVersion
import com.coderGtm.yantra.models.Alias
import com.coderGtm.yantra.models.AppBlock
import com.coderGtm.yantra.models.ShortcutBlock
import com.coderGtm.yantra.requestCmdInputFocusAndShowKeyboard
import com.coderGtm.yantra.requestUpdateIfAvailable
import com.coderGtm.yantra.runInitTasks
import com.coderGtm.yantra.showRatingAndCommunityPopups
import com.coderGtm.yantra.vibrate
import io.noties.markwon.Markwon
import java.io.File
import java.util.TimerTask
import kotlin.math.roundToInt

class Terminal(
    val activity: Activity,
    val binding: ActivityMainBinding,
    val preferenceObject: SharedPreferences
) {
    private val fontSize = preferenceObject.getInt("fontSize", 16).toFloat()
    private val hideKeyboardOnEnter = preferenceObject.getBoolean("hideKeyboardOnEnter", true)
    private val cacheSize = 5
    private val vibrationPermission = preferenceObject.getBoolean("vibrationPermission",true)
    private val getPrimarySuggestions = preferenceObject.getBoolean("getPrimarySuggestions",true)
    private val getSecondarySuggestions = preferenceObject.getBoolean("getSecondarySuggestions",true)
    
    private var commandQueue: MutableList<String> = mutableListOf()
    private var cmdHistoryCursor = -1
    private var commandCache = mutableListOf<Map<String, BaseCommand>>()

    val theme = getCurrentTheme(activity, preferenceObject)
    val commands = getAvailableCommands(activity)
    var initialized = false
    var typeface: Typeface? = Typeface.createFromAsset(activity.assets, "fonts/source_code_pro.ttf")
    var isSleeping = false
    var sleepTimer: TimerTask? = null
    var contactsFetched: Boolean = false
    var contactNames = HashSet<String>()
    var appListFetched: Boolean = false
    var shortcutListFetched: Boolean = false
    var workingDir = ""
    var cmdHistory = ArrayList<String>()
    var username = binding.username

    lateinit var appList: ArrayList<AppBlock>
    lateinit var shortcutList: ArrayList<ShortcutBlock>
    lateinit var wakeBtn: TextView
    lateinit var aliasList: MutableList<Alias>

    fun initialize() {
        if (preferenceObject.getBoolean("useModernPromptDesign", false)) {
            binding.modernPrompt.visibility = View.VISIBLE
            binding.triangle.visibility = View.VISIBLE
            binding.username.visibility = View.GONE
            username = binding.modernPromptUsername
        }

        activity.requestedOrientation = preferenceObject.getInt("orientation", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        goFullScreen()
        enforceThemeComponents()
        setTypeface()
        setArrowKeys(preferenceObject, binding)
        binding.upBtn.setOnClickListener { cmdUp() }
        binding.downBtn.setOnClickListener { cmdDown() }
        setWallpaperIfNeeded(preferenceObject, activity.applicationContext, theme)
        createWakeButton()
        setTextChangedListener()
        createTouchListeners()
        aliasList = getAliases()
        checkAliasNames()
        setInputListener()
        setLauncherAppsListener(this@Terminal)
        appList = getAppsList(this@Terminal)
        shortcutList = getShortcutList(this@Terminal)
        showSuggestions(binding.cmdInput.text.toString(), getPrimarySuggestions, getSecondarySuggestions, this@Terminal)
        //fetching contacts if permitted
        if (ContextCompat.checkSelfPermission(activity.baseContext, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            Thread {
                contactsManager(this)
            }.start()
        }
        Thread {
            requestUpdateIfAvailable(preferenceObject, activity)
        }.start()
    }

    private fun enforceThemeComponents() {
        username.textSize = fontSize
        binding.cmdInput.textSize = fontSize
        activity.window.statusBarColor = Color.TRANSPARENT
        activity.window.navigationBarColor = theme.bgColor
        setPromptText()
        binding.suggestionsTab.background = theme.bgColor.toDrawable()
        username.setTextColor(theme.inputLineTextColor)
        binding.cmdInput.setTextColor(theme.inputLineTextColor)
        val unwrappedCursorDrawable = AppCompatResources.getDrawable(activity,
            R.drawable.cursor_drawable
        )
        val wrappedCursorDrawable = DrawableCompat.wrap(unwrappedCursorDrawable!!)
        DrawableCompat.setTint(wrappedCursorDrawable, theme.inputLineTextColor)
        binding.upBtn.setTextColor(theme.resultTextColor)
        binding.downBtn.setTextColor(theme.resultTextColor)
    }
    private fun setTypeface() {
        val fontName = if (isPro(activity)) {
            preferenceObject.getString("font", DEFAULT_TERMINAL_FONT_NAME) ?: DEFAULT_TERMINAL_FONT_NAME
        }
        else {
            DEFAULT_TERMINAL_FONT_NAME
        }
        if (fontName.endsWith(".ttf")) {
            val fontFile = File(activity.filesDir, fontName)
            if (fontFile.exists()) {
                typeface = Typeface.createFromFile(fontFile)
                username.setTypeface(typeface, Typeface.BOLD)
                binding.cmdInput.typeface = typeface
                finishInitialization()
            }
            return
        }
        val request = FontRequest(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            fontName,
            R.array.com_google_android_gms_fonts_certs
        )
        val callback = object : FontsContractCompat.FontRequestCallback() {

            override fun onTypefaceRetrieved(rTypeface: Typeface) {
                //set font as retrieved cliTypeface
                typeface = rTypeface
                username.setTypeface(typeface, Typeface.BOLD)
                binding.cmdInput.typeface = typeface
                finishInitialization()
            }

            override fun onTypefaceRequestFailed(reason: Int) {
                //set font as source code pro from res folder
                typeface = Typeface.createFromAsset(activity.assets, "fonts/source_code_pro.ttf")
                username.setTypeface(typeface, Typeface.BOLD)
                binding.cmdInput.typeface = typeface
                finishInitialization()
            }
        }
        //make handler to fetch font in background
        val handler = Handler(Looper.getMainLooper())
        FontsContractCompat.requestFont(activity, request, callback, handler)
    }

    private fun setInputListener() {
        binding.cmdInput.setOnEditorActionListener { v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    val inputReceived = binding.cmdInput.text.toString().trim()
                    handleInput(inputReceived)
                    true
                }
                else -> true
            }
        }
    }
    private fun setTextChangedListener() {
        if (getPrimarySuggestions || getSecondarySuggestions) {
            registerTextChangedListener()
        }
    }
    private fun registerTextChangedListener() {
        binding.cmdInput.addTextChangedListener {
            showSuggestions(it.toString(), getPrimarySuggestions, getSecondarySuggestions, this@Terminal)
        }
    }
    fun handleInput(input: String) {
        handleCommand(input)
        binding.cmdInput.setText("")
        if (hideKeyboardOnEnter) hideSoftKeyboard()
        goFullScreen()
    }
    private fun hideSoftKeyboard() {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.cmdInput.windowToken, 0)
    }
    private fun goFullScreen() {
        if (preferenceObject.getBoolean("fullScreen",false)) {
            val windowInsetsController = ViewCompat.getWindowInsetsController(activity.window.decorView)
            // Hide the system bars.
            windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    private fun setArrowKeys(preferenceObject: SharedPreferences, binding: ActivityMainBinding) {
        val showArrowKeys = preferenceObject.getBoolean("showArrowKeys",true)
        if (showArrowKeys) {
            val arrowSize = preferenceObject.getInt("arrowSize", 65).toFloat()
            binding.upBtn.textSize = arrowSize
            binding.downBtn.textSize = arrowSize
            binding.upBtn.visibility = View.VISIBLE
            binding.downBtn.visibility = View.VISIBLE
        }
        else {
            binding.upBtn.visibility = View.GONE
            binding.downBtn.visibility = View.GONE
        }
    }
    private fun createWakeButton() {
        wakeBtn = TextView(activity)
        val spannable = SpannableString("Break")
        spannable.setSpan(UnderlineSpan(), 0, spannable.length, 0)
        wakeBtn.text = spannable
        wakeBtn.textSize = fontSize
        wakeBtn.setTextColor(theme.errorTextColor)
        wakeBtn.setOnClickListener {
            sleepTimer?.cancel()
            isSleeping = false
            binding.terminalOutput.removeView(wakeBtn)
            output("Yantra Launcher awakened mid-sleep (~_^)", theme.errorTextColor, null)
            binding.cmdInput.isEnabled = true
            executeCommandsInQueue()
        }
    }
    fun executeCommandsInQueue() {
        while (commandQueue.isNotEmpty() && !isSleeping) {
            val cmdToExecute = commandQueue.removeFirst()
            handleCommand(cmdToExecute)
        }
    }
    private fun createTouchListeners() {
        binding.scrollView.setGestureListenerCallback((activity as MainActivity))
        // for keyboard open
        binding.inputLineLayout.setOnClickListener {
            requestCmdInputFocusAndShowKeyboard(activity, binding)
        }
    }

    private fun getAliases(): MutableList<Alias> {
        //get alias list from shared preferences
        val defaultStringSet = mutableSetOf<String>()
        for (i in DEFAULT_ALIAS_LIST.indices) {
            defaultStringSet.add(DEFAULT_ALIAS_LIST[i].key + "=" + DEFAULT_ALIAS_LIST[i].value)
        }
        val aliasList = preferenceObject.getStringSet("aliasList", defaultStringSet)?.toMutableList()
        val aliasList2 = mutableListOf<Alias>() //convert to list of list
        for (i in aliasList!!.indices) {
            aliasList2.add(Alias(aliasList[i].split("=")[0],aliasList[i].split("=").drop(1).joinToString("=")))
        }
        return aliasList2
    }
    private fun checkAliasNames() {
        val commandNames = commands.keys
        for (i in aliasList.indices) {
            if (commandNames.contains(aliasList[i].key)) {
                output("--> Alias name cannot be an existing command name. Hence, alias '${aliasList[i].key}' needs to be unaliased to use the '${aliasList[i].key}' command.", theme.warningTextColor, null)
            }
        }
    }
    private fun incrementNumOfCommandsEntered(
        preferenceObject: SharedPreferences,
        preferenceEditObject: SharedPreferences.Editor
    ) {
        val n = preferenceObject.getLong("numOfCmdsEntered",0)
        preferenceEditObject.putLong("numOfCmdsEntered",n+1).apply()
    }
    fun output(text: String, color: Int, style: Int?, markdown: Boolean = false) {
        val t = TextView(activity)
        if (markdown) {
            t.setFont(typeface, null, color, fontSize)
            val markwon = Markwon.create(activity)
            markwon.setMarkdown(t, text)
        }
        else {
            t.setFont(typeface, style, color, fontSize)
            t.text = text
        }
        t.setTextIsSelectable(true)
        activity.runOnUiThread {
            binding.terminalOutput.addView(t)
        }
        // if error then vibrate
        if (color == theme.errorTextColor && vibrationPermission) {
            vibrate(activity = activity)
        }
    }
    fun setPromptText() {
        if (preferenceObject.getBoolean("showCurrentFolderInPrompt", false) && !workingDir.isEmpty()) {
            val splitOfWorkingDir = workingDir.split("/")
            if (preferenceObject.getBoolean("useModernPromptDesign", false)) {
                username.text =
                    "${getUserName(preferenceObject)}/../${splitOfWorkingDir[splitOfWorkingDir.size - 1]}"
                return
            }
            username.text =
                "${getUserNamePrefix(preferenceObject)}${getUserName(preferenceObject)}/../${splitOfWorkingDir[splitOfWorkingDir.size - 1]}>"
            return
        }

        if (preferenceObject.getBoolean("useModernPromptDesign", false)) {
            username.text = getUserName(preferenceObject)
            return
        }

        username.text =
            "${getUserNamePrefix(preferenceObject)}${getUserName(preferenceObject)}>"
    }
    private fun getCommandInstance(commandName: String): BaseCommand? {
        val cachedCommand = commandCache.find { it.containsKey(commandName) }

        if (cachedCommand != null) {
            commandCache.remove(cachedCommand)
            commandCache.add(0, cachedCommand)
            return cachedCommand[commandName]
        }
        else {
            if (commandCache.size >= cacheSize) {
                commandCache.removeAt(commandCache.size - 1)
            }

            val commandClass = commands[commandName]
            if (commandClass != null) {
                val newCommand = mapOf(
                    commandName to commandClass.getDeclaredConstructor(Terminal::class.java)
                        .newInstance(this)
                )
                commandCache.add(0, newCommand)
                return newCommand[commandName]
            }
            return null
        }
    }
    private fun finishInitialization() {
        printIntro()
        if (isPro(activity)) {
            Thread {
                val initList = getInit(preferenceObject)
                runInitTasks(initList, preferenceObject, this@Terminal)
            }.start()
        }
        initialized = true
    }

    private fun printIntro() {
        output("${activity.applicationInfo.loadLabel(activity.packageManager)} (v${BuildConfig.VERSION_NAME}) on ${Build.MANUFACTURER} ${Build.MODEL}",theme.resultTextColor, Typeface.BOLD)
        output(activity.getString(R.string.intro_help_or_community), theme.resultTextColor, Typeface.BOLD)
        output("==================",theme.resultTextColor, Typeface.BOLD)
    }

    fun cmdDown() {
        binding.cmdInput.requestFocus()
        if (cmdHistoryCursor<(cmdHistory.size-1)) {
            cmdHistoryCursor++
            binding.cmdInput.setText(cmdHistory[cmdHistoryCursor])
            binding.cmdInput.setSelection(binding.cmdInput.text!!.length)
            requestCmdInputFocusAndShowKeyboard(activity, binding)
        }
    }
    fun cmdUp() {
        binding.cmdInput.requestFocus()
        if (cmdHistoryCursor>0) {
            cmdHistoryCursor--
            binding.cmdInput.setText(cmdHistory[cmdHistoryCursor])
            binding.cmdInput.setSelection(binding.cmdInput.text!!.length)
            requestCmdInputFocusAndShowKeyboard(activity, binding)
        }
    }
    fun handleCommand(command: String, isAlias: Boolean = false, logCmd: Boolean = true) {
        if (isSleeping) {
            commandQueue.add(command)
            return
        }
        val commandName = command.trim().split(" ").firstOrNull()
        if (!isAlias) {
            if (logCmd && !NO_LOG_COMMANDS.contains(commandName?.lowercase())) {
                if (preferenceObject.getBoolean("useModernPromptDesign", false)) {
                    addChatBubble(getUserName(preferenceObject), command)
                } else {
                    output(getUserNamePrefix(preferenceObject)+getUserName(preferenceObject)+"> $command", theme.commandColor, null)
                }
            }
            if (command.trim()!="") {
                cmdHistory.add(command)
                cmdHistoryCursor = cmdHistory.size
                incrementNumOfCommandsEntered(preferenceObject, preferenceObject.edit())
                showRatingAndCommunityPopups(preferenceObject, preferenceObject.edit(), activity)
                marketProVersion(this@Terminal, preferenceObject)
            }
        }
        commandName?.let { _ ->
            aliasList.find { it.key == commandName }?.let { alias ->
                val newCommand = command.replaceFirst(commandName, alias.value)
                handleCommand(newCommand, true)
                return@handleCommand
            }
        }
        val commandInstance = getCommandInstance(commandName.toString().lowercase())
        if (commandInstance != null) {
            commandInstance.execute(command.trim())
        }
        else {
            if (command.trim() == "") return
            // find most similar command and recommend
            var maxScore = 0.0
            var matchingName = "help"
            for (cmd in commands.keys) {
                val score = findSimilarity(cmd, commandName)
                if (score > maxScore) {
                    matchingName = cmd
                    maxScore = score
                }
            }
            output("$commandName is not a recognized command or alias. Did you mean $matchingName?", theme.errorTextColor, null)
        }
    }

    private fun addChatBubble(username: String, command: String) {
        val mainLayout = LinearLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 2.dpToPx(), 0, 2.dpToPx())
                marginEnd = 8.dpToPx()
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(activity, R.drawable.round_corner_blue)
        }

        val frameLayout = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val whiteBackground = View(activity).apply {
            layoutParams = FrameLayout.LayoutParams(40.spToPX(), FrameLayout.LayoutParams.MATCH_PARENT)
            background = ContextCompat.getDrawable(context, R.drawable.round_corner_white)
        }
        frameLayout.addView(whiteBackground)

        val imageView = ImageView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(24.dpToPx(), 24.dpToPx()).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = 8.dpToPx()
            }
            setImageResource(R.drawable.ic_android)
        }
        frameLayout.addView(imageView)

        mainLayout.addView(frameLayout)

        val whiteTriangle = View(activity).apply {
            layoutParams = LinearLayout.LayoutParams(24.dpToPx(), 24.dpToPx())
            background = ContextCompat.getDrawable(activity, R.drawable.right_triangle)
            backgroundTintList = ColorStateList.valueOf(Color.WHITE)
        }
        mainLayout.addView(whiteTriangle)

        val usernameTextView = TextView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (-8).dpToPx()
                gravity = Gravity.CENTER_VERTICAL
            }
            text = username
            setTextColor(theme.commandColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
            Typeface.createFromAsset(activity.assets, "fonts/source_code_pro.ttf")?.let { setTypeface(it, Typeface.BOLD) }
        }

        mainLayout.addView(usernameTextView)

        val finalLayout = LinearLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }
        finalLayout.addView(mainLayout)

        val bottomTriangle = View(activity).apply {
            layoutParams = LinearLayout.LayoutParams(24.dpToPx(), 24.dpToPx()).apply {
                setMargins(0, 2.dpToPx(), 0, 2.dpToPx())
                marginStart = (-8).dpToPx()
            }
            background = ContextCompat.getDrawable(activity, R.drawable.right_triangle)
        }
        finalLayout.addView(bottomTriangle)

        val commandTextView = TextView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            text = command
            setTextColor(theme.commandColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
            Typeface.createFromAsset(activity.assets, "fonts/source_code_pro.ttf")?.let { setTypeface(it) }
        }

        finalLayout.addView(commandTextView)

        activity.runOnUiThread {
            binding.terminalOutput.addView(finalLayout)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * activity.resources.displayMetrics.density).toInt()
    }

    private fun Int.spToPX(): Int {
        val scaledDensity = activity.resources.displayMetrics.scaledDensity
        return (this * scaledDensity).roundToInt()
    }

}

private fun TextView.setFont(typeface: Typeface?, style: Int?, state: Int, fontSize: Float) {
    if (style == null) {
        this.typeface = typeface
    }
    else {
        this.setTypeface(typeface, style)
    }
    this.setTextColor(state)
    this.textSize = fontSize
}
