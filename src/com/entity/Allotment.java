package com.entity;

public final class Allotment {
    private final int id;
    private final int studentId;
    private final int roomId;
    private final int bedNumber;
    private AllotmentStatus status;

    public Allotment(int id, int studentId, int roomId, int bedNumber, AllotmentStatus status) {
        if (id < 1 || studentId < 1 || roomId < 1 || bedNumber < 1 || bedNumber > Room.BED_COUNT || status == null)
            throw new IllegalArgumentException("Invalid allotment values.");
        this.id = id; this.studentId = studentId; this.roomId = roomId; this.bedNumber = bedNumber; this.status = status;
    }
    public int getId() { return id; }
    public int getStudentId() { return studentId; }
    public int getRoomId() { return roomId; }
    public int getBedNumber() { return bedNumber; }
    public AllotmentStatus getStatus() { return status; }
    public void checkout() {
        if (status != AllotmentStatus.ACTIVE) throw new IllegalStateException("This allotment has already been checked out.");
        status = AllotmentStatus.CHECKED_OUT;
    }
    /** Used only to roll back an unsuccessful persistence operation. */
    public void restoreActiveAfterSaveFailure() { status = AllotmentStatus.ACTIVE; }
}
