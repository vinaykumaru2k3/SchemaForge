package com.schemaforge.model;

/** Where a migration run's schema came from. Stored as its name in the database. */
public enum SourceType {
    FILE, LIVE_DB
}
