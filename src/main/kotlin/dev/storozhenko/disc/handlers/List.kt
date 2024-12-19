package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext
import dev.storozhenko.disc.misc.bold
import dev.storozhenko.disc.misc.createButtons
import java.time.Duration

class List : CommandHandler() {
    override fun handleInternal(context: EventContext) {
        val duration = Duration.ofMillis(context.queue.sumOf { it.duration })
        val durationMessage = "Длина треков в очереди ${toText(duration)}\n"
        context.event.reply(durationMessage).queue()
        context.event.reply(createButtons(context.queue, "remove")).queue()
    }

    private fun toText(duration: Duration) =
        "${duration.toMinutes()} м. ${duration.toSeconds() % 60} c.".bold()
}