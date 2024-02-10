package dev.storozhenko.disc.misc

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData


fun createButtons(queue: Collection<AudioTrack>, prefix: String): MessageCreateData {
    val messageCreateBuilder = MessageCreateBuilder()
    queue.take(25).map {
        Button.primary("$prefix|${it.identifier}", it.info.title.take(79))
    }.chunked(5).forEach { messageCreateBuilder.addActionRow(it) }
    return messageCreateBuilder.build()
}