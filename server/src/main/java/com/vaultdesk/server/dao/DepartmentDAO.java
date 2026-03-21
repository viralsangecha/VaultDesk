package com.vaultdesk.server.dao;

import com.vaultdesk.server.model.Department;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DepartmentDAO {
    private final JdbcTemplate jdbc;  // field

    public DepartmentDAO(JdbcTemplate jdbc) {  // constructor injection
        this.jdbc =jdbc ;
    }

    public List<Department> getAllDepartments()
    {
        try {
            List<Map<String,Object>> rows = jdbc.queryForList(
                    "SELECT * FROM departments");

            List<Department> departments = new ArrayList<>();

            for (Map<String,Object> row : rows) {
                departments.add(new Department(
                        ((Number) row.get("id")).intValue(),
                        (String) row.get("name"),
                        (String) row.get("location")
                ));
            }
            return departments;
        }
        catch (EmptyResultDataAccessException e)
        {
            return null;
        }
    }

    public Department getDepartmentById(int id)
    {
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT * FROM departments where id=?",id);
            return new Department(
                    ((Number) row.get("id")).intValue(),
                    (String) row.get("name"),
                    (String) row.get("location")
            );
        }
        catch (EmptyResultDataAccessException e)
        {
            return null;
        }
    }

    public void saveDepartment(Department dept)
    {
        jdbc.update("INSERT INTO departments (name, location) VALUES (?, ?)",dept.name(),dept.location());
    }
}
