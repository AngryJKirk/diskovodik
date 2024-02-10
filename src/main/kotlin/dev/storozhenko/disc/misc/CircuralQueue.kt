package dev.storozhenko.disc.misc

import java.util.Queue
import java.util.concurrent.atomic.AtomicInteger

class CircularQueue<T> : Queue<T> {
    private val items: MutableList<T> = mutableListOf()
    private var cycles = 0

    @Volatile
    private var currentIndex = AtomicInteger(0)
    override fun add(element: T): Boolean {
        return items.add(element)
    }

    override fun addAll(elements: Collection<T>): Boolean {
        return items.addAll(elements)
    }

    override fun clear() {
        items.clear()
        currentIndex.set(0)
        cycles = 0
    }

    override fun iterator(): MutableIterator<T> {
        return items.iterator()
    }

    override fun remove(): T {
        if (items.isNotEmpty())
            return items.removeAt(currentIndex.get())
        else throw NoSuchElementException()
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        return items.retainAll(elements)
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        return items.removeAll(elements)
    }

    override fun isEmpty(): Boolean {
        return items.isEmpty()
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return items.containsAll(elements)
    }

    override fun contains(element: T): Boolean {
        return items.contains(element)
    }

    override fun remove(element: T): Boolean {
        return items.remove(element)
    }

    override fun poll(): T {
        if (items.isEmpty()) throw NoSuchElementException()
        var index = currentIndex.getAndUpdate { i -> if (i + 1 <= items.indices.last) i + 1 else 0 }
        if (index == 0) cycles++
        if (index > items.indices.last) {
            index = items.indices.last
            currentIndex.set(index)
        }
        val t = items[index]
        if (cycles > 10) {
            clear()
        }
        return t
    }

    override fun element(): T {
        if (items.isEmpty()) throw NoSuchElementException()
        return items.elementAt(currentIndex.get())
    }

    override fun peek(): T {
        if (items.isEmpty()) throw NoSuchElementException()
        return items[currentIndex.get()]
    }

    override val size: Int
        get() = items.size

    override fun offer(e: T): Boolean {
        return items.add(e)
    }
}
