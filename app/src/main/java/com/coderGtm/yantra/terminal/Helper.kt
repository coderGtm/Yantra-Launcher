package com.coderGtm.yantra.terminal

import android.app.WallpaperManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.coderGtm.yantra.R
import com.coderGtm.yantra.Themes
import com.coderGtm.yantra.models.Alias
import com.coderGtm.yantra.models.Theme
import com.coderGtm.yantra.requestCmdInputFocusAndShowKeyboard
import com.coderGtm.yantra.setSystemWallpaper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.regex.Pattern

fun showSuggestions(
    rawInput: String,
    getPrimarySuggestions: Boolean,
    getSecondarySuggestions: Boolean,
    terminal: Terminal
) {
    Thread {
        terminal.activity.runOnUiThread {
            terminal.binding.suggestionsTab.removeAllViews()
        }
        val input = rawInput.trim()
        val suggestions = ArrayList<String>()
        val args = input.split(" ")
        var overrideLastWord = false
        var isPrimary = true
        var executeOnTapViable = true

        if ((args.isEmpty() || (args.size == 1 && terminal.binding.cmdInput.text.toString().lastOrNull() != ' ')) && getPrimarySuggestions) {
            overrideLastWord = true
            val regex = Regex(Pattern.quote(args[0]), RegexOption.IGNORE_CASE)
            val allPrimarySuggestions: MutableSet<String> = terminal.commands.keys.toMutableSet()
            terminal.aliasList.forEach {
                allPrimarySuggestions.add(it.key)
            }
            for (ps in allPrimarySuggestions) {
                if (regex.containsMatchIn(ps)) {
                    suggestions.add(ps)
                }
            }
        }
        else if ((args.size > 1 || (args.size == 1 && terminal.binding.cmdInput.text.toString().lastOrNull() == ' ')) && getSecondarySuggestions) {
            // check for alias
            val effectivePrimaryCmd: String
            val isAliasCmd = terminal.aliasList.any { it.key == args[0] }
            effectivePrimaryCmd = if (isAliasCmd) {
                terminal.aliasList.first { it.key == args[0] }.value
            } else {
                args[0].lowercase()
            }
            val reg = input.removePrefix(args[0]).trim()
            if (effectivePrimaryCmd == "launch") {
                if (!terminal.appListFetched) {
                    return@Thread
                }
                val candidates = terminal.appList.map { it.appName }.toMutableList()
                candidates.add(0, "-p")
                if (args.size>1) {
                    //search using regex
                    overrideLastWord = true

                    val regex = Regex(Pattern.quote(reg), RegexOption.IGNORE_CASE)
                    for (app in candidates) {
                        if (regex.containsMatchIn(app) && !suggestions.contains(app)) {
                            if (app.substring(0, reg.length).lowercase() == reg && reg.isNotEmpty()){
                                suggestions.add(0, app)
                                continue
                            }
                            suggestions.add(app)
                        }
                    }
                }
                else {
                    for (app in candidates) {
                        if (!suggestions.contains(app)) {
                            suggestions.add(app)
                        }
                    }
                }
                isPrimary = false
            }
            else if (effectivePrimaryCmd == "uninstall") {
                if (!terminal.appListFetched) {
                    return@Thread
                }
                if (args.size>1) {
                    //search using regex
                    overrideLastWord = true
                    val regex = Regex(Pattern.quote(input.removePrefix(args[0]).trim()), RegexOption.IGNORE_CASE)
                    for (app in terminal.appList) {
                        if (regex.containsMatchIn(app.appName) && !suggestions.contains(app.appName)) {
                            if (app.appName.substring(0, reg.length).lowercase() == reg && reg.isNotEmpty()){
                                suggestions.add(0, app.appName)
                                continue
                            }
                            suggestions.add(app.appName)
                        }
                    }
                }
                else {
                    for (app in terminal.appList) {
                        if (!suggestions.contains(app.appName)) {
                            suggestions.add(app.appName)
                        }
                    }
                }
                isPrimary = false
            }
            else if (effectivePrimaryCmd == "info") {
                if (!terminal.appListFetched) {
                    return@Thread
                }
                val candidates = terminal.appList.map { it.appName }.toMutableList()
                candidates.add(0, "-p")
                if (args.size>1) {
                    //search using regex
                    overrideLastWord = true
                    val regex = Regex(Pattern.quote(input.removePrefix(args[0]).trim()), RegexOption.IGNORE_CASE)
                    for (app in candidates) {
                        if (regex.containsMatchIn(app) && !suggestions.contains(app)) {
                            if (app.substring(0, reg.length).lowercase() == reg && reg.isNotEmpty()){
                                suggestions.add(0, app)
                                continue
                            }
                            suggestions.add(app)
                        }
                    }
                }
                else {
                    for (app in candidates) {
                        if (!suggestions.contains(app)) {
                            suggestions.add(app)
                        }
                    }
                }
                isPrimary = false
            }
            else if (effectivePrimaryCmd == "call") {
                if (!terminal.contactsFetched) {
                    terminal.activity.runOnUiThread {
                        terminal.binding.suggestionsTab.removeAllViews()
                    }
                    val tv = TextView(terminal.activity)
                    tv.text = terminal.activity.getString(R.string.contacts_not_fetched_yet)
                    tv.setTextColor(terminal.theme.suggestionTextColor)
                    //italics
                    tv.setTypeface(terminal.typeface, Typeface.BOLD_ITALIC)
                    terminal.activity.runOnUiThread {
                        terminal.binding.suggestionsTab.addView(tv)
                    }
                    return@Thread
                }
                else if (args.size>1) {
                    //search using regex
                    overrideLastWord = true
                    val regex = Regex(Pattern.quote(input.removePrefix(args[0]).trim()), RegexOption.IGNORE_CASE)
                    for (contact in terminal.contactNames) {
                        if (regex.containsMatchIn(contact)) {
                            suggestions.add(contact)
                        }
                    }
                }
                else {
                    for (contact in terminal.contactNames) {
                        if (!suggestions.contains(contact)) {
                            suggestions.add(contact)
                        }
                    }
                }
                isPrimary = false
            }
            else if (effectivePrimaryCmd == "list") {
                if (args.size > 1) {
                    overrideLastWord = true
                }
                val regex = Regex(Pattern.quote(input.removePrefix(args[0]).trim()), RegexOption.IGNORE_CASE)
                val listArgs = listOf("apps","themes","contacts")
                for (arg in listArgs) {
                    if (regex.containsMatchIn(arg)) {
                        suggestions.add(arg)
                    }
                }
                isPrimary = false
            }
            else if (effectivePrimaryCmd == "help") {
                if (args.size>1) {
                    overrideLastWord = true
                }
                val regex = Regex(Pattern.quote(input.removePrefix(args[0]).trim()), RegexOption.IGNORE_CASE)
                terminal.commands.keys.filterTo(suggestions) { regex.containsMatchIn(it) }
                isPrimary = false

            }
            else if (effectivePrimaryCmd == "alias") {
                if (args.size > 1) {
                    overrideLastWord = true
                }
                val regex = Regex(Pattern.quote(input.removePrefix(args[0]).trim()), RegexOption.IGNORE_CASE)
                if (regex.containsMatchIn("-1")) {
                    suggestions.add("-1")
                }
                isPrimary = false
            }
            else if (effectivePrimaryCmd == "unalias") {
                if (args.size > 1) {
                    overrideLastWord = true
                }
                val regex = Regex(Pattern.quote(input.removePrefix(args[0]).trim()), RegexOption.IGNORE_CASE)
                val unaliasArgs = terminal.aliasList.toMutableList()
                unaliasArgs.add(0, Alias("-1",""))
                unaliasArgs
                    .filter { regex.containsMatchIn(it.key) }
                    .mapTo(suggestions) { it.key }
                isPrimary = false
            }
            else if (effectivePrimaryCmd == "theme") {
                if (args.size > 1) {
                    overrideLastWord = true
                }
                val regex = Regex(Pattern.quote(input.removePrefix(args[0]).trim()), RegexOption.IGNORE_CASE)
                val themeArgs = mutableListOf<String>()
                Themes.entries.forEach { themeArgs.add(it.name) }
                themeArgs.filterTo(suggestions) { regex.containsMatchIn(it) }
                isPrimary = false
            }
            else if (effectivePrimaryCmd == "sysinfo") {
                if (args.size > 1) {
                    overrideLastWord = true
                }
                val regex = Regex(Pattern.quote(input.removePrefix(args[0]).trim()), RegexOption.IGNORE_CASE)
                val sysInfoArgs= listOf("-os", "-host", "-kernel", "-uptime", "-apps", "-terminal", "-font", "-resolution", "-theme", "-cpu", "-memory")
                for (arg in sysInfoArgs) {
                    if (regex.containsMatchIn(arg)) {
                        suggestions.add(arg)
                    }
                }
                isPrimary = false
            }
        }
        suggestions.forEach { sug ->
            if ((isPrimary && (input.trim() == sug.trim())) || (!isPrimary && (input.removePrefix(args[0]).trim() == sug.trim()))) {
                return@forEach
            }
            val btn = Button(terminal.activity)
            btn.text = sug
            btn.setTextColor(terminal.theme.suggestionTextColor)
            if (terminal.preferenceObject.getBoolean("fontpack___purchased",true)) {
                btn.setTypeface(terminal.typeface, Typeface.BOLD)
            }
            else {
                btn.setTypeface(null, Typeface.BOLD)
            }
            btn.background = Color.TRANSPARENT.toDrawable()
            //set start and end margins
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(20, 0, 20, 0)
            btn.layoutParams = params


            btn.setOnClickListener {
                if (overrideLastWord) {
                    val newCmd = input.substring(0, input.length-args[args.size-1].length) + sug + " "
                    terminal.binding.cmdInput.setText(newCmd)
                }
                else {
                    terminal.binding.cmdInput.setText("$input $sug ")
                }
                terminal.binding.cmdInput.setSelection(terminal.binding.cmdInput.text!!.length)
                requestCmdInputFocusAndShowKeyboard(terminal.activity, terminal.binding)
                terminal.binding.suggestionsTab.removeView(it)

                val actOnSuggestionTap = terminal.preferenceObject.getBoolean("actOnSuggestionTap", false)
                if (!isPrimary && actOnSuggestionTap && executeOnTapViable) {
                    terminal.handleCommand(terminal.binding.cmdInput.text.toString().trim())
                    terminal.binding.cmdInput.setText("")
                }
            }
            if (isPrimary) {
                btn.setOnLongClickListener {
                    try {
                        val commandClass = terminal.commands[sug]
                        if (commandClass != null) {
                            val cmdMetadata = commandClass.getDeclaredConstructor(Terminal::class.java).newInstance(terminal).metadata
                            MaterialAlertDialogBuilder(terminal.activity, R.style.Theme_AlertDialog)
                                .setTitle(cmdMetadata.helpTitle)
                                .setMessage(cmdMetadata.description)
                                .setPositiveButton(terminal.activity.getString(R.string.ok)) { helpDialog, _ ->
                                    helpDialog.dismiss()
                                }
                                    .show()
                        }
                    }
                    catch (e: Exception) {}
                    true
                }
            }
            terminal.activity.runOnUiThread {
                terminal.binding.suggestionsTab.addView(btn)
            }
        }
    }.start()
}

fun setWallpaperIfNeeded(preferenceObject: SharedPreferences, applicationContext: Context, curTheme: Theme, ) {
    if (preferenceObject.getBoolean("defaultWallpaper",true)) {
        val wallpaperManager = WallpaperManager.getInstance(applicationContext)
        val colorDrawable = ColorDrawable(curTheme.bgColor)
        setSystemWallpaper(wallpaperManager, colorDrawable.toBitmap(applicationContext.resources.displayMetrics.widthPixels, applicationContext.resources.displayMetrics.heightPixels))
    }
}