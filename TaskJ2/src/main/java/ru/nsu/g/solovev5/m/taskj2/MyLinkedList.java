package ru.nsu.g.solovev5.m.taskj2;

import java.util.Iterator;
import java.util.NoSuchElementException;

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
    public  MyLinkedList() {
        head = new Node<>(null);
    }

    /**
     * Adds a new value to this list.
     *
     * @param value a new value to store
     */
    public void pushFront(T value) {
        var newNode = new Node<>(value);
        newNode.setNext(head.getNext());
        head.setNext(newNode);
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
    private static class Node<T> {
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
    }

    /**
     * A custom iterator for MyLinkedList.
     */
    private class MyIterator implements Iterator<T> {
        private Node<T> current;

        /**
         * Creates a new MyIterator pointing the first element of the list.
         */
        public MyIterator() {
            current = head;
        }

        @Override
        public boolean hasNext() {
            return current.getNext() != null;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            current = current.getNext();
            return current.getData();
        }
    }
}
