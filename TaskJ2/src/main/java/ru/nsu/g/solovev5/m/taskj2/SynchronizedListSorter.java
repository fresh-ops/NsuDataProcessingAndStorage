package ru.nsu.g.solovev5.m.taskj2;

import java.util.List;

public class SynchronizedListSorter implements Runnable {
    private final List<String> list;
    private final long sleepDuration;
    private volatile int steps = 0;

    public SynchronizedListSorter(List<String> list, long sleepDuration) {
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

    public int getSteps() {
        return steps;
    }

    private void sort() throws InterruptedException {
        for (var i = 0; i + 1 < list.size(); i++) {
            Thread.sleep(sleepDuration);
            synchronized (list) {
                steps++;
                if (list.get(i + 1).compareTo(list.get(i)) < 0) {
                    var temp = list.get(i);
                    list.set(i, list.get(i + 1));
                    list.set(i + 1, temp);
                }
                Thread.sleep(sleepDuration);
            }
        }
    }
}
