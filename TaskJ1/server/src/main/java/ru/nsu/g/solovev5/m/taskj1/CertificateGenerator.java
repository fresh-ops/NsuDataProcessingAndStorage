package ru.nsu.g.solovev5.m.taskj1;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * A X509v3 certificate generator. It produces certificates for dedicated subjects from
 * the exact one issuer.
 */
public class CertificateGenerator {
    private static final long DEFAULT_CERTIFICATE_VALIDITY_DAYS = 365L;
    private static final String DEFAULT_SIGNATURE_ALGORITHM = "SHA256withRSA";

    private final X500Name issuer;
    private final Supplier<BigInteger> idGenerator;
    private final ContentSigner contentSigner;
    private final JcaX509CertificateConverter converter;

    /**
     * Creates a new CertificateGenerator.
     *
     * @param issuer             the issuer of generating certificates
     * @param signingKey         the key to sign generating certificates
     * @param idGenerator        the certificate id generator. It should produce non-colliding ids
     * @param signatureAlgorithm the name of signature algorithm
     * @throws OperatorCreationException if failed to recognize passed signature algorithm
     */
    public CertificateGenerator(
        X500Name issuer,
        PrivateKey signingKey,
        Supplier<BigInteger> idGenerator,
        String signatureAlgorithm
    ) throws OperatorCreationException {
        this.idGenerator = idGenerator;
        this.issuer = issuer;
        this.contentSigner = new JcaContentSignerBuilder(signatureAlgorithm)
            .build(signingKey);
        this.converter = new JcaX509CertificateConverter();
    }

    /**
     * Creates a new CertificateGenerator with SHA256 with RSA signature algorithm.
     *
     * @param issuer      the issuer of generating certificates
     * @param signingKey  the key to sign generating certificates
     * @param idGenerator the certificate id generator. It should produce non-colliding ids
     * @throws OperatorCreationException if failed to recognize passed signature algorithm
     */
    public CertificateGenerator(
        X500Name issuer,
        PrivateKey signingKey,
        Supplier<BigInteger> idGenerator
    ) throws OperatorCreationException {
        this(issuer, signingKey, idGenerator, DEFAULT_SIGNATURE_ALGORITHM);
    }

    /**
     * Generates a new certificate.
     *
     * @param subjectName  the certificate subject
     * @param publicKey    the key to be associated with the certificate
     * @param validityDays the certificate validity in days
     * @return a new certificate
     * @throws CertificateException if failed to create a certificate
     */
    public X509Certificate generate(
        X500Name subjectName,
        PublicKey publicKey,
        long validityDays
    ) throws CertificateException {
        var startTime = Instant.now();
        var endTime = startTime.plus(validityDays, ChronoUnit.DAYS);

        var certificateBuilder = new X509v3CertificateBuilder(
            issuer,
            idGenerator.get(),
            Date.from(startTime),
            Date.from(endTime),
            subjectName,
            SubjectPublicKeyInfo.getInstance(publicKey.getEncoded())
        );

        return converter.getCertificate(certificateBuilder.build(contentSigner));
    }

    /**
     * Generates a new certificate with a 1-year validity
     *
     * @param subjectName the certificate subject
     * @param publicKey   the key to be associated with the certificate
     * @return a new certificate
     * @throws CertificateException if failed to create a certificate
     */
    public X509Certificate generate(X500Name subjectName, PublicKey publicKey) throws CertificateException {
        return generate(subjectName, publicKey, DEFAULT_CERTIFICATE_VALIDITY_DAYS);
    }
}
