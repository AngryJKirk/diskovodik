package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext
import dev.storozhenko.disc.misc.bold
import dev.storozhenko.disc.misc.friendly
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class RepeatOne : CommandHandler() {
    override fun handleInternal(context: EventContext) {
        context.manager.listener.repeatOne = context.manager.listener.repeatOne.not()
        val newState = context.manager.listener.repeatOne.friendly().bold()
        context.event.reply("Режим повтора одного трека $newState").queue()
    }

    override fun command(): SlashCommandData {
        return Commands.slash("repeat_one", "Ставит репит текущего трека")
    }
}