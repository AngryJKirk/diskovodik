package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.getLogger
import dev.storozhenko.disc.misc.EventContext

abstract class CommandHandler {

    private val log = getLogger()
    fun handle(context: EventContext) {
        log.info("Handle: ${this::class.simpleName}, message: ${context.event.options}")
        handleInternal(context)
    }

    abstract fun handleInternal(context: EventContext)

}