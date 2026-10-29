package ru.nsu.g.solovev5.m.taskj3;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

public class Main {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private static final ConcurrentLinkedQueue<String> messagesQueue = new ConcurrentLinkedQueue<>();
    private static final Phaser phaser = new Phaser(0);

    static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Expected 1 argument, but " + args.length + " passed");
            return;
        }

        var root = args[0];
        if (!root.endsWith("/")) {
            root += '/';
        }
        try {
            var _ = URI.create(root).toURL();
        } catch (Exception e) {
            System.err.println("Bad url format: " + e.getMessage());
            return;
        }

        phaser.register();
        executor.submit(createRequestRunnable(root, ""));
        phaser.awaitAdvance(0);

        executor.close();
        httpClient.close();

        var sortedMessages = messagesQueue.stream()
            .sorted()
            .toArray(String[]::new);

        System.out.println("Total: " + sortedMessages.length);
        for (var message : sortedMessages) {
            System.out.println(message);
        }
    }

    private static Runnable createRequestRunnable(String root, String path) {
        return () -> {
            var request = HttpRequest.newBuilder()
                .uri(URI.create(root + path))
                .GET()
                .build();

            try {
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    var parser = new Gson();
                    var body = parser.fromJson(response.body(), ServerResponseBody.class);

                    messagesQueue.add(body.message());
                    for (var successor : body.successors()) {
                        phaser.register();
                        executor.submit(createRequestRunnable(root, successor));
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                phaser.arriveAndDeregister();
            }
        };
    }
}
