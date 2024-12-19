package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.getLogger
import dev.storozhenko.disc.misc.EventContext
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

abstract class CommandHandler {

    private val log = getLogger()
    fun handle(context: EventContext) {
        log.info("Handle: ${this::class.simpleName}, message: ${context.event.options}")
        handleInternal(context)
    }

    abstract fun handleInternal(context: EventContext)

    abstract fun command(): SlashCommandData

}