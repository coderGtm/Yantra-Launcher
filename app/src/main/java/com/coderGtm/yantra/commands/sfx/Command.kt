package com.coderGtm.yantra.commands.sfx

import android.graphics.Typeface
import android.media.MediaPlayer
import com.coderGtm.yantra.R
import com.coderGtm.yantra.blueprints.BaseCommand
import com.coderGtm.yantra.models.CommandMetadata
import com.coderGtm.yantra.terminal.Terminal

class Command(terminal: Terminal) : BaseCommand(terminal) {
    override val metadata = CommandMetadata(
        name = "sfx",
        helpTitle = terminal.activity.getString(R.string.cmd_sfx_title),
        description = terminal.activity.getString(R.string.cmd_sfx_help),
    )

    override fun execute(command: String) {
        val args = command.split(" ")

        if (args.size == 1) {
            // play default sound
            val mediaPlayer = MediaPlayer.create(terminal.activity, R.raw.beep)
            mediaPlayer.start()
        } else {
            val sound = args[1]
            val files = terminal.activity.filesDir.listFiles()
            if (!files.isNullOrEmpty()) {
                for (file in files) {
                    if ((file.name == sound + ".mp3" || file.name == sound + ".wav" || file.name == sound + ".ogg") && file.isFile) {
                        output(terminal.activity.getString(R.string.playing_sound_effect, sound), terminal.theme.successTextColor, Typeface.ITALIC)
                        val mediaPlayer = MediaPlayer()
                        mediaPlayer.setDataSource(file.absolutePath)
                        mediaPlayer.prepare()
                        mediaPlayer.start()
                        return
                    }
                }
                output(terminal.activity.getString(R.string.sfx_not_found, sound), terminal.theme.errorTextColor, null)
            }
        }
    }
}