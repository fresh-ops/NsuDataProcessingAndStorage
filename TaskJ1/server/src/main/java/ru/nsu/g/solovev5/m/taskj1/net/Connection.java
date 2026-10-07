package ru.nsu.g.solovev5.m.taskj1.net;

import java.nio.channels.SocketChannel;
import java.util.UUID;

public record Connection(
    UUID id,
    SocketChannel channel
) {
}
