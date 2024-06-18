package com.coderGtm.yantra.commands.run

import com.coderGtm.yantra.R
import com.coderGtm.yantra.blueprints.BaseCommand
import com.coderGtm.yantra.LuaExecutor
import com.coderGtm.yantra.getScripts
import com.coderGtm.yantra.models.CommandMetadata
import com.coderGtm.yantra.terminal.Terminal

class Command(terminal: Terminal) : BaseCommand(terminal) {
    override val metadata = CommandMetadata(
        name = "run",
        helpTitle = terminal.activity.getString(R.string.cmd_run_title),
        description = terminal.activity.getString(R.string.cmd_run_help)
    )

    override fun execute(command: String) {
        val args = command.split(" ")
        if (args.size < 2) {
            output(terminal.activity.getString(R.string.specify_script_to_run), terminal.theme.errorTextColor)
            return
        }
        if (args.size > 2) {
            if (args[1].trim() == "-lua") {
                if (args.size > 3) {
                    terminal.output("invalid usage", terminal.theme.errorTextColor, null)
                    return
                }
                val luaScriptName = args[2].trim()
                val scriptBody = terminal.preferenceObject.getString("script_$luaScriptName","") ?: ""
                val luaExecutor = LuaExecutor(luaScriptName, terminal)

                Thread {
                    luaExecutor.execute(scriptBody)
                }.start()
                return
            }
        }
        val rcvdScriptName = args[1]
        val scripts = getScripts(terminal.preferenceObject)

        if (rcvdScriptName in scripts) {
            val scriptBody = terminal.preferenceObject.getString("script_$rcvdScriptName","") ?: ""
            val cmdsInScript = scriptBody.split("\n")
            cmdsInScript.forEach {
                terminal.handleCommand(it.trim())
            }
        }
        else {
            output(terminal.activity.getString(R.string.script_not_found, rcvdScriptName),terminal.theme.errorTextColor)
            return
        }
    }
}