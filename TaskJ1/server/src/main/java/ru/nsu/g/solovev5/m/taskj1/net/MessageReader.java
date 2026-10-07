package ru.nsu.g.solovev5.m.taskj1.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A multi connection message reader.
 */
public class MessageReader extends AbstractService {
    private Selector selector;
    private final BiConsumer<UUID, String> onMessageReceived;
    private final Consumer<UUID> onConnectionClosed;

    private final BlockingQueue<Connection> pendingConnections = new LinkedBlockingQueue<>();
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);

    /**
     * Creates a new MessageReader.
     *
     * @param onMessageReceived  the callback receiving a connection UUID and message
     * @param onConnectionClosed the callback receiving a closed connection UUID
     */
    public MessageReader(BiConsumer<UUID, String> onMessageReceived, Consumer<UUID> onConnectionClosed) {
        this.onMessageReceived = onMessageReceived;
        this.onConnectionClosed = onConnectionClosed;
    }

    /**
     * Adds a new connection in this reader.
     *
     * @param connection the connection to read
     */
    public void addConnection(Connection connection) {
        pendingConnections.add(connection);
        if (selector != null) selector.wakeup();
    }

    @Override
    protected void loop() {
        try {
            selector = Selector.open();
            runReadingLoop();
        } catch (IOException e) {
            System.err.println("Error opening selector: " + e.getMessage());
        } finally {
            closeSelector();
        }
    }

    private void runReadingLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            processPendingConnections();

            try {
                if (selector.select() == 0) continue;
                if (Thread.currentThread().isInterrupted()) return;

                var keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    var key = keys.next();
                    keys.remove();

                    if (!key.isValid() || !key.isReadable()) continue;

                    handleRead(key);
                }
            } catch (IOException e) {
                System.err.println("Error reading channels: " + e.getMessage());
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
                var key = connection.channel().register(selector, SelectionKey.OP_READ);
                key.attach(new ReadingState(connection.id(), new ByteArrayOutputStream()));
            } catch (IOException e) {
                System.err.println("Error registering channel: " + e.getMessage());
            }
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        var channel = (SocketChannel) key.channel();
        var bytesRead = channel.read(buffer);

        if (bytesRead <= 0) {
            if (bytesRead == -1) handleConnectionClosed(key);
            return;
        }

        var state = (ReadingState) key.attachment();
        buffer.flip();
        state.extendMessage(buffer);
        buffer.clear();

        if (state.isMessageReceived()) {
            handleMessageReceived(key, state);
        }
    }

    private void handleMessageReceived(SelectionKey key, ReadingState state) {
        var bytes = state.bytes();
        var message = new String(bytes, 0, bytes.length - 1, StandardCharsets.US_ASCII);
        state.receivingMessage().reset();
        key.cancel();

        onMessageReceived.accept(state.connectionId, message);
    }

    private void handleConnectionClosed(SelectionKey key) {
        var state = (ReadingState) key.attachment();
        key.cancel();

        onConnectionClosed.accept(state.connectionId);
    }

    private record ReadingState(
        UUID connectionId,
        ByteArrayOutputStream receivingMessage
    ) {
        public void extendMessage(ByteBuffer buffer) {
            var bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            receivingMessage.write(bytes, 0, bytes.length);
        }

        public boolean isMessageReceived() {
            var currentData = receivingMessage.toByteArray();
            return currentData.length > 0 && currentData[currentData.length - 1] == '\0';
        }

        public byte[] bytes() {
            return receivingMessage.toByteArray();
        }
    }
}
