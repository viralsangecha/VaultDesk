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

        // ─────────────────────────────────────────────
        // 1. USERS
        // ─────────────────────────────────────────────
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
        // Add last_login column if it doesn't exist (safe migration)
        try {
            jdbc.execute("ALTER TABLE users ADD COLUMN last_login TEXT");
            System.out.println("✔ last_login column added.");
        } catch (Exception e) {
            // Column already exists, ignore
        }
        try {
            jdbc.execute("ALTER TABLE users ADD COLUMN dept_id INTEGER DEFAULT 0");
            System.out.println("✔ dept_id column added to users.");
        } catch (Exception e) {
            // Column already exists, ignore
        }
        System.out.println("✔ users table ready.");

        // ─────────────────────────────────────────────
        // 2. DEPARTMENTS
        // ─────────────────────────────────────────────
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS departments (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                name     TEXT NOT NULL,
                location TEXT
            )
        """);
        System.out.println("✔ departments table ready.");

        // ─────────────────────────────────────────────
        // 3. EMPLOYEES
        // ─────────────────────────────────────────────
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
        System.out.println("✔ employees table ready.");

        // ─────────────────────────────────────────────
        // 4. VENDOR CONTACTS
        //    Suppliers, AMC partners, service vendors
        // ─────────────────────────────────────────────
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS vendor_contacts (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                name           TEXT NOT NULL,
                contact_person TEXT,
                phone          TEXT,
                email          TEXT,
                category       TEXT,
                address        TEXT,
                notes          TEXT
            )
        """);
        System.out.println("✔ vendor_contacts table ready.");

        // ─────────────────────────────────────────────
        // 5. ASSETS  (universal — all device types)
        //    PC, Laptop, Server, Printer, MFP, Scanner,
        //    CCTV Camera, DVR/NVR, Network Switch,
        //    Router, Firewall, Access Point, UPS,
        //    Projector, TV/Screen, Desk Phone,
        //    Mobile Phone, Tablet, Biometric Device,
        //    Card Reader, Intercom, External HDD,
        //    Pendrive, Other
        //    Specs (ram, processor, IP, etc.)
        //    are stored in asset_specs key-value table.
        // ─────────────────────────────────────────────
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS assets (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                asset_tag       TEXT UNIQUE,
                name            TEXT NOT NULL,
                category        TEXT,
                brand           TEXT,
                model           TEXT,
                serial_number   TEXT,
                department_id   INTEGER,
                location        TEXT,
                status          TEXT DEFAULT 'Active',
                assigned_to     INTEGER,
                assigned_date   TEXT,
                purchase_date   TEXT,
                warranty_expiry TEXT,
                vendor_id       INTEGER,
                purchase_cost   REAL,
                notes           TEXT,
                created_at      TEXT DEFAULT (datetime('now')),
                updated_at      TEXT,
                FOREIGN KEY (department_id) REFERENCES departments(id),
                FOREIGN KEY (assigned_to)   REFERENCES employees(id),
                FOREIGN KEY (vendor_id)     REFERENCES vendor_contacts(id)
            )
        """);
        System.out.println("✔ assets table ready.");

        // ─────────────────────────────────────────────
        // 6. ASSET SPECS  (flexible key-value per asset)
        //    Examples:
        //    PC/Laptop  → processor, ram_gb, storage_gb,
        //                  os, ip_address, mac_address,
        //                  gpu, monitor_size
        //    CCTV       → resolution, lens_mm, ir_range_m,
        //                  type (Dome/Bullet/PTZ),
        //                  ip_address, mac_address
        //    DVR/NVR    → channels, hdd_slots, ip_address
        //    Printer    → print_speed_ppm, print_type,
        //                  ip_address, mac_address,
        //                  supported_cartridge
        //    Switch     → ports, managed, ip_address,
        //                  mac_address, poe
        //    Router     → wan_ports, lan_ports, ip_address,
        //                  mac_address, wifi
        //    UPS        → capacity_va, battery_type,
        //                  runtime_minutes
        //    Mobile     → imei, sim_number, os
        //    Biometric  → ip_address, type (Fingerprint/Face)
        //    Any other spec you need in future
        // ─────────────────────────────────────────────
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS asset_specs (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                asset_id    INTEGER NOT NULL,
                spec_key    TEXT NOT NULL,
                spec_value  TEXT,
                FOREIGN KEY (asset_id) REFERENCES assets(id)
            )
        """);
        System.out.println("✔ asset_specs table ready.");

        // ─────────────────────────────────────────────
        // 7. COMPONENTS  (individual parts inside assets)
        //    PC/Laptop  → RAM stick, HDD, SSD, NVMe,
        //                  CPU, GPU, PSU, NIC, Keyboard,
        //                  Mouse, Monitor
        //    CCTV       → Lens, IR Board, Housing, PSU
        //    DVR/NVR    → HDD, DVR Card
        //    UPS        → Battery pack
        //    Laptop     → Battery, Adapter
        //    Network    → SFP Module, Fiber Transceiver
        //    Server     → RAM, HDD, RAID card, PSU
        //    Printer    → Fuser unit, Drum unit
        //    Any other part going forward
        // ─────────────────────────────────────────────
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS components (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                asset_id       INTEGER,
                component_type TEXT NOT NULL,
                brand          TEXT,
                model          TEXT,
                serial_number  TEXT,
                specs          TEXT,
                status         TEXT DEFAULT 'In Use',
                purchase_date  TEXT,
                notes          TEXT,
                created_at     TEXT DEFAULT (datetime('now')),
                FOREIGN KEY (asset_id) REFERENCES assets(id)
            )
        """);
        System.out.println("✔ components table ready.");

        // ─────────────────────────────────────────────
        // 8. ASSET HISTORY  (movement + assignment log)
        // ─────────────────────────────────────────────
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
                FOREIGN KEY (asset_id)      REFERENCES assets(id),
                FOREIGN KEY (from_employee) REFERENCES employees(id),
                FOREIGN KEY (to_employee)   REFERENCES employees(id),
                FOREIGN KEY (done_by)       REFERENCES users(id)
            )
        """);
        System.out.println("✔ asset_history table ready.");

        // ─────────────────────────────────────────────
        // 9. CONSUMABLE STOCK
        //    Quantity-tracked items — not individual units
        //    Toner cartridges, Ink cartridges, Drums,
        //    Fusers, Patch cables (Cat5e/Cat6/Fiber),
        //    Power cables, HDMI/VGA/DP/USB cables,
        //    Thermal paste, Cleaning kits, Label tapes,
        //    Paper reams, Cable ties, Wall plates,
        //    Keystone jacks, Blank USBs, DVDs,
        //    Screws/standoffs, Any other consumable
        // ─────────────────────────────────────────────
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS consumable_stock (
                id                INTEGER PRIMARY KEY AUTOINCREMENT,
                name              TEXT NOT NULL,
                category          TEXT,
                compatible_models TEXT,
                quantity_in_stock INTEGER DEFAULT 0,
                reorder_level     INTEGER DEFAULT 0,
                unit              TEXT DEFAULT 'pieces',
                vendor_id         INTEGER,
                unit_cost         REAL,
                storage_location  TEXT,
                notes             TEXT,
                last_updated      TEXT DEFAULT (datetime('now')),
                FOREIGN KEY (vendor_id) REFERENCES vendor_contacts(id)
            )
        """);
        System.out.println("✔ consumable_stock table ready.");

        // ─────────────────────────────────────────────
        // 10. CONSUMABLE USAGE LOG
        //     Every time a consumable is issued/used
        //     e.g. toner installed into printer,
        //     cable given to an employee, etc.
        // ─────────────────────────────────────────────
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS consumable_usage_log (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                consumable_id  INTEGER NOT NULL,
                asset_id       INTEGER,
                employee_id    INTEGER,
                quantity_used  INTEGER DEFAULT 1,
                used_by        INTEGER,
                usage_date     TEXT DEFAULT (datetime('now')),
                notes          TEXT,
                FOREIGN KEY (consumable_id) REFERENCES consumable_stock(id),
                FOREIGN KEY (asset_id)      REFERENCES assets(id),
                FOREIGN KEY (employee_id)   REFERENCES employees(id),
                FOREIGN KEY (used_by)       REFERENCES users(id)
            )
        """);
        System.out.println("✔ consumable_usage_log table ready.");

        // ─────────────────────────────────────────────
        // 11. MAINTENANCE LOG
        //     Repairs, AMC visits, preventive service,
        //     upgrades, cleaning — for any asset type
        // ─────────────────────────────────────────────
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS maintenance_log (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                asset_id         INTEGER NOT NULL,
                maintenance_type TEXT,
                description      TEXT,
                done_by_internal INTEGER,
                done_by_vendor   INTEGER,
                cost             REAL,
                maintenance_date TEXT,
                next_due_date    TEXT,
                status           TEXT DEFAULT 'Completed',
                notes            TEXT,
                logged_by        INTEGER,
                created_at       TEXT DEFAULT (datetime('now')),
                FOREIGN KEY (asset_id)         REFERENCES assets(id),
                FOREIGN KEY (done_by_internal) REFERENCES users(id),
                FOREIGN KEY (done_by_vendor)   REFERENCES vendor_contacts(id),
                FOREIGN KEY (logged_by)        REFERENCES users(id)
            )
        """);
        System.out.println("✔ maintenance_log table ready.");

        // ─────────────────────────────────────────────
        // 12. TICKETS
        // ─────────────────────────────────────────────
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
        System.out.println("✔ tickets table ready.");

        // ─────────────────────────────────────────────
        // 13. TICKET COMMENTS
        // ─────────────────────────────────────────────
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS ticket_comments (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                ticket_id  INTEGER,
                comment    TEXT,
                added_by   INTEGER,
                added_at   TEXT,
                FOREIGN KEY (ticket_id) REFERENCES tickets(id),
                FOREIGN KEY (added_by)  REFERENCES users(id)
            )
        """);
        System.out.println("✔ ticket_comments table ready.");

        // ─────────────────────────────────────────────
        // 14. LICENSES  (software license tracking)
        // ─────────────────────────────────────────────
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
        System.out.println("✔ licenses table ready.");

        // ─────────────────────────────────────────────
        // 15. ACTIVITY LOG  (audit trail)
        // ─────────────────────────────────────────────
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS activity_log (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id    INTEGER,
                action     TEXT,
                table_name TEXT,
                record_id  INTEGER,
                details    TEXT,
                logged_at  TEXT,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        """);
        System.out.println("✔ activity_log table ready.");

        // ─────────────────────────────────────────────
        // DEFAULT ADMIN USER
        // username: admin  |  password: admin123
        // SHA-256: 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
        // ─────────────────────────────────────────────
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
        System.out.println("✔ Default admin user ready.");

        System.out.println("=== All 15 tables ready. VaultDesk database initialized. ===");
    }
}