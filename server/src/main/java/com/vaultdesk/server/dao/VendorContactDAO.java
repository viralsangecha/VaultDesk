package com.vaultdesk.server.dao;

import com.vaultdesk.server.model.VendorContact;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class VendorContactDAO
{
    private final JdbcTemplate jdbc;  // field

    public VendorContactDAO(JdbcTemplate jdbc) {  // constructor injection
        this.jdbc =jdbc ;
    }

    public List<VendorContact> getAllVendors()
    {
        try {
            List<Map<String,Object>> rows = jdbc.queryForList(
                    "SELECT * FROM vendor_contacts");

            List<VendorContact> vendorContacts = new ArrayList<>();

            for (Map<String,Object> row : rows) {
                vendorContacts.add(new VendorContact(
                        ((Number) row.get("id")).intValue(),
                        (String) row.get("name"),
                        (String) row.get("contact_person"),
                        (String) row.get("phone"),
                        (String) row.get("email"),
                        (String) row.get("category"),
                        (String) row.get("address"),
                        (String) row.get("notes")
                ));
            }
            return vendorContacts;
        }
        catch (EmptyResultDataAccessException e)
        {
            return null;
        }
    }

    public VendorContact getVendorById(int id)
    {
        try {
            Map<String,Object> row = jdbc.queryForMap(
                    "SELECT * FROM vendor_contacts where id=?",id);

            return new VendorContact(
                        ((Number) row.get("id")).intValue(),
                        (String) row.get("name"),
                        (String) row.get("contact_person"),
                        (String) row.get("phone"),
                        (String) row.get("email"),
                        (String) row.get("category"),
                        (String) row.get("address"),
                        (String) row.get("notes")
            );
        }
        catch (EmptyResultDataAccessException e)
        {
            return null;
        }

    }

    public void saveVendor(VendorContact v)
    {
        jdbc.update(
                "INSERT INTO vendor_contacts (name, contact_person, phone, email, category, address, notes) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                v.name(), v.contactPerson(), v.phone(), v.email(), v.category(), v.address(), v.notes()
        );

    }
}
