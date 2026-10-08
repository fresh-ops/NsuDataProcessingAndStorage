package ru.nsu.g.solovev5.m.taskj1.net;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import ru.nsu.g.solovev5.m.taskj1.shared.AbstractService;
import ru.nsu.g.solovev5.m.taskj1.shared.Request;
import ru.nsu.g.solovev5.m.taskj1.shared.Response;

/**
 * A connection orchestrator.
 */
public class ConnectionManager extends AbstractService {
    private final BlockingQueue<Request> requests;
    private final BlockingQueue<Response> responses;

    private final ConnectionAcceptor connectionAcceptor;
    private final MessageReader messageReader;
    private final ResponseSender responseSender;
    private final ConcurrentHashMap<UUID, Connection> connections;

    /**
     * Creates a new ConnectionManager.
     *
     * @param serverChannel the managing socket channel
     * @param requests      the queue to put new requests
     * @param responses     the queue to poll responses
     */
    public ConnectionManager(
        ServerSocketChannel serverChannel,
        BlockingQueue<Request> requests,
        BlockingQueue<Response> responses
    ) {
        this.requests = requests;
        this.responses = responses;

        connectionAcceptor = new ConnectionAcceptor(serverChannel, this::handleAccept);
        messageReader = new MessageReader(this::handleMessageReceived, this::handleConnectionClosed);
        responseSender = new ResponseSender(this::handleConnectionClosed);
        connections = new ConcurrentHashMap<>();
    }

    /**
     * Starts this manager.
     */
    @Override
    public synchronized void start() {
        var services = new AbstractService[]{connectionAcceptor, messageReader, responseSender};

        for (var service : services) {
            service.start();
        }
        super.start();
    }

    /**
     * Stops this manager.
     *
     * @throws InterruptedException if any thread has interrupted the current thread
     */
    @Override
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
        super.stop();
    }

    @Override
    protected void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            var response = responses.poll();
            if (response == null) continue;
            var connection = connections.get(response.connectionId());
            if (connection == null) continue;

            responseSender.addConnection(connection, response.message());
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
            requests.add(new Request(connection.id(), message));
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
