package ru.nsu.g.solovev5.m.taskj1.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import ru.nsu.g.solovev5.m.taskj1.shared.AbstractService;

/**
 * A multi connection response sender.
 */
public class ResponseSender extends AbstractService {
    private Selector selector;
    private final Consumer<UUID> onResponseSent;

    private final BlockingQueue<PendingConnection> pendingConnections = new LinkedBlockingQueue<>();

    /**
     * Creates a new ResponseSender.
     *
     * @param onResponseSent the callback on response sent
     */
    public ResponseSender(Consumer<UUID> onResponseSent) {
        this.onResponseSent = onResponseSent;
    }

    /**
     * Adds a new connection to send response.
     *
     * @param connection the connection to send to
     * @param response the response to send
     */
    public void addConnection(Connection connection, byte[] response) {
        pendingConnections.add(new PendingConnection(
            connection.id(),
            connection.channel(),
            response
        ));
        if (selector != null) selector.wakeup();
    }

    @Override
    protected void loop() {
        try {
            selector = Selector.open();
            runWritingLoop();
        } catch (IOException e) {
            System.err.println("Error opening selector: " + e.getMessage());
        } finally {
            closeSelector();
        }
    }

    private void runWritingLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            processPendingConnections();

            try {
                if (selector.select() == 0) continue;
                if (Thread.currentThread().isInterrupted()) return;

                var keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    var key = keys.next();
                    keys.remove();

                    if (!key.isValid() || !key.isWritable()) continue;

                    handleWrite(key);
                }
            } catch (IOException e) {
                System.err.println("Error writing channels: " + e.getMessage());
            }
        }
    }

    private void closeSelector() {
        try {
            selector.close();
        } catch (IOException e) {
            System.err.println("Error closing selector: " + e.getMessage());
        } finally {
            selector = null;
        }
    }

    private void processPendingConnections() {
        while (selector != null && !pendingConnections.isEmpty()) {
            var connection = pendingConnections.poll();
            try {
                connection.channel().configureBlocking(false);
                var key = connection.channel().register(selector, SelectionKey.OP_WRITE);
                key.attach(new WritingState(connection.id(), ByteBuffer.wrap(connection.response())));
            } catch (IOException e) {
                System.err.println("Error registering channel: " + e.getMessage());
            }
        }
    }

    private void handleWrite(SelectionKey key) {
        var channel = (SocketChannel) key.channel();
        var state = (WritingState) key.attachment();

        try {
            channel.write(state.buffer());
        } catch (IOException e) {
            System.err.println("Error writing channel: " + e.getMessage());
            key.cancel();
            onResponseSent.accept(state.connectionId());
        }

        if (!state.buffer().hasRemaining()) {
            handleResponseSent(state);
            key.cancel();
        }
    }

    private void handleResponseSent(WritingState state) {
        onResponseSent.accept(state.connectionId());
        state.buffer().clear();
    }

    private record PendingConnection(
        UUID id,
        SocketChannel channel,
        byte[] response
    ) {
    }

    private record WritingState(
        UUID connectionId,
        ByteBuffer buffer
    ) {
    }
}
