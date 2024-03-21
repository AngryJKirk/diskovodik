package dev.storozhenko.disc.handlers

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.storozhenko.disc.misc.EventContext
import dev.storozhenko.disc.misc.MusicManager
import dev.storozhenko.disc.handlers.discord.SingleTrackLoadHandler
import dev.storozhenko.disc.misc.createButtons
import dev.storozhenko.disc.urlRegex
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.Queue

object SearchResults {
    val searchResults: MutableMap<Long, Collection<AudioTrack>> = mutableMapOf()
}

class Search(private val musicManager: MusicManager) : CommandHandler() {
    override fun handleInternal(context: EventContext) {
        var query = context.event.message.contentRaw.substringAfter(" ")
        if (!urlRegex.matches(query)) {
            query = "ytsearch:$query"
        }
        musicManager.playerManager.loadItemOrdered(
            context.manager, query,
            SearchLoadHandler(context.event, context.queue)
        )
    }
}

class SearchLoadHandler(private val event: MessageReceivedEvent, queue: Queue<AudioTrack>) :
    SingleTrackLoadHandler(queue, event) {

    override fun playlistLoaded(playlist: AudioPlaylist) {
        SearchResults.searchResults[event.guild.idLong] = playlist.tracks
        event.message.reply(createButtons(playlist.tracks, "search")).queue()
    }
}