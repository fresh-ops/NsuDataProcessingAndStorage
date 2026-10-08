package ru.nsu.g.solovev5.m.taskj1.net;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A connection orchestrator.
 */
public class ConnectionManager {
    private final ConnectionAcceptor connectionAcceptor;
    private final MessageReader messageReader;
    private final ResponseSender responseSender;
    private final ConcurrentHashMap<UUID, Connection> connections;

    /**
     * Creates a new ConnectionManager.
     *
     * @param serverChannel the managing socket channel
     */
    public ConnectionManager(ServerSocketChannel serverChannel) {
        connectionAcceptor = new ConnectionAcceptor(serverChannel, this::handleAccept);
        messageReader = new MessageReader(this::handleMessageReceived, this::handleConnectionClosed);
        responseSender = new ResponseSender(this::handleConnectionClosed);
        connections = new ConcurrentHashMap<>();
    }

    /**
     * Starts this manager.
     */
    public synchronized void start() {
        var services = new AbstractService[]{connectionAcceptor, messageReader, responseSender};

        for (var service : services) {
            service.start();
        }
    }

    /**
     * Stops this manager.
     *
     * @throws InterruptedException if any thread has interrupted the current thread
     */
    public synchronized void stop() throws InterruptedException {
        var isInterrupted = false;
        var services = new AbstractService[]{connectionAcceptor, messageReader, responseSender};

        for (var service : services) {
            while (true) {
                try {
                    service.stop();
                    break;
                } catch (InterruptedException e) {
                    isInterrupted = true;
                }
            }
        }

        if (isInterrupted) {
            throw new InterruptedException();
        }
    }

    private void handleAccept(SocketChannel socketChannel) {
        var connection = new Connection(UUID.randomUUID(), socketChannel);
        System.out.println("New connection: " + connection.id());
        messageReader.addConnection(connection);
        connections.put(connection.id(), connection);
    }

    private void handleMessageReceived(UUID connectionId, String message) {
        System.out.println("Message from " + connectionId + ": " + message);
        var connection = connections.get(connectionId);
        if (connection != null) {
            responseSender.addConnection(connection, "Ok\n".getBytes(StandardCharsets.US_ASCII));
        }
    }

    private void handleConnectionClosed(UUID connectionId) {
        System.out.println("Connection closed: " + connectionId);
        var connection = connections.remove(connectionId);
        if (connection == null) {
            return;
        }

        try {
            connection.channel().close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + connectionId);
        }
    }
}
