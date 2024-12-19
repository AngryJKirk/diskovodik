package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext
import dev.storozhenko.disc.misc.inlineCode

class Help : CommandHandler() {
    override fun handleInternal(context: EventContext) {
        context.event.message.reply(
            """
                    ${"!add".inlineCode()} УРЛ добавляет песню или плейлист с ютуба, можно просто текстом
                    ${"!start".inlineCode()} начинает играть
                    ${"!skip".inlineCode()} скипнуть трек
                    ${"!clear".inlineCode()} очищает очередь и перестает играть
                    ${"!list".inlineCode()} показывает инфо и выдает песни, если на них нажать удалятся из очереди
                    ${"!repeat_one".inlineCode()} ставит репит текущего трека
                    ${"!play_that".inlineCode()} показывает очередь которую можно проиграть
                    ${"!search".inlineCode()} позволяет искать и добавлять песни по запросу
                """.trimIndent()
        ).queue()
    }
}