package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData


class Start : CommandHandler() {
    override fun handleInternal(context: EventContext) {
        val event = context.event
        if (context.queue.isEmpty()) {
            event.reply("песни добавь, еблан").queue()
            return
        }
        val voiceChannel = event.member?.voiceState?.channel
        if (voiceChannel != null) {
            val guild = event.guild ?: throw RuntimeException("guild is null")
            guild.audioManager.openAudioConnection(voiceChannel)
            context.manager.audioPlayer.playTrack(context.queue.poll().makeClone())
            context.event.reply("Стартуем").setEphemeral(true).queue()
        } else {
            event.reply("В канал войди, долбоеб, куда мне тебе играть музыку, в канаву матери?")
                .queue()
        }
    }

    override fun command(): SlashCommandData {
        return Commands.slash("start", "Начинает играть музыку")
    }
}