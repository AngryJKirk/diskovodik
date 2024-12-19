package dev.storozhenko.disc

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.storozhenko.disc.handlers.Add
import dev.storozhenko.disc.handlers.Clear
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
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Queue

@Suppress("unused")
inline fun <reified T> T.getLogger(): Logger = LoggerFactory.getLogger(T::class.java)

fun main() {
    val token = getEnv("DISCORD_TOKEN")

    val jda = JDABuilder.createDefault(token)
        .enableIntents(GatewayIntent.MESSAGE_CONTENT)
        .addEventListeners(MainListener())
        .setActivity(Activity.listening("Music"))
        .build()
    jda.awaitReady()
    jda.updateCommands()
        .addCommands(
            Commands.slash("add", "Добавляет песню или плейлист с YouTube")
                .addOption(OptionType.STRING, "url", "Ссылка или текст", true),
            Commands.slash("start", "Начинает играть музыку"),
            Commands.slash("skip", "Пропускает текущий трек"),
            Commands.slash("clear", "Очищает очередь и останавливает воспроизведение"),
            Commands.slash("list", "Показывает текущую очередь песен с удалением по нажатию"),
            Commands.slash("help", "Помогите"),
            Commands.slash("repeat_one", "Ставит репит текущего трека"),
            Commands.slash("play_that", "Проигрывает очередь"),
            Commands.slash("search", "Ищет и добавляет песню")
                .addOption(OptionType.STRING, "query", "Текст для поиска", true)
        )
        .queue()

}

private val queues: MutableMap<Long, Queue<AudioTrack>> = mutableMapOf()
private val musicManager = MusicManager(
    getEnv("PO_TOKEN"),
    getEnv("VISITOR_DATA"),
)

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
private fun getEnv(name: String) = System.getenv()[name] ?: throw IllegalStateException("$name does not exist")
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
                event.reply("Бля, чет нет такого трека уже").queue()
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
        event.reply("Добавил ${track.info.title.bold()} в очередь").queue()
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        try {
            if (event.user.isBot) return

            val handler = when (event.name) {
                "start" -> start::handle
                "clear" -> clear::handle
                "list" -> list::handle
                "repeat_one" -> repeatOne::handle
                "skip" -> skip::handle
                "add" -> add::handle
                "search" -> search::handle
                "play_that" -> playThat::handle
                "help" -> help::handle
                else -> {
                    event.reply("Неизвестная команда: ${event.name}").setEphemeral(true).queue()
                    return
                }
            }
            val context = toContext(event)
            handler(context)
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
