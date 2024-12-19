package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class Skip : CommandHandler() {
    override fun handleInternal(context: EventContext) {
        if (context.queue.isNotEmpty()) {
            context.manager.audioPlayer.playTrack(context.queue.poll().makeClone())
        } else {
            context.manager.audioPlayer.stopTrack()
        }
        context.event.reply("Скипаем").setEphemeral(true).queue()
    }

    override fun command(): SlashCommandData {
        return Commands.slash("skip", "Пропускает текущий трек")
    }
}