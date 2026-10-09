package com.entity;

public final class Admin extends User {
    public Admin() { super(0, "Hostel Admin"); }

    @Override
    public void showProfile() { System.out.println("Admin ID: 0 | " + getName()); }
}
