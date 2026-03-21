package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.EmployeeDAO;
import com.vaultdesk.server.model.Employee;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeDAO employeeDAO;  // field

    public EmployeeController(EmployeeDAO employeeDAO) {  // constructor injection
        this.employeeDAO= employeeDAO;
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

    // PUT update
    @PutMapping("/{id}")
    public ResponseEntity<?> updateempbyid(@PathVariable int id,
                                    @RequestBody Employee emp) {
        int rows = employeeDAO.updateEmployee(emp);
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("Employee updated");
    }

    // DELETE deactivate
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteempbyid(@PathVariable int id) {
        int rows = employeeDAO.deactivateEmployee(id);
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("Employee deactivated");
    }




}
