package dev.storozhenko.disc

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import java.time.Duration
import java.util.Queue

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
private val searchResults: MutableMap<Long, Collection<AudioTrack>> = mutableMapOf()
private val musicManager = MusicManager()
val urlRegex = Regex("\\b((?:https?://|www\\.)\\S+)\\b")

class MainListener : ListenerAdapter() {
    override fun onButtonInteraction(event: ButtonInteractionEvent) {

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
    }

    private fun searchButtonHandler(buttonId: String, event: ButtonInteractionEvent, queue: Queue<AudioTrack>) {
        val tracks = searchResults[event.guild?.idLong] ?: return
        val track = tracks.find { it.identifier == buttonId }
        if (track == null) {
            event.reply("Чет пошло не так").queue()
            return
        }
        queue.add(track)
        event.reply("Добавил ${track.info.title} в очередь").queue()
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.message.author.isBot || event.message.author.isSystem) return

        val content = event.message.contentRaw
        val queue = queues.computeIfAbsent(event.guild.idLong) { _ -> CircularQueue() }
        val manager = musicManager.getGuildMusicManager(event.guild, queue)

        if (content.startsWith("!start")) {
            start(event, queue, manager)
            return
        }
        if (content.startsWith("!clear")) {
            clear(queue, manager, event)
            return
        }
        if (content.startsWith("!list")) {
            list(queue, event)
            return
        }
        if (content.startsWith("!repeat_one")) {
            repeatOne(manager, event)
            return
        }
        if (content.startsWith("!skip")) {
            skip(queue, manager)
            return
        }
        if (content.startsWith("!add")) {
            add(content, manager, event, queue)
            return
        }
        if (content.startsWith("!search")) {
            search(event, manager, queue)
            return
        }
        if (content.startsWith("!play_that")) {
            playThat(event, queue)
            return
        }
        if (content.startsWith("!help")) {
            help(event)
            return
        }
    }

    private fun search(event: MessageReceivedEvent, manager: MusicManager.GuildMusicManager, queue: Queue<AudioTrack>) {
        var query = event.message.contentRaw.substringAfter(" ")
        if (!urlRegex.matches(query)) {
            query = "ytsearch:$query"
        }
        musicManager.playerManager.loadItemOrdered(
            manager, query,
            SearchLoadHandler(event, queue)
        )
    }

    private fun help(event: MessageReceivedEvent) {
        event.message.reply(
            """
                    !add УРЛ добавляет песню или плейлист с ютуба, можно просто текстом
                    !start начинает играть
                    !skip скипнуть трек
                    !clear очищает очередь и перестает играть
                    !list показывает инфо и выдает песни, если на них нажать удалятся из очереди
                    !repeat_one ставит репит текущего трека
                    !play_that показывает очередь которую можно проиграть
                    !search позволяет искать и добавлять песни по запросу
                """.trimIndent()
        ).queue()
    }

    private fun playThat(
        event: MessageReceivedEvent,
        queue: Queue<AudioTrack>
    ) {
        event.message.reply(createButtons(queue, "play")).queue()
    }

    private fun add(
        content: String,
        manager: MusicManager.GuildMusicManager,
        event: MessageReceivedEvent,
        queue: Queue<AudioTrack>
    ) {
        var trackUrl = content.substringAfter(" ")
        if (urlRegex.matches(trackUrl).not()) {
            trackUrl = "ytsearch:$trackUrl"
        }
        // Load and play the track
        musicManager.playerManager.loadItemOrdered(manager, trackUrl, SingleTrackLoadHandler(queue, event))
    }

    private fun start(
        event: MessageReceivedEvent,
        queue: Queue<AudioTrack>,
        manager: MusicManager.GuildMusicManager
    ) {
        val voiceChannel = event.member?.voiceState?.channel
        if (queue.isEmpty()) {
            event.message.reply("песни добавь еблан").queue()
            return
        }
        if (voiceChannel != null) {
            event.guild.audioManager.openAudioConnection(voiceChannel)
            manager.audioPlayer.playTrack(queue.poll().makeClone())
        } else {
            event.message.reply("В канал войди долбоеб, куда мне тебе играть музыку, в канаву матери?")
                .queue()
        }
    }

    private fun clear(
        queue: Queue<AudioTrack>,
        manager: MusicManager.GuildMusicManager,
        event: MessageReceivedEvent
    ) {
        queue.clear()
        manager.audioPlayer.stopTrack()
        event.message.reply("Ок").queue()
    }

    private fun list(
        queue: Queue<AudioTrack>,
        event: MessageReceivedEvent
    ) {
        val duration = Duration.ofMillis(queue.sumOf { it.duration })
        val durationMessage = "Длина треков в очереди ${duration.toMinutes()} м. ${duration.toSeconds() % 60} c.\n"
        event.message.reply(durationMessage).queue()
        event.message.reply(createButtons(queue, "remove")).queue()
    }

    private fun repeatOne(
        manager: MusicManager.GuildMusicManager,
        event: MessageReceivedEvent
    ) {
        manager.listener.repeatOne = manager.listener.repeatOne.not()
        event.message.reply("Режим повтора одного трека ${manager.listener.repeatOne}").queue()
    }

    private fun skip(
        queue: Queue<AudioTrack>,
        manager: MusicManager.GuildMusicManager
    ) {
        if (queue.isNotEmpty()) {
            manager.audioPlayer.playTrack(queue.poll().makeClone())
        } else {
            manager.audioPlayer.stopTrack()
        }
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

    companion object {
        fun createButtons(queue: Collection<AudioTrack>, prefix: String): MessageCreateData {
            val messageCreateBuilder = MessageCreateBuilder()
            queue.take(25).map {
                Button.primary("$prefix|${it.identifier}", it.info.title.take(79))
            }.chunked(5).forEach { messageCreateBuilder.addActionRow(it) }
            return messageCreateBuilder.build()
        }
    }

    class SearchLoadHandler(private val event: MessageReceivedEvent, queue: Queue<AudioTrack>) :
        SingleTrackLoadHandler(queue, event) {

        override fun playlistLoaded(playlist: AudioPlaylist) {
            searchResults[event.guild.idLong] = playlist.tracks
            event.message.reply(createButtons(playlist.tracks, "search")).queue()
        }
    }

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
}