package com.vaultdesk.server.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class LicenseAssignmentDAO {
    private final JdbcTemplate jdbc;
    private final LicenseDAO licenseDAO;

    public LicenseAssignmentDAO(JdbcTemplate jdbc, LicenseDAO licenseDAO) {
        this.jdbc = jdbc;
        this.licenseDAO = licenseDAO;
    }

    public List<Map<String, Object>> getAssignments(int licenseId) {
        return jdbc.queryForList(
                "SELECT la.id as assignment_id, la.employee_id, la.assigned_date, la.notes, " +
                        "e.name as employee_name, e.emp_code " +
                        "FROM license_assignments la JOIN employees e ON e.id = la.employee_id " +
                        "WHERE la.license_id = ? ORDER BY la.assigned_date DESC",
                licenseId);
    }

    /** Returns 0 if already assigned or if seats are full; otherwise the new assignment's id. */
    public int assign(int licenseId, int employeeId, String notes, int seatsTotal) {
        Integer alreadyAssigned = jdbc.queryForObject(
                "SELECT COUNT(*) FROM license_assignments WHERE license_id = ? AND employee_id = ?",
                Integer.class, licenseId, employeeId);
        if (alreadyAssigned != null && alreadyAssigned > 0) return 0;

        Integer currentUsed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM license_assignments WHERE license_id = ?", Integer.class, licenseId);
        if (currentUsed != null && seatsTotal > 0 && currentUsed >= seatsTotal) return -1; // full

        jdbc.update(
                "INSERT INTO license_assignments (license_id, employee_id, notes) VALUES (?, ?, ?)",
                licenseId, employeeId, notes);
        licenseDAO.syncSeatsUsed(licenseId);

        Integer newId = jdbc.queryForObject("SELECT MAX(id) FROM license_assignments", Integer.class);
        return newId != null ? newId : 0;
    }

    public int unassign(int assignmentId, int licenseId) {
        int rows = jdbc.update("DELETE FROM license_assignments WHERE id = ?", assignmentId);
        licenseDAO.syncSeatsUsed(licenseId);
        return rows;
    }
}