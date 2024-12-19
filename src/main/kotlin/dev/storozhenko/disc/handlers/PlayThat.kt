package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext
import dev.storozhenko.disc.misc.createButtons

class PlayThat : CommandHandler() {
    override fun handleInternal(context: EventContext) {
        context.event.reply(createButtons(context.queue, "play")).queue()
    }
}