package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext

class RepeatOne : CommandHandler {
    override fun handle(context: EventContext) {
        context.manager.listener.repeatOne = context.manager.listener.repeatOne.not()
        context.event.message.reply("Режим повтора одного трека ${context.manager.listener.repeatOne}")
            .queue()
    }
}