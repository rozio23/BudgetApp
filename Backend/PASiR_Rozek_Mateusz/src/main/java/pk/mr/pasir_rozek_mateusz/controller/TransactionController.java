package pk.mr.pasir_rozek_mateusz.controller;

import jakarta.validation.Valid;
import pk.mr.pasir_rozek_mateusz.dto.TransactionDTO;
import pk.mr.pasir_rozek_mateusz.model.Transaction;
import pk.mr.pasir_rozek_mateusz.model.TransactionType;
import pk.mr.pasir_rozek_mateusz.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pk.mr.pasir_rozek_mateusz.service.TransactionService;

import java.util.List;

@RestController
@RequestMapping("api/transactions")
public class TransactionController {

    private TransactionService transactionService;

    public TransactionController(TransactionService transactionService){
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable long id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody TransactionDTO transactionDTO) {
        Transaction newTransaction = transactionService.createTransaction(transactionDTO);
        return ResponseEntity.ok(newTransaction);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> updateTransaction(@PathVariable Long id,
                                                         @Valid @RequestBody TransactionDTO transactionDTO){
        Transaction updatedTransaction = transactionService.updateTransaction(id, transactionDTO);
        return ResponseEntity.ok(updatedTransaction);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
