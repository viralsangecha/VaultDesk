package com.vaultdesk.server;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;



@Component
public class TicketDAO {
    public final JdbcTemplate jdbc;

    public TicketDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }


    public List<Map<String,Object>> getAllTickets()
    {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM tickets");

        return rows;
    }

    public ResponseEntity<?>  getTicketById(int id)
    {
        try {
            Map<String,Object> rows = jdbc.queryForMap("SELECT * FROM tickets where id=?",id);
            return ResponseEntity.ok(rows);
        }
        catch (EmptyResultDataAccessException ex)
        {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<?> updateTicketStatus(int id, String status)
    {
        int rows=jdbc.update("UPDATE tickets SET status = ? WHERE id = ?", status, id);
        if (rows==0)
        {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("Ticket updated succssesfull");
    }

    public ResponseEntity<?> deleteTicket(int id)
    {
        int rows=jdbc.update("delete from   tickets where id=?",id);
        if (rows==0)
        {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("Ticket deleted succssesfull");
    }
}
