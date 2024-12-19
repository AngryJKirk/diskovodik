package dev.storozhenko.disc.handlers.discord

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.storozhenko.disc.getLogger
import dev.storozhenko.disc.misc.bold
import dev.storozhenko.disc.misc.spoiler
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.*

open class SingleTrackLoadHandler(private val queue: Queue<AudioTrack>, private val event: MessageReceivedEvent) :
    AudioLoadResultHandler {
    private val log = getLogger()
    override fun trackLoaded(track: AudioTrack) {
        event.message.reply("Добавил ${track.info.title.bold()} в очередь.").queue()
        queue.add(track)
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        try {

            if (playlist.isSearchResult) {
                trackLoaded(playlist.tracks.first())
                return
            }
            event.message.reply("Добавил ${playlist.tracks.size.toString().bold()} песенок в очередь.").queue()
            queue.addAll(playlist.tracks)
        } catch (e: Exception) {
            log.error("playlistLoaded failed", e)
        }
    }

    override fun noMatches() {
        event.message.reply("Не нашел ничего").queue()
    }

    override fun loadFailed(exception: FriendlyException) {
        log.error("Search has failed", exception)
        event.message.reply(
            "Хуйня какая-то, не работает: ${exception.message}. Инфа для джавистов:\n" +
                    (exception.cause?.message?.spoiler() + "\n" + exception.cause?.stackTrace?.joinToString("\n")).spoiler()
        ).queue()
    }
}