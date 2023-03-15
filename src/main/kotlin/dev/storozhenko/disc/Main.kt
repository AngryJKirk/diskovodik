package dev.storozhenko.disc

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.LinkedList
import java.util.Queue

private val log = LoggerFactory.getLogger("Main")

@Suppress("unused")
inline fun <reified T> T.getLogger(): Logger = LoggerFactory.getLogger(T::class.java)

fun main() {
    log.info("The application is starting")
    val token = getEnv("DISCORD_TOKEN")

    val jda = JDABuilder.createDefault(token)
        .enableIntents(GatewayIntent.MESSAGE_CONTENT)
        .addEventListeners(Listener())
        .setActivity(Activity.playing("Music"))
        .build()
    jda.awaitReady()
    log.info("The application has started")
}

private val queues: MutableMap<Long, Queue<AudioTrack>> = mutableMapOf()
private val musicManager = MusicManager()

class Listener : ListenerAdapter() {
    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val queue = queues[event.guild?.idLong] ?: return
        val track = queue.firstOrNull { it.identifier == event.button.id }
        val result = queue.removeIf { it.identifier == event.button.id }
        if (result) {
            event.reply("Удалил ${track?.info?.title}").queue()
            val updatedActionRows = event.message.actionRows.map { actionRow ->
                ActionRow.of(actionRow.buttons.filterNot { it.id == event.button.id })
            }

            event.message.editMessageComponents(updatedActionRows).queue()
        } else {
            event.reply("Чет не удалилось").queue()
        }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val message = event.message
        val content = message.contentRaw
        val queue = queues.computeIfAbsent(event.guild.idLong) { _ -> LinkedList() }
        val guildAudioPlayer = musicManager.getGuildAudioPlayer(event.guild, queue)
        if (message.author.isBot || message.author.isSystem) return
        if (content.startsWith("!play")) {
            val voiceChannel = event.member?.voiceState?.channel
            if (queue.isEmpty()) {
                event.channel.sendMessage("песни добавь еблан").queue()
            }
            if (voiceChannel != null) {
                event.guild.audioManager.openAudioConnection(voiceChannel)
                guildAudioPlayer.audioPlayer.playTrack(queue.poll())
            } else {
                event.channel.sendMessage("В канал войди долбоеб, куда мне тебе играть музыку, в канаву матери?")
                    .queue()
            }
        }
        if (content.startsWith("!clear")) {
            queue.clear()
            guildAudioPlayer.audioPlayer.stopTrack()
            event.channel.sendMessage("Ок").queue()
        }
        if (content.startsWith("!list")) {
            val duration = Duration.ofMillis(queue.sumOf { it.duration })
            val durationMessage = "Длина треков в очереди ${duration.toMinutes()} м. ${duration.toSeconds() % 60} c.\n"
            val messageCreateBuilder = MessageCreateBuilder()
            queue.take(25).map {
                Button.primary(it.identifier, it.info.title)
            }.chunked(5).forEach { messageCreateBuilder.addActionRow(it) }
            event.channel.sendMessage(durationMessage).queue()
            event.channel.sendMessage(messageCreateBuilder.build()).queue()
        }

        if (content.startsWith("!repeat_one")) {
            guildAudioPlayer.listener.repeatOne = guildAudioPlayer.listener.repeatOne.not()
            event.channel.sendMessage("Режим повтора одного трека ${guildAudioPlayer.listener.repeatOne}").queue()
        }
        if (content.startsWith("!skip")) {
            if (queue.isNotEmpty()) {
                guildAudioPlayer.audioPlayer.playTrack(queue.poll())
            } else {
                guildAudioPlayer.audioPlayer.stopTrack()
            }
        }
        if (content.startsWith("!add")) {

            val trackUrl = content.substringAfter(" ")

            // Load and play the track
            musicManager.playerManager.loadItemOrdered(guildAudioPlayer, trackUrl, object :
                AudioLoadResultHandler {
                override fun trackLoaded(track: AudioTrack) {
                    event.channel.sendMessage("Добавил ${track.info.title} в очередь.").queue()
                    queue.add(track)
                }

                override fun playlistLoaded(playlist: AudioPlaylist) {
                    event.channel.sendMessage("Добавил ${playlist.tracks.size} песенок в очередь.").queue()
                    queue.addAll(playlist.tracks)
                }

                override fun noMatches() {
                    event.channel.sendMessage("Не нашел ничего по: $trackUrl").queue()
                }

                override fun loadFailed(exception: FriendlyException) {
                    event.channel.sendMessage("Хуйня какая-то, не работает: ${exception.message}").queue()
                }
            })
        }
        if (content.startsWith("!help")) {
            event.channel.sendMessage(
                """
                !add УРЛ добавляет песню или плейлист с ютуба
                !play начинает играть
                !clear очищает очередь и перестает играть
                !list показывает инфо и выдает песни, если на них нажать удалятся из очереди
                !repeat_one ставит репит текущего трека
            """.trimIndent()
            ).queue()
        }
    }
}

private fun getEnv(envName: String): String {
    return System.getenv()[envName] ?: throw IllegalStateException("$envName does not exist")
}
