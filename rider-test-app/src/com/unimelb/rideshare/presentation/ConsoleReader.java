package com.unimelb.rideshare.presentation;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Scanner;

/**
 * Lightweight console helper for gathering user input.
 */
public final class ConsoleReader {
    private final Scanner scanner = new Scanner(System.in);

    public String readLine(String prompt) {
        System.out.print(prompt + ": ");
        return scanner.nextLine().trim();
    }

    public int readInt(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    public DayOfWeek readDay(String prompt) {
        while (true) {
            String value = readLine(prompt + " (e.g., MONDAY)").toUpperCase();
            try {
                return DayOfWeek.valueOf(value);
            } catch (IllegalArgumentException ex) {
                System.out.println("Invalid day of week.");
            }
        }
    }

    public LocalTime readTime(String prompt) {
        while (true) {
            String value = readLine(prompt + " (HH:mm)");
            try {
                return LocalTime.parse(value);
            } catch (Exception ex) {
                System.out.println("Invalid time format.");
            }
        }
    }
}
