package com.entity;

public final class Student extends User {
    private final String department;

    public Student(int id, String name, String department) {
        super(id, name);
        if (id < 1) throw new IllegalArgumentException("Student IDs must be positive.");
        if (department == null || department.isBlank() || department.contains("|") || department.contains("\n") || department.contains("\r")) {
            throw new IllegalArgumentException("Department is required and cannot contain | or line breaks.");
        }
        this.department = department.trim();
    }

    public String getDepartment() { return department; }

    @Override
    public void showProfile() {
        System.out.printf("Student ID: %d | Name: %s | Department: %s%n", getId(), getName(), department);
    }
}
