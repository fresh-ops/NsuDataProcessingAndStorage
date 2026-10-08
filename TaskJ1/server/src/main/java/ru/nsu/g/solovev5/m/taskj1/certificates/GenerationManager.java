package ru.nsu.g.solovev5.m.taskj1.certificates;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.OperatorCreationException;
import ru.nsu.g.solovev5.m.taskj1.shared.AbstractService;
import ru.nsu.g.solovev5.m.taskj1.shared.Request;
import ru.nsu.g.solovev5.m.taskj1.shared.Response;

public class GenerationManager extends AbstractService {
    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 8192;

    private final BlockingQueue<Request> requests;
    private final BlockingQueue<Response> responses;
    private final ExecutorService executor;

    private final CertificateGenerator certificateGenerator;
    private final ThreadLocal<KeyPairGenerator> keyPairGenerator;
    private final ConcurrentHashMap<String, Set<UUID>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Creates a new GenerationManager.
     *
     * @param issuer      the issuer name
     * @param signingKey  the issuer signing key
     * @param idGenerator the certificate id generator. It MUST be thread-safe
     * @param requests    the queue of new requests
     * @param responses   the queue of resolved responses
     * @param executor    the thread pool
     * @throws OperatorCreationException if failed to recognize passed signature algorithm
     */
    public GenerationManager(
        X500Name issuer,
        PrivateKey signingKey,
        Supplier<BigInteger> idGenerator,
        BlockingQueue<Request> requests,
        BlockingQueue<Response> responses,
        ExecutorService executor
    ) throws OperatorCreationException {
        this.requests = requests;
        this.responses = responses;
        this.executor = executor;

        certificateGenerator = new CertificateGenerator(issuer, signingKey, idGenerator);
        keyPairGenerator = ThreadLocal.withInitial(() -> {
            try {
                var kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM);
                kpg.initialize(KEY_SIZE);
                return kpg;
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Override
    protected void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                var request = requests.take();

                processRequest(request);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void processRequest(Request request) {
        Set<UUID> newSet = ConcurrentHashMap.newKeySet();

        pendingRequests.compute(request.message(), (_, connections) -> {
            if (connections == null) {
                newSet.add(request.connectionId());
                return newSet;
            }
            connections.add(request.connectionId());
            return connections;
        });

        if (newSet.isEmpty()) return;

        executor.submit(() -> {
            try {
                var keys = keyPairGenerator.get().generateKeyPair();
                var certificate = certificateGenerator.generate(
                    new X500Name(request.message()),
                    keys.getPublic()
                );

                var buffer = new ByteArrayOutputStream();
                buffer.write(certificate.getEncoded().length);
                buffer.write(certificate.getEncoded());
                buffer.write(keys.getPrivate().getEncoded().length);
                buffer.write(keys.getPrivate().getEncoded());
                buffer.write(keys.getPublic().getEncoded().length);
                buffer.write(keys.getPublic().getEncoded());
                var bytes = buffer.toByteArray();
                buffer.reset();

                sendResponse(request.message(), bytes);
            } catch (Exception e) {
                System.err.println("Failed to generate certificate: " + e.getMessage());
            }
        });
    }

    private void sendResponse(String request, byte[] response) {
        var connections = pendingRequests.remove(request);
        if (connections == null) return;

        for (var connection : connections) {
            responses.add(new Response(connection, response));
        }
    }
}
