package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext


class Start : CommandHandler {
    override fun handle(context: EventContext) {
        val event = context.event
        if (context.queue.isEmpty()) {
            event.message.reply("песни добавь еблан").queue()
            return
        }
        val voiceChannel = event.member?.voiceState?.channel
        if (voiceChannel != null) {
            event.guild.audioManager.openAudioConnection(voiceChannel)
            context.manager.audioPlayer.playTrack(context.queue.poll().makeClone())
        } else {
            event.message.reply("В канал войди долбоеб, куда мне тебе играть музыку, в канаву матери?")
                .queue()
        }
    }
}