package com.schemaforge.model;

/** Table constraint kinds shared by the file parser, the live extractor and the converter. */
public enum ConstraintType {
    PRIMARY_KEY("PRIMARY KEY"),
    UNIQUE("UNIQUE"),
    FOREIGN_KEY("FOREIGN KEY"),
    CHECK("CHECK");

    private final String sql;

    ConstraintType(String sql) {
        this.sql = sql;
    }

    /** The keyword(s) used in DDL, e.g. {@code PRIMARY KEY}. */
    public String sql() {
        return sql;
    }
}
