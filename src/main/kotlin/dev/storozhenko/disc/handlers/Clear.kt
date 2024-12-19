package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext

class Clear : CommandHandler() {
    override fun handleInternal(context: EventContext) {
        context.queue.clear()
        context.manager.audioPlayer.stopTrack()
        context.event.reply("Ок").queue()
    }
}