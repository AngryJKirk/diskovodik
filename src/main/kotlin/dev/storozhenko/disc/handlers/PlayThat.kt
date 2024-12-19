package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext
import dev.storozhenko.disc.misc.createButtons
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class PlayThat : CommandHandler() {
    override fun handleInternal(context: EventContext) {
        context.event.reply(createButtons(context.queue, "play")).queue()
    }

    override fun command(): SlashCommandData {
        return Commands.slash("play_that", "Проигрывает очередь")
    }
}