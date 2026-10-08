package ru.nsu.g.solovev5.m.taskj1.certificates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.PrivateKey;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

/**
 * A utility class for reading pem keys.
 */
public class PemKeyReader {
    private PemKeyReader() {
    }

    /**
     * Reads private key from the specified file.
     *
     * @param path path to the containing a private key
     * @return a parsed private key
     * @throws IOException in case of a parse error
     */
    public static PrivateKey readPrivateKey(final Path path) throws IOException {
        if (path == null) {
            throw new NullPointerException("Cannot read private key from null");
        }
        if (!Files.exists(path)) {
            throw new NoSuchFileException("File not found: " + path);
        }

        try (var reader = Files.newBufferedReader(path); var pemParser = new PEMParser(reader)) {
            var converter = new JcaPEMKeyConverter();
            var parsed = pemParser.readObject();

            if (parsed == null) {
                throw new IOException("Empty or invalid PEM file: " + path);
            }

            return switch (parsed) {
                case PrivateKeyInfo keyInfo -> converter.getPrivateKey(keyInfo);
                case PEMKeyPair keyPair -> converter.getKeyPair(keyPair).getPrivate();
                default -> throw new IOException("Unsupported key type: " + path);
            };
        }
    }
}
