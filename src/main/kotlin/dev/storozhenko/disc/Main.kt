package dev.storozhenko.disc

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.storozhenko.disc.handlers.*
import dev.storozhenko.disc.misc.CircularQueue
import dev.storozhenko.disc.misc.EventContext
import dev.storozhenko.disc.misc.MusicManager
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

@Suppress("unused")
inline fun <reified T> T.getLogger(): Logger = LoggerFactory.getLogger(T::class.java)

fun main() {
    val token = System.getenv()["DISCORD_TOKEN"] ?: throw IllegalStateException("DISCORD_TOKEN does not exist")

    val jda = JDABuilder.createDefault(token)
        .enableIntents(GatewayIntent.MESSAGE_CONTENT)
        .addEventListeners(MainListener())
        .setActivity(Activity.listening("Music"))
        .build()
    jda.awaitReady()
}

private val queues: MutableMap<Long, Queue<AudioTrack>> = mutableMapOf()
private val musicManager = MusicManager()

private val add = Add(musicManager)
private val search = Search(musicManager)
private val clear = Clear()
private val help = Help()
private val list = dev.storozhenko.disc.handlers.List()
private val playThat = PlayThat()
private val repeatOne = RepeatOne()
private val skip = Skip()
private val start = Start()
val urlRegex = Regex("\\b((?:https?://|www\\.)\\S+)\\b")

class MainListener : ListenerAdapter() {
    private val log = getLogger()
    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        try {

            val queue = queues[event.guild?.idLong] ?: return

            val (prefix, buttonId) = event.button.id?.split("|") ?: return

            if (prefix == "search") {
                searchButtonHandler(buttonId, event, queue)
            }

            val track = queue.firstOrNull { it.identifier == buttonId }

            if (track == null) {
                event.reply("Бля чет нет такого трека уже").queue()
                return
            }

            val guild = event.guild ?: return
            if (prefix == "play") {
                playButtonHandler(guild, queue, track, event)
            }
            if (prefix == "remove") {
                removeButtonHandler(queue, buttonId, event, track)
            }
        } catch (e: Exception) {
            log.error("onButtonInteraction failed", e)
        }
    }

    private fun searchButtonHandler(buttonId: String, event: ButtonInteractionEvent, queue: Queue<AudioTrack>) {
        val tracks = SearchResults.searchResults[event.guild?.idLong] ?: return
        val track = tracks.find { it.identifier == buttonId }
        if (track == null) {
            event.reply("Чет пошло не так").queue()
            return
        }
        queue.add(track)
        event.reply("Добавил ${track.info.title} в очередь").queue()
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        try {

            if (event.message.author.isBot || event.message.author.isSystem) return

            val context = toContext(event)
            val handler = when {
                context.forCommand("!start") -> start::handle
                context.forCommand("!clear") -> clear::handle
                context.forCommand("!list") -> list::handle
                context.forCommand("!repeat_one") -> repeatOne::handle
                context.forCommand("!skip") -> skip::handle
                context.forCommand("!add") -> add::handle
                context.forCommand("!search") -> search::handle
                context.forCommand("!play_that") -> playThat::handle
                context.forCommand("!help") -> help::handle
                else -> return
            }
            handler(context)
        } catch (e: Exception) {
            log.error("onMessageReceived failed", e)
        }
    }

    private fun toContext(event: MessageReceivedEvent): EventContext {
        val queue = queues.computeIfAbsent(event.guild.idLong) { _ -> CircularQueue() }
        val manager = musicManager.getGuildMusicManager(event.guild, queue)
        return EventContext(event, queue, manager)
    }

    private fun removeButtonHandler(
        queue: Queue<AudioTrack>,
        buttonId: String,
        event: ButtonInteractionEvent,
        track: AudioTrack
    ) {
        val result = queue.removeIf { it.identifier == buttonId }
        if (result) {
            event.reply("Удалил ${track.info?.title}").queue()
            val updatedActionRows = event.message.actionRows.map { actionRow ->
                ActionRow.of(actionRow.buttons.filterNot { it.id == event.button.id })
            }

            event.message.editMessageComponents(updatedActionRows).queue()
        } else {
            event.reply("Чет не удалилось").queue()
        }
    }

    private fun playButtonHandler(
        guild: Guild,
        queue: Queue<AudioTrack>,
        track: AudioTrack,
        event: ButtonInteractionEvent
    ) {
        val manager = musicManager.getGuildMusicManager(guild, queue)
        manager.audioPlayer.playTrack(track.makeClone())
        event.reply("Играем ${track.info?.title}").queue()
    }


}