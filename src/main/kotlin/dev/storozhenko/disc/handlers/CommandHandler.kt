package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext

interface CommandHandler {

    fun handle(context: EventContext)

}