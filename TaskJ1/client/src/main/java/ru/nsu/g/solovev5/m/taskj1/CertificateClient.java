package ru.nsu.g.solovev5.m.taskj1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
    name = "client",
    mixinStandardHelpOptions = true,
    description = "Requests a certificate from specified server"
)
public class CertificateClient implements Callable<Integer> {
    @CommandLine.Parameters(
        index = "0",
        paramLabel = "SUBJECT_NAME",
        description = "name of the certificate subject"
    )
    private String subjectName;

    @CommandLine.Option(
        names = {"--port"},
        paramLabel = "PORT",
        description = "server port. Defaults to 8080",
        defaultValue = "8080"
    )
    private int port;

    @CommandLine.Option(
        names = {"--url"},
        paramLabel = "SERVER_URL",
        description = "url of server",
        defaultValue = "localhost"
    )
    private String url;

    static void main(String[] args) {
        var exitCode = new CommandLine(new CertificateClient()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("Requesting a certificate");
        System.out.println("For " + subjectName);
        System.out.println("From " + url + ":" + port);

        try (var socketChannel = SocketChannel.open()) {
            socketChannel.connect(new InetSocketAddress(url, port));
            socketChannel.configureBlocking(true);
            System.out.println("Sending certificate request");
            sendRequest(socketChannel);

            System.out.println("Reading response");
            var response = readResponse(socketChannel);

            try (var inputStream = new DataInputStream(new ByteArrayInputStream(response))) {
                var certificateLength = inputStream.readInt();
                var certificateBytes = new byte[certificateLength];
                inputStream.readFully(certificateBytes);

                var privateLength = inputStream.readInt();
                var privateBytes = new byte[privateLength];
                inputStream.readFully(privateBytes);

                var publicLength = inputStream.readInt();
                var publicBytes = new byte[publicLength];
                inputStream.readFully(publicBytes);

                saveAsPem(certificateBytes, "client.crt", "CERTIFICATE", false);
                saveAsPem(privateBytes, "client.key", "PRIVATE KEY", false);
                saveAsPem(publicBytes, "client.key", "PUBLIC KEY", true);

                System.out.println("Files saved");
            } finally {
                System.out.println("Connection closed");
            }
        }

        return 0;
    }


    private void sendRequest(SocketChannel socketChannel) throws IOException {
        var subjectBytes = subjectName.getBytes(StandardCharsets.US_ASCII);
        var buffer = ByteBuffer.allocate(subjectBytes.length + 1)
            .put(subjectBytes)
            .put((byte) 0)
            .flip();

        socketChannel.write(buffer);
    }

    private byte[] readResponse(SocketChannel socketChannel) throws IOException {
        var buffer = ByteBuffer.allocate(1024);
        var response = new ByteArrayOutputStream();
        while (socketChannel.read(buffer) != -1) {
            buffer.flip();
            response.write(buffer.array(), buffer.position(), buffer.remaining());
            buffer.clear();
        }

        return response.toByteArray();
    }

    private void saveAsPem(byte[] derBytes, String filePath, String type, boolean append) throws IOException {
        var base64Content = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(derBytes);

        try (var writer = new FileWriter(filePath, append)) {
            writer.write("-----BEGIN " + type + "-----\n");
            writer.write(base64Content);
            writer.write("\n-----END " + type + "-----\n");
        }
    }
}
