package com.vaultdesk.server;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public DatabaseInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {

        // 1. USERS
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                username      TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                full_name     TEXT,
                role          TEXT DEFAULT 'ENGINEER',
                active        INTEGER DEFAULT 1,
                created_at    TEXT
            )
        """);
        System.out.println("users table ready.");

        // 2. DEPARTMENTS
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS departments (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                name     TEXT NOT NULL,
                location TEXT
            )
        """);
        System.out.println("departments table ready.");

        // 3. EMPLOYEES
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS employees (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                name          TEXT NOT NULL,
                emp_code      TEXT,
                department_id INTEGER,
                designation   TEXT,
                email         TEXT,
                phone         TEXT,
                join_date     TEXT,
                leave_date    TEXT,
                active        INTEGER DEFAULT 1,
                notes         TEXT,
                FOREIGN KEY (department_id) REFERENCES departments(id)
            )
        """);
        System.out.println("employees table ready.");

        // 4. ASSETS
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS assets (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                asset_tag       TEXT,
                name            TEXT NOT NULL,
                category        TEXT,
                brand           TEXT,
                model           TEXT,
                serial_number   TEXT,
                processor       TEXT,
                ram_gb          INTEGER,
                storage_gb      INTEGER,
                os              TEXT,
                ip_address      TEXT,
                mac_address     TEXT,
                department_id   INTEGER,
                location        TEXT,
                purchase_date   TEXT,
                warranty_expiry TEXT,
                vendor_name     TEXT,
                purchase_cost   REAL,
                status          TEXT DEFAULT 'Active',
                assigned_to     INTEGER,
                assigned_date   TEXT,
                notes           TEXT,
                created_at      TEXT,
                updated_at      TEXT,
                FOREIGN KEY (department_id) REFERENCES departments(id),
                FOREIGN KEY (assigned_to)   REFERENCES employees(id)
            )
        """);
        System.out.println("assets table ready.");

        // 5. ASSET HISTORY
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS asset_history (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                asset_id      INTEGER,
                action        TEXT,
                from_employee INTEGER,
                to_employee   INTEGER,
                action_date   TEXT,
                notes         TEXT,
                done_by       INTEGER,
                FOREIGN KEY (asset_id) REFERENCES assets(id)
            )
        """);
        System.out.println("asset_history table ready.");

        // 6. TICKETS
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS tickets (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                ticket_no     TEXT UNIQUE,
                title         TEXT NOT NULL,
                description   TEXT,
                category      TEXT,
                priority      TEXT DEFAULT 'Medium',
                status        TEXT DEFAULT 'Open',
                reported_by   INTEGER,
                asset_id      INTEGER,
                assigned_to   INTEGER,
                created_at    TEXT,
                updated_at    TEXT,
                resolved_at   TEXT,
                resolution    TEXT,
                FOREIGN KEY (reported_by) REFERENCES employees(id),
                FOREIGN KEY (asset_id)    REFERENCES assets(id),
                FOREIGN KEY (assigned_to) REFERENCES users(id)
            )
        """);
        System.out.println("tickets table ready.");

        // 7. TICKET COMMENTS
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS ticket_comments (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                ticket_id  INTEGER,
                comment    TEXT,
                added_by   INTEGER,
                added_at   TEXT,
                FOREIGN KEY (ticket_id) REFERENCES tickets(id)
            )
        """);
        System.out.println("ticket_comments table ready.");

        // 8. LICENSES
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS licenses (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                software_name TEXT NOT NULL,
                license_type  TEXT,
                license_key   TEXT,
                seats_total   INTEGER,
                seats_used    INTEGER DEFAULT 0,
                vendor        TEXT,
                purchase_date TEXT,
                expiry_date   TEXT,
                cost          REAL,
                notes         TEXT
            )
        """);
        System.out.println("licenses table ready.");

        // 9. ACTIVITY LOG
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS activity_log (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id    INTEGER,
                action     TEXT,
                table_name TEXT,
                record_id  INTEGER,
                details    TEXT,
                logged_at  TEXT
            )
        """);
        System.out.println("activity_log table ready.");

        // Default admin user (password: admin123)
        // SHA-256 of "admin123"
        jdbc.execute("""
            INSERT OR IGNORE INTO users
            (username, password_hash, full_name, role, active, created_at)
            VALUES (
                'admin',
                '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
                'System Admin',
                'ADMIN',
                1,
                datetime('now')
            )
        """);
        System.out.println("Default admin user ready.");

        System.out.println("=== All tables ready. VaultDesk database initialized. ===");
    }
}