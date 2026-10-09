package com.entity;

public abstract class User {
    private final int id;
    private final String name;

    protected User(int id, String name) {
        if (id < 0 || name == null || name.isBlank() || name.contains("|") || name.contains("\n") || name.contains("\r")) {
            throw new IllegalArgumentException("Invalid user ID or name.");
        }
        this.id = id;
        this.name = name.trim();
    }

    public final int getId() { return id; }
    public final String getName() { return name; }
    public abstract void showProfile();
}
