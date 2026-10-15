package ru.nsu.g.solovev5.m.taskj2;

import java.util.Scanner;

public class Main {
    static void main() {
        var scanner = new Scanner(System.in);

        System.out.print(">>> ");
        System.out.flush();
        while (scanner.hasNext()) {
            var input = scanner.nextLine();
            System.out.println(input);

            System.out.print(">>> ");
            System.out.flush();
        }

        System.out.println("\nBye.");
    }
}
