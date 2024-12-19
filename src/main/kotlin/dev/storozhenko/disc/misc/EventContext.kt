package dev.storozhenko.disc.misc

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.util.Queue

class EventContext(
    val event: SlashCommandInteractionEvent,
    val queue: Queue<AudioTrack>,
    val manager: MusicManager.GuildMusicManager
)