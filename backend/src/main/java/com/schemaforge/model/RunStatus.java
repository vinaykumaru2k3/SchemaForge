package com.schemaforge.model;

/** Lifecycle state of a migration run. Stored as its name in the database. */
public enum RunStatus {
    PENDING, PARSED, ANALYZED, CONVERTED, VALIDATED, FAILED
}
