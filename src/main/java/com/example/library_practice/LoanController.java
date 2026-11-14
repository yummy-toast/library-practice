package com.example.library_practice;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class LoanController {
    private final LoanService loans;

    public LoanController(LoanService loans) {
        this.loans = loans;
    }

    @PostMapping("/loans")
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowResponse borrow(@RequestBody BorrowRequest request) {
        if (request.bookID() == null || request.bookID().isBlank()
                || request.readerID() == null || request.readerID().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "bookID and readerID are required");
        }

        long loanID = loans.borrow(request.bookID(), request.readerID());
        return new BorrowResponse(loanID);
    }

    @PostMapping("/loans/{id}/return")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void returnLoan(@PathVariable("id") long loanID) {
        loans.returnLoan(loanID);
    }

    public record BorrowRequest(String bookID, String readerID) {}

    public record BorrowResponse(long loanID) {}
}