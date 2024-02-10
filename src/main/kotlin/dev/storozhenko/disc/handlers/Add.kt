package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext
import dev.storozhenko.disc.misc.MusicManager
import dev.storozhenko.disc.handlers.discord.SingleTrackLoadHandler
import dev.storozhenko.disc.urlRegex

class Add(private val musicManager: MusicManager) : CommandHandler {

    override fun handle(context: EventContext) {
        context.content
            .replace("!add ", "")
            .split(",")
            .map { trackSearch -> if (urlRegex.matches(trackSearch).not()) "ytsearch:$trackSearch" else trackSearch }
            .forEach { trackSearch ->
                musicManager.playerManager.loadItemOrdered(
                    context.manager,
                    trackSearch,
                    SingleTrackLoadHandler(context.queue, context.event)
                )
            }
    }
}