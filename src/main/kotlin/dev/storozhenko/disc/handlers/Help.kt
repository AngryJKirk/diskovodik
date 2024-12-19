package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext
import dev.storozhenko.disc.misc.inlineCode
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class Help : CommandHandler() {
    override fun handleInternal(context: EventContext) {
        context.event.jda.retrieveCommands().queue { commands ->
            val helpMessage = commands
                .sortedBy { it.name.length }
                .joinToString("\n") { cmd ->
                    "/${cmd.name}".inlineCode() + " - ${cmd.description}"
                }
            context.event.reply(helpMessage).setEphemeral(true).queue()
        }
    }

    override fun command(): SlashCommandData {
        return Commands.slash("help", "Помогите")
    }
}