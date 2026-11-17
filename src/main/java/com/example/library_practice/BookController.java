package com.example.library_practice;

import java.time.Duration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class BookController {
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public BookController(JdbcTemplate jdbc, StringRedisTemplate redis) {
        this.jdbc = jdbc;
        this.redis = redis;
    }

    @GetMapping("/books/{id}")
public Book getBook(@PathVariable("id") String id) {
    String key = "library:book:details:" + id;
    var details = redis.<String, String>opsForHash();

    try {
        var cached = details.entries(key);
        if (cached.containsKey("bookName") && cached.containsKey("author")) {
            int stockNum = jdbc.query(
                    "SELECT stockNum FROM book WHERE bookID = ?",
                    (row, index) -> row.getInt("stockNum"),
                    id)
                    .stream()
                    .findFirst()
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
            return new Book(id, cached.get("bookName"), cached.get("author"), stockNum);
        }
    } catch (org.springframework.data.redis.RedisConnectionFailureException ex) {
        // Redis 连接失败时，下面会从 MySQL 查询完整数据。
    }

    Book book = loadFromMySql(id);
    try {
        details.putAll(key, java.util.Map.of(
                "bookName", book.bookName(),
                "author", book.author()));
        redis.expire(key, Duration.ofMinutes(5));
    } catch (DataAccessException ex) {
        // 写缓存失败时，仍然返回已经查到的图书。
    }
    return book;
}

private Book loadFromMySql(String id) {
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
