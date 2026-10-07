package ru.nsu.g.solovev5.m.taskj1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
    name = "server",
    mixinStandardHelpOptions = true,
    description = "Starts a new certificate server"
)
public class CertificateServer implements Callable<Integer> {
    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 8192;

    @CommandLine.Parameters(
        index = "0",
        paramLabel = "ISSUER_NAME"
    )
    private String issuerName;

    @CommandLine.Parameters(
        index = "1",
        paramLabel = "SIGNING_KEY",
        description = "PEM file containing signing key"
    )
    private Path signingKeyPath;

    @CommandLine.Option(
        names = {"--threads"},
        paramLabel = "THREADS",
        description = "number of generating keys threads. Defaults to available processors"
    )
    private int threadCount = Runtime.getRuntime().availableProcessors();

    @CommandLine.Option(
        names = {"--port"},
        paramLabel = "PORT",
        description = "listening port. Defaults to 8080",
        defaultValue = "8080"
    )
    private int port;

    static void main(String[] args) {
        var exitCode = new CommandLine(new CertificateServer()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws IOException {
        System.out.println("Starting a new certificate server...");
        System.out.println("Issuer name: " + issuerName);
        System.out.println("Signing key: " + signingKeyPath);
        System.out.println("Number of threads: " + threadCount);

        try (
            var selector = Selector.open();
            var serverChannel = ServerSocketChannel.open()
        ) {
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(port));

            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Listening at port " + port);

            while (true) {
                int readyChannels = selector.select();
                if (readyChannels == 0) continue;

                var selectedKeys = selector.selectedKeys();
                var keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        handleAccept(serverChannel, selector);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        }
    }

    private void handleAccept(ServerSocketChannel serverChannel, Selector selector) throws IOException {
        var clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);

        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("New connection: " + clientChannel.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) throws IOException {
        var clientChannel = (SocketChannel) key.channel();
        var state = (ByteArrayOutputStream) key.attachment();
        if (state == null) {
            state = new ByteArrayOutputStream();
            key.attach(state);
        }
        var buffer = ByteBuffer.allocate(256);

        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            System.out.println("Connection closed: " + clientChannel.getRemoteAddress());
            clientChannel.close();
            key.cancel();
            return;
        }

        if (bytesRead > 0) {
            buffer.flip();
            state.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
            buffer.clear();

            var currentData = state.toByteArray();
            if (currentData[currentData.length - 1] == '\0') {
                var message = new String(currentData, 0, currentData.length - 1, StandardCharsets.US_ASCII);
                System.out.println("New message[" + clientChannel.getRemoteAddress() + "]: " + message);
                clientChannel.write(ByteBuffer.wrap("Ok\n".getBytes(StandardCharsets.US_ASCII)));
            }
        }
    }
}
