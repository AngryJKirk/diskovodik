package dev.storozhenko.disc.misc

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.Queue

class EventContext(
    val event: MessageReceivedEvent,
    val queue: Queue<AudioTrack>,
    val manager: MusicManager.GuildMusicManager
    ) {
    val content: String = event.message.contentRaw

    fun forCommand(commandPrefix: String) = content.startsWith(commandPrefix)
}