package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.handlers.discord.SingleTrackLoadHandler
import dev.storozhenko.disc.misc.EventContext
import dev.storozhenko.disc.misc.MusicManager
import dev.storozhenko.disc.urlRegex
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class Add(private val musicManager: MusicManager) : CommandHandler() {

    override fun handleInternal(context: EventContext) {
        val url = context.event.getOption("url") ?: throw RuntimeException("Url cannot be null")
        url.asString
            .split(",")
            .map { trackSearch -> if (urlRegex.matches(trackSearch).not()) "ytsearch:$trackSearch" else trackSearch }
            .forEach { trackSearch ->
                musicManager.playerManager.loadItemOrdered(
                    context.manager,
                    trackSearch,
                    SingleTrackLoadHandler(context.queue, context.event)
                )
            }
    }

    override fun command(): SlashCommandData {
        return Commands.slash("add", "Добавляет песню или плейлист с YouTube")
            .addOption(OptionType.STRING, "url", "Ссылка или текст", true)
    }
}