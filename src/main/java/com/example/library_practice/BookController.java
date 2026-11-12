package com.example.library_practice;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class BookController {
    private final JdbcTemplate jdbc;

    public BookController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/books/{id}")
    public Book getBook(@PathVariable("id") String id) {
        return jdbc.query("""
                SELECT bookID, bookName, author, stockNum
                FROM book
                WHERE bookID = ?
                """,
                (row, index) -> new Book(
                        row.getString("bookID"),
                        row.getString("bookName"),
                        row.getString("author"),
                        row.getInt("stockNum")),
                id)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    }

    public record Book(String bookID, String bookName, String author, int stockNum) {}
}
