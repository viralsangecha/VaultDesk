package com.vaultdesk.server.dao;

import com.vaultdesk.server.model.Employee;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class EmployeeDAO {
    private final JdbcTemplate jdbc;  // field

    public EmployeeDAO(JdbcTemplate jdbc) {  // constructor injection
        this.jdbc =jdbc ;
    }

    public List<Employee> getAllEmployees()
    {
        try {
            List<Map<String,Object>> rows = jdbc.queryForList(
                    "SELECT * FROM employees");

            List<Employee> employees = new ArrayList<>();

            for (Map<String,Object> row : rows) {
                employees.add(new Employee(
                        ((Number) row.get("id")).intValue(),
                        (String) row.get("name"),
                        (String) row.get("emp_code"),
                        ((Number) row.get("department_id")).intValue(),
                        (String) row.get("designation"),
                        (String) row.get("email"),
                        (String) row.get("phone"),
                        (String) row.get("join_date"),
                        (String) row.get("leave_date"),
                        ((Number) row.get("active")).intValue(),
                        (String) row.get("notes")
                ));
            }
            return employees;
        }
        catch (EmptyResultDataAccessException e)
        {
            return null;
        }
    }

    public List<Employee> getEmployeesByDept(int deptId) {
        try {
            List<Map<String,Object>> rows = jdbc.queryForList(
                    "SELECT * FROM employees WHERE department_id = ? AND active = 1",
                    deptId);
            List<Employee> employees = new ArrayList<>();
            for (Map<String,Object> row : rows) {
                employees.add(new Employee(
                        ((Number) row.get("id")).intValue(),
                        (String) row.get("name"),
                        (String) row.get("emp_code"),
                        ((Number) row.get("department_id")).intValue(),
                        (String) row.get("designation"),
                        (String) row.get("email"),
                        (String) row.get("phone"),
                        (String) row.get("join_date"),
                        (String) row.get("leave_date"),
                        ((Number) row.get("active")).intValue(),
                        (String) row.get("notes")
                ));
            }
            return employees;
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Employee getEmployeeById(int id)
    {
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT * FROM employees where id=?",id);
            return new Employee(
                    ((Number) row.get("id")).intValue(),
                    (String) row.get("name"),
                    (String) row.get("emp_code"),
                    ((Number) row.get("department_id")).intValue(),
                    (String) row.get("designation"),
                    (String) row.get("email"),
                    (String) row.get("phone"),
                    (String) row.get("join_date"),
                    (String) row.get("leave_date"),
                    ((Number) row.get("active")).intValue(),
                    (String) row.get("notes")
            );
        }
        catch (EmptyResultDataAccessException e)
        {
            return null;
        }
    }

    public void saveEmployee(Employee emp)
    {
        jdbc.update(
                "INSERT INTO employees (name, emp_code, department_id, designation, " +
                        "email, phone, join_date, active, notes) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?)",
                emp.name(), emp.empCode(), emp.departmentId(), emp.designation(),
                emp.email(), emp.phone(), emp.joinDate(), emp.notes()
        );
    }

    public int deactivateEmployee(int id)
    {
        return jdbc.update("update employees set active=0 where id=?",id);
    }

    public int updateEmployee(Employee emp) {
        return jdbc.update(
                "UPDATE employees SET name=?, emp_code=?, department_id=?, " +
                        "designation=?, email=?, phone=?, notes=? WHERE id=?",
                emp.name(), emp.empCode(), emp.departmentId(),
                emp.designation(), emp.email(), emp.phone(),
                emp.notes(), emp.id()
        );
    }

}
