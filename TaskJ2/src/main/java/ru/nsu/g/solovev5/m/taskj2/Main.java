package ru.nsu.g.solovev5.m.taskj2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Scanner;

public class Main {
    static void main() throws InterruptedException {
        var scanner = new Scanner(System.in);
        System.out.print("Enter sorting threads number >>> ");
        var threadsNumber = scanner.nextInt();
        System.out.print("Enter sleep duration >>> ");
        var sleepDuration = scanner.nextLong();
        scanner.nextLine();
        String userChoice;

        do {
            System.out.print("Use custom list? (y/n) >>> ");
            userChoice = scanner.nextLine();
        } while (!userChoice.equals("y") && !userChoice.equals("n"));

        if (userChoice.equals("y")) {
            runMyLinkedListSorting(scanner, threadsNumber, sleepDuration);
        } else {
            runSynchronizedListSorting(scanner, threadsNumber, sleepDuration);
        }
    }

    private static void runMyLinkedListSorting(
        Scanner scanner,
        int threadsNumber,
        long sleepDuration
    ) throws InterruptedException {
        var lines = new MyLinkedList<String>();
        var threads = new Thread[threadsNumber];
        for (var i = 0; i < threadsNumber; i++) {
            threads[i] = new Thread(new MyLinkedListSorter(lines, sleepDuration));
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

    private static void runSynchronizedListSorting(
        Scanner scanner,
        int threadsNumber,
        long sleepDuration
    ) throws InterruptedException {
        var lines = Collections.synchronizedList(new ArrayList<String>());
        var threads = new Thread[threadsNumber];
        for (var i = 0; i < threadsNumber; i++) {
            threads[i] = new Thread(new SynchronizedListSorter(lines, sleepDuration));
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
                lines.addFirst(input);
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
