package dev.storozhenko.disc.handlers.discord

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.Queue

open class SingleTrackLoadHandler(private val queue: Queue<AudioTrack>, private val event: MessageReceivedEvent) :
    AudioLoadResultHandler {
    override fun trackLoaded(track: AudioTrack) {
        event.message.reply("Добавил ${track.info.title} в очередь.").queue()
        queue.add(track)
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        if (playlist.isSearchResult) {
            trackLoaded(playlist.tracks.first())
            return
        }
        event.message.reply("Добавил ${playlist.tracks.size} песенок в очередь.").queue()
        queue.addAll(playlist.tracks)
    }

    override fun noMatches() {
        event.message.reply("Не нашел ничего").queue()
    }

    override fun loadFailed(exception: FriendlyException) {
        event.message.reply("Хуйня какая-то, не работает: ${exception.message}").queue()
    }
}