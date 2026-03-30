package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.DepartmentDAO;
import com.vaultdesk.server.model.Department;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {
    private final DepartmentDAO departmentDAO;

    public DepartmentController(DepartmentDAO departmentDAO) {
        this.departmentDAO = departmentDAO;
    }

    @GetMapping
    public ResponseEntity<?> getalldepartment() {
        return ResponseEntity.ok(departmentDAO.getAllDepartments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getdepartmentbyid(@PathVariable int id)
    {
        Department department  =departmentDAO.getDepartmentById(id);
        if (department == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(department);
    }

    @PostMapping
    public ResponseEntity<?> savedeprtment(@RequestBody Department department)
    {
        departmentDAO.saveDepartment(department);
        return ResponseEntity.status(201).body("Department added");
    }

}
