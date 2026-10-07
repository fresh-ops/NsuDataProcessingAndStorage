package ru.nsu.g.solovev5.m.taskj1.net;

/**
 * A service running in a separate thread.
 */
public abstract class AbstractService {
    private Thread thread;

    /**
     * Checks if this service is running.
     *
     * @return {@code true} if this service is running, {@code false} otherwise
     */
    public synchronized boolean isRunning() {
        return thread != null && thread.isAlive();
    }

    /**
     * Starts this task. If this service is already started, it does nothing.
     */
    public synchronized void start() {
        if (isRunning()) {
            return;
        }

        thread = new Thread(this::loop);
        thread.start();
    }

    /**
     * Stops this task. If this service is not running, it does nothing.
     *
     * @throws InterruptedException if any thread has interrupted the current thread
     */
    public synchronized void stop() throws InterruptedException {
        if (isRunning()) {
            try {
                thread.interrupt();
                thread.join(5_000);
            } finally {
                thread = null;
            }
        }
    }

    /**
     * Performs the loop.
     */
    protected abstract void loop();
}
