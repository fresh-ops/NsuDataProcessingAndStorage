package ru.nsu.g.solovev5.m.taskj1.net;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;
import ru.nsu.g.solovev5.m.taskj1.shared.AbstractService;

/**
 * A class for accepting connection to the server socket.
 */
public class ConnectionAcceptor extends AbstractService {
    private final ServerSocketChannel serverChannel;
    private final Consumer<SocketChannel> onAccept;

    /**
     * Creates a new ConnectionAcceptor.
     *
     * @param serverChannel the socket channel accepting connections
     * @param onAccept the callback triggering on every accepting
     */
    public ConnectionAcceptor(ServerSocketChannel serverChannel, Consumer<SocketChannel> onAccept) {
        this.serverChannel = serverChannel;
        this.onAccept = onAccept;
    }

    @Override
    protected void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                var client = serverChannel.accept();
                if (Thread.currentThread().isInterrupted()) {
                    if (client != null) {
                        client.close();
                    }
                    break;
                }

                if (client != null) {
                    onAccept.accept(client);
                }
            } catch (ClosedByInterruptException e) {
                break;
            } catch (IOException e) {
                if (!serverChannel.isOpen()) {
                    break;
                }
                System.err.println("Error accepting client connection: " + e.getMessage());
            }
        }
    }
}
