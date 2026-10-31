package ru.nsu.g.solovev5.m.taskj3;

public record ServerResponseBody(
    String message,
    String[] successors
) {
}
