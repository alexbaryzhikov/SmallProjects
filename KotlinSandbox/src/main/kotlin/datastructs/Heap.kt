package datastructs

interface Heap<T : Comparable<T>> {
    fun peek(): T?
    fun extract(): T?
    fun insert(element: T): Boolean
    fun remove(element: T): Boolean
    fun modify(element: T, newElement: T): Boolean
}