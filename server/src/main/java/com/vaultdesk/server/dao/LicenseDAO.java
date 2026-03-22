package com.vaultdesk.server.dao;

import com.vaultdesk.server.model.License;
import com.vaultdesk.server.model.Ticket;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class LicenseDAO {
    public final JdbcTemplate jdbc;

    public LicenseDAO(JdbcTemplate jdbc)
    {
        this.jdbc=jdbc;
    }

    public List<License> getAllLicenses()
    {
        try {
            List<Map<String,Object>> rows = jdbc.queryForList(
                    "SELECT * FROM licenses");

            List<License> licenses = new ArrayList<>();

            for (Map<String,Object> row : rows) {
                licenses.add(mapRowToLicense(row));
            }
            return licenses;
        }
        catch (EmptyResultDataAccessException e)
        {
            return null;
        }
    }

    public List<License> getExpiringLicenses(int days)
    {

    }



    private License mapRowToLicense(Map<String,Object> row) {
        return new License(
                ((Number) row.get("id")).intValue(),
                (String) row.get("software_name"),
                (String) row.get("license_type"),
                (String) row.get("license_key"),
                row.get("seats_total") != null ? ((Number) row.get("seats_total")).intValue() : 0,
                row.get("seats_used") != null ? ((Number) row.get("seats_used")).intValue() : 0,
                (String) row.get("vendor"),
                (String) row.get("purchase_date"),
                (String) row.get("expiry_date"),
                row.get("cost") != null ? ((Number) row.get("cost")).doubleValue() : 0.0,
                (String) row.get("notes")
        );
    }
}
