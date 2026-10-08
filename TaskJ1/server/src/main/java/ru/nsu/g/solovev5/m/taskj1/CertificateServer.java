package ru.nsu.g.solovev5.m.taskj1;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.bouncycastle.asn1.x500.X500Name;
import picocli.CommandLine;
import ru.nsu.g.solovev5.m.taskj1.certificates.GenerationManager;
import ru.nsu.g.solovev5.m.taskj1.certificates.IdGenerator;
import ru.nsu.g.solovev5.m.taskj1.certificates.PemKeyReader;
import ru.nsu.g.solovev5.m.taskj1.net.ConnectionManager;
import ru.nsu.g.solovev5.m.taskj1.shared.Request;
import ru.nsu.g.solovev5.m.taskj1.shared.Response;

@CommandLine.Command(
    name = "server",
    mixinStandardHelpOptions = true,
    description = "Starts a new certificate server"
)
public class CertificateServer implements Callable<Integer> {
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
    public Integer call() throws Exception {
        System.out.println("Starting a new certificate server...");
        System.out.println("Issuer name: " + issuerName);
        System.out.println("Signing key: " + signingKeyPath);
        System.out.println("Number of threads: " + threadCount);

        var requests = new LinkedBlockingQueue<Request>();
        var responses = new LinkedBlockingQueue<Response>();

        try (
            var executor = Executors.newFixedThreadPool(threadCount);
            var serverChannel = ServerSocketChannel.open()
        ) {
            serverChannel.bind(new InetSocketAddress(port));

            var connectionManager = new ConnectionManager(serverChannel, requests, responses);
            var generationManager = new GenerationManager(
                new X500Name(issuerName),
                PemKeyReader.readPrivateKey(signingKeyPath),
                new IdGenerator(),
                requests,
                responses,
                executor
            );
            connectionManager.start();
            generationManager.start();
            System.out.println("Listening at port " + port + "...");
            Thread.sleep(2 * 60 * 1000);
            generationManager.stop();
            connectionManager.stop();
        }

        return 0;
    }

}
