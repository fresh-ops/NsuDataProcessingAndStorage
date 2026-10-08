package ru.nsu.g.solovev5.m.taskj1.shared;

import java.util.UUID;

public record Response(
    UUID connectionId,
    byte[] message
) {
}
