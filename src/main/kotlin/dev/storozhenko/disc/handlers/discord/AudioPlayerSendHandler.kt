package dev.storozhenko.disc.handlers.discord

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer

class AudioPlayerSendHandler(private val audioPlayer: AudioPlayer) : AudioSendHandler {

    private val buffer: ByteBuffer = ByteBuffer.allocate(1024)
    private val frameBuffer: MutableAudioFrame = MutableAudioFrame().apply { setBuffer(buffer) }

    override fun canProvide(): Boolean {
        return audioPlayer.provide(frameBuffer)
    }

    override fun provide20MsAudio(): ByteBuffer {
        buffer.flip()
        return buffer
    }

    override fun isOpus(): Boolean {
        return true
    }
}