package com.main;

import com.entity.*;
import com.service.HostelService;
import com.util.FileManager;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;

/** Console entry point. All console input is read as lines and validated here. */

public final class Main {
    private final Scanner scanner = new Scanner(System.in);
    private final HostelService service;

    private Main(HostelService service) { this.service = service; }

    public static void main(String[] args) {
        try {
            HostelService service = new HostelService(new FileManager(Path.of("data")));
            new Main(service).run();
        } catch (EndOfInput ex) {
            System.out.println("\nGoodbye.");
        } catch (IOException ex) {
            System.err.println("Startup failed: " + ex.getMessage());
            System.err.println("Correct the indicated data file and restart. Normal operation has stopped.");
        }
    }

    private void run() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== HOSTEL ROOM ALLOTMENT SYSTEM ===");
            System.out.println("1. Register student\n2. Admin menu\n3. Student menu\n0. Exit");
            switch (readChoice("Choice: ", 0, 3)) {
                case 1 -> registerStudent();
                case 2 -> adminMenu();
                case 3 -> studentMenu();
                case 0 -> running = false;
                default -> throw new IllegalStateException();
            }
        }
        System.out.println("Goodbye.");
    }

    private void adminMenu() {
        Admin admin = new Admin();
        boolean back = false;
        while (!back) {
            System.out.println("\n=== ADMIN MENU ===");
            System.out.println("1. Add room\n2. View rooms and beds\n3. View students\n4. Allot bed\n5. Record checkout\n6. View reports\n0. Back");
            switch (readChoice("Choice: ", 0, 6)) {
                case 1 -> addRoom();
                case 2 -> showRooms();
                case 3 -> showStudents();
                case 4 -> allotBed();
                case 5 -> checkout();
                case 6 -> reports();
                case 0 -> back = true;
                default -> throw new IllegalStateException();
            }
        }
        admin.showProfile();
    }

    private void studentMenu() {
        Student student = null;
        while (student == null) {
            String value = readLine("Enter student ID (0 to go back): ");
            Integer id = parseInteger(value);
            if (id == null) { System.out.println("Enter a valid numeric ID."); continue; }
            if (id == 0) return;
            student = service.findStudent(id);
            if (student == null) System.out.println("Student ID not found. Try again.");
        }
        boolean back = false;
        while (!back) {
            System.out.println("\n=== STUDENT MENU (ID " + student.getId() + ") ===");
            System.out.println("1. View my profile\n2. View rooms\n3. View my active allotment\n0. Back");
            switch (readChoice("Choice: ", 0, 3)) {
                case 1 -> student.showProfile();
                case 2 -> showRooms();
                case 3 -> showStudentAllotment(student.getId());
                case 0 -> back = true;
                default -> throw new IllegalStateException();
            }
        }
    }

    private void registerStudent() {
        String name = readRequired("Student name: ");
        String department = readRequired("Department: ");
        try {
            Student student = service.registerStudent(name, department);
            System.out.println("Student " + student.getId() + " saved.");
        } catch (IOException | IllegalArgumentException ex) { showError(ex); }
    }
    private void addRoom() {
        String number = readRequired("Room number: ");
        try {
            Room room = service.addRoom(number);
            System.out.println("Room " + room.getId() + " saved. Beds: 4");
        } catch (IOException | IllegalArgumentException ex) { showError(ex); }
    }
    private void showRooms() {
        if (service.getRooms().isEmpty()) { System.out.println("No records found."); return; }
        for (Room room : service.getRooms()) room.showDetails(service.getBedOccupants(room.getId()));
    }
    private void showStudents() {
        if (service.getStudents().isEmpty()) { System.out.println("No records found."); return; }
        for (Student student : service.getStudents()) student.showProfile();
    }
    private void allotBed() {
        int studentId = readExistingStudentId();
        int roomId = readExistingRoomId();
        try {
            Allotment a = service.allotBed(studentId, roomId);
            System.out.println("Allotment " + a.getId() + " saved. Bed: " + a.getBedNumber());
        } catch (IOException | IllegalArgumentException ex) { showError(ex); }
    }
    private void checkout() {
        int id = readExistingAllotmentId();
        try { service.checkout(id); System.out.println("Checkout saved."); }
        catch (IOException | IllegalArgumentException | IllegalStateException ex) { showError(ex); }
    }
    private void showStudentAllotment(int studentId) {
        Allotment a = service.getActiveAllotment(studentId);
        if (a == null) { System.out.println("No active allotment found."); return; }
        Room room = service.findRoom(a.getRoomId());
        System.out.printf("Room: %s | Bed: %d | %s%n", room.getRoomNumber(), a.getBedNumber(), a.getStatus());
    }
    private void reports() {
        HostelService.OccupancyReport report = service.showReport();
        System.out.print(report.text());
        if (readYesNo("Save report to report.txt? (y/n): ")) {
            try { service.saveReport(); System.out.println("Report saved."); }
            catch (IOException ex) { showError(ex); }
        }
    }

    private String readRequired(String prompt) {
        while (true) {
            String value = readLine(prompt).trim();
            if (value.isEmpty()) { System.out.println("This field is required."); continue; }
            if (value.contains("|") || value.contains("\n") || value.contains("\r")) { System.out.println("The value cannot contain | or line breaks."); continue; }
            return value;
        }
    }
    private int readInt(String prompt) {
        while (true) {
            Integer value = parseInteger(readLine(prompt));
            if (value != null && value > 0) return value;
            System.out.println("Enter a positive whole number.");
        }
    }
    private int readExistingStudentId() {
        while (true) {
            int id = readInt("Student ID: ");
            if (service.findStudent(id) != null) return id;
            System.out.println("Student ID not found. Try again.");
        }
    }
    private int readExistingRoomId() {
        while (true) {
            int id = readInt("Room ID: ");
            if (service.findRoom(id) != null) return id;
            System.out.println("Room ID not found. Try again.");
        }
    }
    private int readExistingAllotmentId() {
        while (true) {
            int id = readInt("Allotment ID: ");
            if (service.findAllotment(id) != null) return id;
            System.out.println("Allotment ID not found. Try again.");
        }
    }
    private int readChoice(String prompt, int min, int max) {
        while (true) {
            Integer value = parseInteger(readLine(prompt));
            if (value != null && value >= min && value <= max) return value;
            System.out.println("Choose a number from " + min + " to " + max + ".");
        }
    }
    private boolean readYesNo(String prompt) {
        while (true) {
            String value = readLine(prompt).trim();
            if (value.equalsIgnoreCase("y")) return true;
            if (value.equalsIgnoreCase("n")) return false;
            System.out.println("Enter y or n.");
        }
    }
    private String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) throw new EndOfInput();
        return scanner.nextLine();
    }
    private static Integer parseInteger(String value) {
        try { return Integer.valueOf(value.trim()); } catch (NumberFormatException ex) { return null; }
    }
    private static void showError(Exception ex) { System.out.println("Error: " + ex.getMessage()); }

    private static final class EndOfInput extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
