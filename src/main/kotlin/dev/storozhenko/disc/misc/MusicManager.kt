package dev.storozhenko.disc.misc

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import dev.lavalink.youtube.YoutubeAudioSourceManager
import dev.lavalink.youtube.clients.Web
import dev.storozhenko.disc.handlers.discord.AudioPlayerSendHandler
import net.dv8tion.jda.api.entities.Guild
import java.util.Queue

class MusicManager(
    private val poToken: String,
    private val visitorData: String
) {
    val playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
        .apply {
            registerSourceManager(YoutubeAudioSourceManager())
            @Suppress("DEPRECATION")
            AudioSourceManagers.registerRemoteSources(this, com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager::class.java)
            Web.setPoTokenAndVisitorData(poToken, visitorData)
        }
    private val musicManagers: MutableMap<Long, GuildMusicManager> = HashMap()

    init {
        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)
    }

    fun getGuildMusicManager(guild: Guild, queue: Queue<AudioTrack>): GuildMusicManager {
        val guildId = guild.idLong
        var musicManager = musicManagers[guildId]

        if (musicManager == null) {
            musicManager = GuildMusicManager(playerManager, queue)
            musicManagers[guildId] = musicManager
        }

        guild.audioManager.sendingHandler = musicManager.sendHandler

        return musicManager
    }

    inner class GuildMusicManager(playerManager: AudioPlayerManager, queue: Queue<AudioTrack>) {
        val listener = AudioEventListener(queue)
        val audioPlayer: AudioPlayer = playerManager.createPlayer().apply { addListener(listener) }
        val sendHandler: AudioPlayerSendHandler = AudioPlayerSendHandler(audioPlayer)
    }

    class AudioEventListener(private val queue: Queue<AudioTrack>) : AudioEventAdapter() {
        var repeatOne = false
        override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason?) {
            if (endReason == AudioTrackEndReason.REPLACED) return
            if (endReason?.mayStartNext == true && repeatOne) {
                player.playTrack(track.makeClone())
            } else if (queue.isNotEmpty()) {
                player.playTrack(queue.poll().makeClone())
            }
        }
    }
}