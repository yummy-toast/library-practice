package com.example.library_practice;

import java.sql.Statement;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoanService {
    private final JdbcTemplate jdbc;

    public LoanService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public long borrow(String bookID, String readerID) {
        int readers = jdbc.queryForObject(
                "SELECT COUNT(*) FROM reader WHERE readerID = ?",
                Integer.class,
                readerID);
        if (readers == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reader not found");
        }

        int updated = jdbc.update(
                "UPDATE book SET stockNum = stockNum - 1 WHERE bookID = ? AND stockNum > 0",
                bookID);
        if (updated == 0) {
            int books = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM book WHERE bookID = ?",
                    Integer.class,
                    bookID);
            if (books == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No stock available");
        }

        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO loan (readerID, bookID) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, readerID);
            statement.setString(2, bookID);
            return statement;
        }, keys);

        Number loanID = keys.getKey();
        if (loanID == null) {
            throw new IllegalStateException("MySQL did not return a loan ID");
        }
        return loanID.longValue();
    }

    @Transactional
    public void returnLoan(long loanID) {
        int updated = jdbc.update(
                "UPDATE loan SET loanStatus = 'RETURNED' WHERE loanID = ? AND loanStatus = 'BORROWED'",
                loanID);
    
        if (updated == 0) {
            int count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM loan WHERE loanID = ?",
                    Integer.class,
                    loanID);
    
            if (count == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Loan already returned");
        }
    
        String bookID = jdbc.queryForObject(
                "SELECT bookID FROM loan WHERE loanID = ?",
                String.class,
                loanID);
    
        int restocked = jdbc.update(
                "UPDATE book SET stockNum = stockNum + 1 WHERE bookID = ?",
                bookID);
    
        if (restocked != 1) {
            throw new IllegalStateException("Could not restore book stock");
        }
    }
}