package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.EmployeeDAO;
import com.vaultdesk.server.model.Employee;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeDAO employeeDAO;
    private final JdbcTemplate jdbc;

    public EmployeeController(EmployeeDAO employeeDAO,
                              JdbcTemplate jdbc) {
        this.employeeDAO = employeeDAO;
        this.jdbc        = jdbc;
    }

    @GetMapping
    public ResponseEntity<?> getallemployes() {
        return ResponseEntity.ok(employeeDAO.getAllEmployees());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getempbyid(@PathVariable int id) {
        Employee e = employeeDAO.getEmployeeById(id);
        if (e == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(e);
    }

    @PostMapping
    public ResponseEntity<?> saveemp(@RequestBody Employee emp) {
        employeeDAO.saveEmployee(emp);
        return ResponseEntity.status(201).body("Employee added");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateempbyid(
            @PathVariable int id,
            @RequestBody Employee emp) {
        int rows = employeeDAO.updateEmployee(emp);
        if (rows == 0) return ResponseEntity.notFound().build();
        logActivity(0, "UPDATE", "employees", id,
                "Employee updated: " + emp.name());
        return ResponseEntity.ok("Employee updated");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteempbyid(@PathVariable int id) {
        int rows = employeeDAO.deactivateEmployee(id);
        if (rows == 0) return ResponseEntity.notFound().build();
        logActivity(0, "DELETE", "employees", id,
                "Employee deactivated #" + id);
        return ResponseEntity.ok("Employee deactivated");
    }

    @GetMapping("/department/{deptId}")
    public ResponseEntity<?> getByDept(@PathVariable int deptId) {
        return ResponseEntity.ok(
                employeeDAO.getEmployeesByDept(deptId));
    }

    private void logActivity(int userId, String action,
                             String tableName, int recordId,
                             String details) {
        try {
            jdbc.update(
                    "INSERT INTO activity_log " +
                            "(user_id, action, table_name, " +
                            "record_id, details, logged_at) " +
                            "VALUES (?, ?, ?, ?, ?, datetime('now'))",
                    userId, action, tableName, recordId, details);
        } catch (Exception e) {
            System.out.println("Activity log error: "
                    + e.getMessage());
        }
    }
    @GetMapping("/inactive")
    public ResponseEntity<?> getInactiveEmployees() {
        return ResponseEntity.ok(
                employeeDAO.getInactiveEmployees());
    }

    @PutMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable int id) {
        int rows = employeeDAO.reactivateEmployee(id);
        if (rows == 0) return ResponseEntity.notFound().build();
        logActivity(0, "UPDATE", "employees", id,
                "Employee reactivated #" + id);
        return ResponseEntity.ok("Employee reactivated");
    }
}