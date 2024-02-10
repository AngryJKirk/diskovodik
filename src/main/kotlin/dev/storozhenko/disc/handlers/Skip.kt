package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext

class Skip : CommandHandler {
    override fun handle(context: EventContext) {
        if (context.queue.isNotEmpty()) {
            context.manager.audioPlayer.playTrack(context.queue.poll().makeClone())
        } else {
            context.manager.audioPlayer.stopTrack()
        }
    }
}