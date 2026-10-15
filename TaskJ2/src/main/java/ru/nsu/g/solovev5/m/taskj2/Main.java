package ru.nsu.g.solovev5.m.taskj2;

import java.util.Scanner;

public class Main {
    static void main() throws InterruptedException {
        var scanner = new Scanner(System.in);
        var lines = new MyLinkedList<String>();
        var threads = new Thread[4];
        for (var i = 0; i < 4; i++) {
            threads[i] = new Thread(new MyLinkedListSorter(lines, 2_000L));
            threads[i].start();
        }

        System.out.print(">>> ");
        System.out.flush();
        while (scanner.hasNextLine()) {
            var input = scanner.nextLine();
            if (input.isEmpty()) {
                for (var line : lines) {
                    System.out.println(line);
                }
            } else {
                lines.pushFront(input);
            }

            System.out.print(">>> ");
            System.out.flush();
        }

        for (var thread : threads) {
            thread.interrupt();
        }
        try {
            for (var thread : threads) {
                thread.join();
            }
        } finally {
            System.out.println("\nBye.");
        }
    }
}
