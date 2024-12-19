package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext
import dev.storozhenko.disc.misc.bold
import dev.storozhenko.disc.misc.createButtons
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.time.Duration

class List : CommandHandler() {
    override fun handleInternal(context: EventContext) {
        val duration = Duration.ofMillis(context.queue.sumOf { it.duration })
        val durationMessage = "Длина треков в очереди ${toText(duration)}\n"
        context.event.reply(durationMessage).queue()
        context.event.reply(createButtons(context.queue, "remove")).queue()
    }

    override fun command(): SlashCommandData {
        return Commands.slash("list", "Показывает текущую очередь песен с удалением по нажатию")
    }

    private fun toText(duration: Duration) =
        "${duration.toMinutes()} м. ${duration.toSeconds() % 60} c.".bold()
}