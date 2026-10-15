package ru.nsu.g.solovev5.m.taskj2;

import java.util.Scanner;

public class Main {
    static void main() {
        var scanner = new Scanner(System.in);
        var lines = new MyLinkedList<String>();

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

        System.out.println("\nBye.");
    }
}
