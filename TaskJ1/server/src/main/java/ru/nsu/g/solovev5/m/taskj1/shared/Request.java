package ru.nsu.g.solovev5.m.taskj1.shared;

import java.util.UUID;

public record Request(
    UUID connectionId,
    String message
) {
}
