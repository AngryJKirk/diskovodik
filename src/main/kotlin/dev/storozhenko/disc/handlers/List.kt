package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext
import dev.storozhenko.disc.misc.createButtons
import java.time.Duration

class List : CommandHandler() {
    override fun handleInternal(context: EventContext) {
        val duration = Duration.ofMillis(context.queue.sumOf { it.duration })
        val durationMessage = "Длина треков в очереди ${duration.toMinutes()} м. ${duration.toSeconds() % 60} c.\n"
        context.event.message.reply(durationMessage).queue()
        context.event.message.reply(createButtons(context.queue, "remove")).queue()
    }
}