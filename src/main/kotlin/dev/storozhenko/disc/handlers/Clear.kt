package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class Clear : CommandHandler() {
    override fun handleInternal(context: EventContext) {
        context.queue.clear()
        context.manager.audioPlayer.stopTrack()
        context.event.reply("Ок").queue()
    }

    override fun command(): SlashCommandData {
        return Commands.slash("clear", "Очищает очередь и останавливает воспроизведение")
    }
}