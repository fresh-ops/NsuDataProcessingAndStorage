package ru.nsu.g.solovev5.m.taskj1.certificates;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;
import java.util.function.Supplier;

/**
 * A thread-safe secure id generator.
 */
public class IdGenerator implements Supplier<BigInteger> {
    private final Random random = new  SecureRandom();

    @Override
    public BigInteger get() {
        return new BigInteger(159, random);
    }
}
