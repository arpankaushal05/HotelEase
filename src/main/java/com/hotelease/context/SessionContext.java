package com.hotelease.context;

import com.hotelease.model.User;

public class SessionContext {

    private static final SessionContext INSTANCE = new SessionContext();

    private User currentUser;

    private SessionContext() {
    }

    public static SessionContext getInstance() {
        return INSTANCE;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public void clear() {
        currentUser = null;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }
}
