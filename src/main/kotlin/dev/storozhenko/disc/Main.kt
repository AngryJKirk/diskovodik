package dev.storozhenko.disc

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.storozhenko.disc.handlers.Add
import dev.storozhenko.disc.handlers.Clear
import dev.storozhenko.disc.handlers.CommandHandler
import dev.storozhenko.disc.handlers.Help
import dev.storozhenko.disc.handlers.PlayThat
import dev.storozhenko.disc.handlers.RepeatOne
import dev.storozhenko.disc.handlers.Search
import dev.storozhenko.disc.handlers.SearchResults
import dev.storozhenko.disc.handlers.Skip
import dev.storozhenko.disc.handlers.Start
import dev.storozhenko.disc.misc.CircularQueue
import dev.storozhenko.disc.misc.EventContext
import dev.storozhenko.disc.misc.MusicManager
import dev.storozhenko.disc.misc.bold
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Queue

@Suppress("unused")
inline fun <reified T> T.getLogger(): Logger = LoggerFactory.getLogger(T::class.java)

private val queues: MutableMap<Long, Queue<AudioTrack>> = mutableMapOf()
private val musicManager = MusicManager(
    getEnv("PO_TOKEN"),
    getEnv("VISITOR_DATA"),
)


private val commandHandlers = listOf(
    Add(musicManager),
    Search(musicManager),
    Clear(),
    dev.storozhenko.disc.handlers.List(),
    PlayThat(),
    RepeatOne(),
    Skip(),
    Start(),
    Help()
)
private val commandHandlersMap = commandHandlers.associateBy { it.command().name }


fun main() {
    val token = getEnv("DISCORD_TOKEN")

    val jda = JDABuilder.createDefault(token)
        .enableIntents(GatewayIntent.MESSAGE_CONTENT)
        .addEventListeners(MainListener())
        .setActivity(Activity.listening("Music"))
        .build()
    jda.awaitReady()
    jda.updateCommands().addCommands(commandHandlers.map(CommandHandler::command)).queue()
}


val urlRegex = Regex("\\b((?:https?://|www\\.)\\S+)\\b")
private fun getEnv(name: String) = System.getenv()[name] ?: throw IllegalStateException("$name does not exist")
class MainListener : ListenerAdapter() {
    private val log = getLogger()

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        try {

            val queue = queues[event.guild?.idLong] ?: return
            val (prefix, buttonId) = event.button.id?.split("|") ?: return
            val guild = event.guild ?: return

            if (prefix == "search") {
                searchButtonHandler(guild, buttonId, event, queue)
                return
            }

            val track = queue.firstOrNull { it.identifier == buttonId }

            if (track == null) {
                event.reply("Бля, чет нет такого трека уже").queue()
                return
            }

            when (prefix) {
                "play" -> playButtonHandler(guild, queue, track, event)
                "remove" -> removeButtonHandler(queue, buttonId, event, track)
            }

        } catch (e: Exception) {
            log.error("onButtonInteraction failed", e)
        }
    }

    private fun searchButtonHandler(
        guild: Guild,
        buttonId: String,
        event: ButtonInteractionEvent,
        queue: Queue<AudioTrack>
    ) {
        val tracks = SearchResults.searchResults[guild.idLong] ?: return
        val track = tracks.find { it.identifier == buttonId }
        if (track == null) {
            event.reply("Чет пошло не так").queue()
            return
        }
        queue.add(track)
        event.reply("Добавил ${track.info.title.bold()} в очередь").queue()
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        try {
            if (event.user.isBot) return

            val handler = commandHandlersMap[event.name]
            if (handler == null) {
                event.reply("Unknown command: ${event.name}").setEphemeral(true).queue()
                return
            }
            handler.handle(toContext(event))
        } catch (e: Exception) {
            log.error("onSlashCommandInteraction failed", e)
        }
    }

    private fun toContext(event: SlashCommandInteractionEvent): EventContext {
        val guild = event.guild ?: throw RuntimeException("Event is missing guild, not supported")
        val queue = queues.computeIfAbsent(guild.idLong) { _ -> CircularQueue() }
        val manager = musicManager.getGuildMusicManager(guild, queue)
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
            event.reply("Удалил ${track.info?.title?.bold()}").queue()
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
        event.reply("Играем ${track.info?.title?.bold()}").queue()
    }

}
