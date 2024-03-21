package dev.storozhenko.disc.handlers

import dev.storozhenko.disc.misc.EventContext

class Help : CommandHandler() {
    override fun handleInternal(context: EventContext) {
        context.event.message.reply(
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
}