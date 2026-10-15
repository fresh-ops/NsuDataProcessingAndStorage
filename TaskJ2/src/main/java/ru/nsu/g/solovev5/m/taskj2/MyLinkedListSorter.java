package ru.nsu.g.solovev5.m.taskj2;

public class MyLinkedListSorter implements Runnable {
    private final MyLinkedList<String> list;
    private final long sleepDuration;

    public MyLinkedListSorter(MyLinkedList<String> list, long sleepDuration) {
        this.list = list;
        this.sleepDuration = sleepDuration;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                sort();
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void sort() throws InterruptedException {
        var previous = list.getHead();
        while (true) {
            Thread.sleep(sleepDuration);
            previous.lock();
            var current = previous.getNext();
            if (current == null) {
                previous.unlock();
                break;
            }
            current.lock();

            var next = current.getNext();
            if (next == null) {
                previous.unlock();
                current.unlock();
                break;
            }
            next.lock();
            if (next.getData().compareTo(current.getData()) < 0) {
                previous.setNext(next);
                current.setNext(next.getNext());
                next.setNext(current);


                current = next;
                next = current.getNext();
            }

            try {
                Thread.sleep(sleepDuration);
            } finally {
                previous.unlock();
                current.unlock();
                next.unlock();
            }

            previous = current;
        }
    }
}
