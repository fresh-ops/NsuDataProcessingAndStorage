package ru.nsu.g.solovev5.m.taskj1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import ru.nsu.g.solovev5.m.taskj1.net.ConnectionManager;

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

        try (var serverChannel = ServerSocketChannel.open()) {
            serverChannel.bind(new InetSocketAddress(port));

            var connectionManager = new ConnectionManager(serverChannel);
            connectionManager.start();
            System.out.println("Listening at port " + port + "...");
            Thread.sleep(30 * 1000);
            connectionManager.stop();
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        }

        return 0;
    }

}
