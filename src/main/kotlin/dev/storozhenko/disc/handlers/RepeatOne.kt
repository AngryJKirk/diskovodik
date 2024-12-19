package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext
import dev.storozhenko.disc.misc.bold
import dev.storozhenko.disc.misc.friendly

class RepeatOne : CommandHandler() {
    override fun handleInternal(context: EventContext) {
        context.manager.listener.repeatOne = context.manager.listener.repeatOne.not()
        val newState = context.manager.listener.repeatOne.friendly().bold()
        context.event.reply("Режим повтора одного трека $newState").queue()
    }
}