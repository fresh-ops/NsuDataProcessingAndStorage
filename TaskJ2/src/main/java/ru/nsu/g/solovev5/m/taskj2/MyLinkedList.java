package ru.nsu.g.solovev5.m.taskj2;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A custom realization of a linked list.
 *
 * @param <T> the type of the stored data
 */
public class MyLinkedList<T> implements Iterable<T> {
    private final Node<T> head;

    /**
     * Creates a new empty MyLinkedList.
     */
    public MyLinkedList() {
        head = new Node<>(null);
    }

    /**
     * Adds a new value to this list.
     *
     * @param value a new value to store
     */
    public void pushFront(T value) {
        var newNode = new Node<>(value);
        head.lock();
        try {
            newNode.setNext(head.getNext());
            head.setNext(newNode);
        } finally {
            head.unlock();
        }
    }

    public Node<T> getHead() {
        return head;
    }

    @Override
    public Iterator<T> iterator() {
        return new MyIterator();
    }

    /**
     * A data storing entity in MyLinkedList.
     *
     * @param <T> the type of the stored data
     */
    public static class Node<T> {
        private final ReentrantLock lock = new ReentrantLock();
        private final T data;
        private Node<T> next;

        /**
         * Creates a new Node.
         *
         * @param data the data to store
         */
        public Node(T data) {
            this.data = data;
        }

        /**
         * Gets the stored data.
         *
         * @return the stored data
         */
        public T getData() {
            return data;
        }

        /**
         * Gets the next node in this list.
         *
         * @return the next node in the list
         */
        public Node<T> getNext() {
            return next;
        }

        /**
         * Sets the next node in this list.
         *
         * @param next the new next node
         */
        public void setNext(Node<T> next) {
            this.next = next;
        }

        public void lock() {
            lock.lock();
        }

        public void unlock() {
            lock.unlock();
        }
    }

    /**
     * A custom iterator for MyLinkedList.
     */
    private class MyIterator implements Iterator<T> {
        private Node<T> current;
        private boolean isClosed = false;

        /**
         * Creates a new MyIterator pointing the first element of the list.
         */
        public MyIterator() {
            current = head;
            current.lock();
        }

        @Override
        public boolean hasNext() {
            if (isClosed) {
                return false;
            }
            var hasNext = current.getNext() != null;
            if (!hasNext) {
                isClosed = true;
                current.unlock();
            }

            return hasNext;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            var next = current.getNext();
            next.lock();
            current.unlock();
            current = next;
            return current.getData();
        }
    }
}
