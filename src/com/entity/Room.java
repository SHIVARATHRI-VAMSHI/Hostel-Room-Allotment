package com.entity;

import java.util.Arrays;

public final class Room {
    public static final int BED_COUNT = 4;
    private final int id;
    private final String roomNumber;

    public Room(int id, String roomNumber) {
        if (id < 1 || roomNumber == null || roomNumber.isBlank() || roomNumber.contains("|")
                || roomNumber.contains("\n") || roomNumber.contains("\r")) {
            throw new IllegalArgumentException("Invalid room ID or number.");
        }
        this.id = id;
        this.roomNumber = roomNumber.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public int getId() { return id; }
    public String getRoomNumber() { return roomNumber; }
    public void showDetails(int[] occupants) {
        if (occupants == null || occupants.length != BED_COUNT) throw new IllegalArgumentException("Exactly four occupants are required.");
        System.out.printf("Room ID: %d | Room: %s | Beds: %s%n", id, roomNumber, Arrays.toString(occupants));
    }
}
