package com.service;

import com.entity.*;
import com.util.FileManager;
import java.io.IOException;
import java.util.*;

/** Business operations and in-memory records; console interaction stays in Main. */
public final class HostelService {
    public record OccupancyReport(int roomCount, int totalBeds, int occupiedBeds, int freeBeds, int activeStudents, String text) { }
    private final FileManager files;
    private final ArrayList<Student> students = new ArrayList<>();
    private final ArrayList<Room> rooms = new ArrayList<>();
    private final ArrayList<Allotment> allotments = new ArrayList<>();
    private final HashSet<String> roomNumbers = new HashSet<>();
    private int nextStudentId = 1, nextRoomId = 1, nextAllotmentId = 1;

    public HostelService(FileManager files) throws IOException {
        this.files = Objects.requireNonNull(files);
        FileManager.Data data = files.loadAll();
        students.addAll(data.students()); rooms.addAll(data.rooms()); allotments.addAll(data.allotments());
        for (Room room : rooms) roomNumbers.add(room.getRoomNumber());
        nextStudentId = nextId(students.stream().map(Student::getId).toList());
        nextRoomId = nextId(rooms.stream().map(Room::getId).toList());
        nextAllotmentId = nextId(allotments.stream().map(Allotment::getId).toList());
    }
    private static int nextId(List<Integer> ids) { return ids.stream().mapToInt(Integer::intValue).max().orElse(0) + 1; }
    public List<Student> getStudents() { return List.copyOf(students); }
    public List<Room> getRooms() { return List.copyOf(rooms); }
    public List<Allotment> getAllotments() { return List.copyOf(allotments); }
    public Student findStudent(int id) { return students.stream().filter(s -> s.getId() == id).findFirst().orElse(null); }
    public Room findRoom(int id) { return rooms.stream().filter(r -> r.getId() == id).findFirst().orElse(null); }
    public Allotment findAllotment(int id) { return allotments.stream().filter(a -> a.getId() == id).findFirst().orElse(null); }

    public Student registerStudent(String name, String department) throws IOException {
        Student student = new Student(nextStudentId, name, department); students.add(student);
        try { files.saveStudents(students); } catch (IOException | RuntimeException ex) { students.remove(student); throw ex; }
        nextStudentId++; return student;
    }
    public Room addRoom(String roomNumber) throws IOException {
        if (roomNumber == null || roomNumber.isBlank()) throw new IllegalArgumentException("Room number is required.");
        String normalized = roomNumber.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("|") || normalized.contains("\n") || normalized.contains("\r")) throw new IllegalArgumentException("Room number cannot contain | or line breaks.");
        if (roomNumbers.contains(normalized)) throw new IllegalArgumentException("That room number already exists.");
        Room room = new Room(nextRoomId, normalized); rooms.add(room);
        try { files.saveRooms(rooms); } catch (IOException | RuntimeException ex) { rooms.remove(room); throw ex; }
        roomNumbers.add(normalized); nextRoomId++; return room;
    }
    public int[] getBedOccupants(int roomId) {
        if (findRoom(roomId) == null) throw new IllegalArgumentException("Room ID not found.");
        int[] occupants = new int[Room.BED_COUNT];
        for (Allotment a : allotments) if (a.getRoomId() == roomId && a.getStatus() == AllotmentStatus.ACTIVE)
            occupants[a.getBedNumber() - 1] = a.getStudentId();
        return occupants;
    }
    public Allotment getActiveAllotment(int studentId) {
        return allotments.stream().filter(a -> a.getStudentId() == studentId && a.getStatus() == AllotmentStatus.ACTIVE).findFirst().orElse(null);
    }
    public Allotment allotBed(int studentId, int roomId) throws IOException {
        if (findStudent(studentId) == null) throw new IllegalArgumentException("Student ID not found.");
        if (findRoom(roomId) == null) throw new IllegalArgumentException("Room ID not found.");
        if (getActiveAllotment(studentId) != null) throw new IllegalArgumentException("This student already has an ACTIVE allotment.");
        int[] occupants = getBedOccupants(roomId); int bed = 0;
        for (int i = 0; i < occupants.length; i++) if (occupants[i] == 0) { bed = i + 1; break; }
        if (bed == 0) throw new IllegalArgumentException("All four beds in this room are occupied.");
        Allotment allotment = new Allotment(nextAllotmentId, studentId, roomId, bed, AllotmentStatus.ACTIVE);
        allotments.add(allotment);
        try { files.saveAllotments(allotments); } catch (IOException | RuntimeException ex) { allotments.remove(allotment); throw ex; }
        nextAllotmentId++; return allotment;
    }
    public void checkout(int allotmentId) throws IOException {
        Allotment allotment = findAllotment(allotmentId);
        if (allotment == null) throw new IllegalArgumentException("Allotment ID not found.");
        allotment.checkout();
        try { files.saveAllotments(allotments); } catch (IOException | RuntimeException ex) { allotment.restoreActiveAfterSaveFailure(); throw ex; }
    }
    public OccupancyReport showReport() {
        int occupied = 0; Set<Integer> active = new HashSet<>();
        for (Room room : rooms) for (int studentId : getBedOccupants(room.getId())) if (studentId != 0) { occupied++; active.add(studentId); }
        int total = rooms.size() * Room.BED_COUNT;
        String text = "HOSTEL OCCUPANCY REPORT\nRooms: " + rooms.size() + "\nTotal beds: " + total + "\nOccupied beds: " + occupied
                + "\nFree beds: " + (total - occupied) + "\nStudents with an active allotment: " + active.size() + "\n";
        return new OccupancyReport(rooms.size(), total, occupied, total - occupied, active.size(), text);
    }
    public void saveReport() throws IOException { files.saveReport(showReport().text()); }
}
