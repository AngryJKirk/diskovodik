package dev.storozhenko.disc.misc

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.storozhenko.disc.getLogger
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class CircularQueue : Queue<AudioTrack> {
    private val log = getLogger()
    private val items: MutableList<AudioTrack> = mutableListOf()
    private var cycles = 0

    @Volatile
    private var currentIndex = AtomicInteger(0)
    override fun add(element: AudioTrack): Boolean {
        log.info("Added ${getTrackName(element)}")
        return items.add(element)
    }

    override fun addAll(elements: Collection<AudioTrack>): Boolean {
        log.info("Added all ${elements.joinToString(transform = ::getTrackName)}")
        return items.addAll(elements)
    }

    override fun clear() {
        log.info("tracks to remove: ${items.joinToString(transform = ::getTrackName)}")
        items.clear()
        currentIndex.set(0)
        cycles = 0
    }

    override fun iterator(): MutableIterator<AudioTrack> {
        return items.iterator()
    }

    override fun remove(): AudioTrack {
        if (items.isNotEmpty())
            return items.removeAt(currentIndex.get())
        else throw NoSuchElementException()
    }

    override fun retainAll(elements: Collection<AudioTrack>): Boolean {
        return items.retainAll(elements)
    }

    override fun removeAll(elements: Collection<AudioTrack>): Boolean {
        return items.removeAll(elements)
    }

    override fun isEmpty(): Boolean {
        return items.isEmpty()
    }

    override fun containsAll(elements: Collection<AudioTrack>): Boolean {
        return items.containsAll(elements)
    }

    override fun contains(element: AudioTrack): Boolean {
        return items.contains(element)
    }

    override fun remove(element: AudioTrack): Boolean {
        return items.remove(element)
    }

    override fun poll(): AudioTrack {
        if (items.isEmpty()) throw NoSuchElementException()
        var index = currentIndex.getAndUpdate { i -> if (i + 1 <= items.indices.last) i + 1 else 0 }
        if (index == 0) cycles++
        if (index > items.indices.last) {
            index = items.indices.last
            currentIndex.set(index)
        }
        val t = items[index]
        if (cycles > 10) {
            log.info("clearing the queue due to threshold")
            clear()
        }
        log.info("Next track is ${getTrackName(t)}")
        return t
    }

    override fun element(): AudioTrack {
        if (items.isEmpty()) throw NoSuchElementException()
        return items.elementAt(currentIndex.get())
    }

    override fun peek(): AudioTrack {
        if (items.isEmpty()) throw NoSuchElementException()
        return items[currentIndex.get()]
    }

    override val size: Int
        get() = items.size

    override fun offer(e: AudioTrack): Boolean {
        return items.add(e)
    }

    private fun getTrackName(audioTrack: AudioTrack): String {
        val title = audioTrack.info?.title ?: "[N/A]"
        val author = audioTrack.info?.author ?: "[N/A]"
        return "$author - $title"
    }
}
