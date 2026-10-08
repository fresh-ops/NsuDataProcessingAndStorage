package ru.nsu.g.solovev5.m.taskj1.certificates;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
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
    private final ConcurrentHashMap<String, CompletableFuture<byte[]>> cached = new ConcurrentHashMap<>();

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
        var future = cached.computeIfAbsent(
            request.message(),
            key -> CompletableFuture.supplyAsync(() -> generateResponse(key), executor)
        );

        future.thenAcceptAsync(response -> {
            try {
                if (response != null) {
                    responses.put(new Response(request.connectionId(), response));
                }
            } catch (InterruptedException _) {
            }
        }, executor);
    }

    private byte[] generateResponse(String request) {
        try {
            var keyPair = keyPairGenerator.get().generateKeyPair();
            var certificate = certificateGenerator.generate(
                new X500Name(request),
                keyPair.getPublic()
            );

            var buffer = new ByteArrayOutputStream();
            try (var dataOut = new DataOutputStream(buffer)) {
                dataOut.writeInt(certificate.getEncoded().length);
                dataOut.write(certificate.getEncoded());
                dataOut.writeInt(keyPair.getPrivate().getEncoded().length);
                dataOut.write(keyPair.getPrivate().getEncoded());
                dataOut.writeInt(keyPair.getPublic().getEncoded().length);
                dataOut.write(keyPair.getPublic().getEncoded());
            }
            return buffer.toByteArray();
        } catch (CertificateException | IOException e) {
            System.err.println("Failed to generate certificate for " + request);
            cached.remove(request);
            return null;
        }
    }
}
