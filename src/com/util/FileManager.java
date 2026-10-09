package com.util;

import com.entity.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;

/** UTF-8 text persistence with replacement-based atomic saves. */
public final class FileManager {
    public record Data(List<Student> students, List<Room> rooms, List<Allotment> allotments) { }
    private final Path dataDirectory;
    private final Path studentsFile, roomsFile, allotmentsFile, reportFile;

    public FileManager(Path dataDirectory) throws IOException {
        this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
        Files.createDirectories(this.dataDirectory);
        studentsFile = this.dataDirectory.resolve("students.txt");
        roomsFile = this.dataDirectory.resolve("rooms.txt");
        allotmentsFile = this.dataDirectory.resolve("allotments.txt");
        reportFile = this.dataDirectory.resolve("report.txt");
    }

    public Data loadAll() throws IOException {
        List<Student> students = read(studentsFile, 3, f -> new Student(number(f[0]), f[1], f[2]));
        List<Room> rooms = read(roomsFile, 2, f -> new Room(number(f[0]), f[1]));
        List<Allotment> allotments = read(allotmentsFile, 5, f -> {
            try { return new Allotment(number(f[0]), number(f[1]), number(f[2]), number(f[3]), AllotmentStatus.valueOf(f[4])); }
            catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Invalid allotment status or values.", ex); }
        });
        ensureUnique(students.stream().map(Student::getId).toList(), studentsFile, "student ID");
        ensureUnique(rooms.stream().map(Room::getId).toList(), roomsFile, "room ID");
        Set<String> roomNumbers = new HashSet<>();
        for (Room room : rooms) if (!roomNumbers.add(room.getRoomNumber())) throw new IOException(roomsFile.getFileName() + ": duplicate room number " + room.getRoomNumber());
        ensureUnique(allotments.stream().map(Allotment::getId).toList(), allotmentsFile, "allotment ID");
        Set<Integer> studentIds = new HashSet<>(); students.forEach(s -> studentIds.add(s.getId()));
        Set<Integer> roomIds = new HashSet<>(); rooms.forEach(r -> roomIds.add(r.getId()));
        Set<Integer> activeStudents = new HashSet<>(); Set<String> activeBeds = new HashSet<>();
        for (Allotment a : allotments) {
            if (!studentIds.contains(a.getStudentId()) || !roomIds.contains(a.getRoomId()))
                throw new IOException(allotmentsFile.getFileName() + ": allotment " + a.getId() + " references a missing student or room");
            if (a.getStatus() == AllotmentStatus.ACTIVE) {
                if (!activeStudents.add(a.getStudentId())) throw new IOException(allotmentsFile.getFileName() + ": student has multiple ACTIVE allotments");
                if (!activeBeds.add(a.getRoomId() + ":" + a.getBedNumber())) throw new IOException(allotmentsFile.getFileName() + ": duplicate ACTIVE room and bed");
            }
        }
        return new Data(students, rooms, allotments);
    }

    private static int number(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ex) { throw new IllegalArgumentException("Expected an integer.", ex); }
    }
    private static void ensureUnique(List<Integer> ids, Path file, String label) throws IOException {
        if (new HashSet<>(ids).size() != ids.size()) throw new IOException(file.getFileName() + ": duplicate " + label);
    }
    private <T> List<T> read(Path path, int fieldsExpected, Function<String[], T> parser) throws IOException {
        List<T> result = new ArrayList<>();
        if (!Files.exists(path)) return result;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line; int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] fields = line.split("\\|", -1);
                    if (fields.length != fieldsExpected) throw new IllegalArgumentException("Expected " + fieldsExpected + " fields.");
                    result.add(parser.apply(fields));
                } catch (RuntimeException ex) {
                    throw new IOException(path.getFileName() + ":" + lineNumber + ": " + ex.getMessage(), ex);
                }
            }
        } catch (IOException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith(path.getFileName().toString() + ":")) throw ex;
            throw new IOException("Cannot read " + path.getFileName() + ": " + ex.getMessage(), ex);
        }
        return result;
    }

    public void saveStudents(List<Student> values) throws IOException {
        save(studentsFile, values, s -> s.getId() + "|" + clean(s.getName()) + "|" + clean(s.getDepartment()));
    }
    public void saveRooms(List<Room> values) throws IOException {
        save(roomsFile, values, r -> r.getId() + "|" + clean(r.getRoomNumber()));
    }
    public void saveAllotments(List<Allotment> values) throws IOException {
        save(allotmentsFile, values, a -> a.getId() + "|" + a.getStudentId() + "|" + a.getRoomId() + "|" + a.getBedNumber() + "|" + a.getStatus());
    }
    private static String clean(String value) {
        if (value.contains("|") || value.contains("\n") || value.contains("\r")) throw new IllegalArgumentException("Text cannot contain | or line breaks.");
        return value;
    }
    private <T> void save(Path target, List<T> values, Function<T, String> formatter) throws IOException {
        Path temporary = Files.createTempFile(dataDirectory, target.getFileName().toString(), ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)) {
                for (T value : values) { writer.write(formatter.apply(value)); writer.newLine(); }
            }
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    public void saveReport(String report) throws IOException {
        Path temporary = Files.createTempFile(dataDirectory, "report.txt", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)) { writer.write(report); }
            try { Files.move(temporary, reportFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, reportFile, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
