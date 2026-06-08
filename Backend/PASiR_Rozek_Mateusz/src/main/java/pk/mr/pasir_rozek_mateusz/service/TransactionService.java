package pk.mr.pasir_rozek_mateusz.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.expression.AccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pk.mr.pasir_rozek_mateusz.dto.BalanceDTO;
import pk.mr.pasir_rozek_mateusz.dto.TransactionDTO;
import pk.mr.pasir_rozek_mateusz.model.Transaction;
import pk.mr.pasir_rozek_mateusz.model.TransactionType;
import pk.mr.pasir_rozek_mateusz.model.User;
import pk.mr.pasir_rozek_mateusz.repository.TransactionRepository;
import pk.mr.pasir_rozek_mateusz.repository.UserRepository;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            try {
                throw new AccessDeniedException("Użytkownik nie jest uwierzytelniony");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono zalogowanego użytkownika: " + email));
    }

    public List<Transaction> getAllTransactions() {
        User user = getCurrentUser();
        return transactionRepository.findAllByUser(user);
    }

    public Transaction getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID " + id));
        if (!transaction.getUser().getEmail().equals(getCurrentUser().getEmail())) {
            try {
                throw new AccessDeniedException("Nie masz dostępu do tej transakcji");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        return transaction;
    }

    public Transaction updateTransaction(Long id, TransactionDTO transactionDTO) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID " + id));

        if (!transaction.getUser().getEmail().equals(getCurrentUser().getEmail())) {
            try {
                throw new AccessDeniedException("Nie masz dostępu do tej transakcji");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        transaction.setAmount(transactionDTO.getAmount());
        transaction.setType(TransactionType.valueOf(transactionDTO.getType()));
        transaction.setTags(transactionDTO.getTags());
        transaction.setNotes(transactionDTO.getNotes());

        return transactionRepository.save(transaction);
    }

    public Transaction createTransaction(TransactionDTO transactionDTO) {
        Transaction transaction = new Transaction();
        transaction.setAmount(transactionDTO.getAmount());
        transaction.setType(TransactionType.valueOf(transactionDTO.getType()));
        transaction.setTags(transactionDTO.getTags());
        transaction.setNotes(transactionDTO.getNotes());
        transaction.setUser(getCurrentUser());
        transaction.setTimestamp(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie można usunąć. Nie znaleziono transakcji o ID " + id));

        if (!transaction.getUser().getEmail().equals(getCurrentUser().getEmail())) {
            try {
                throw new AccessDeniedException("Nie masz uprawnień do usunięcia tej transakcji");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        transactionRepository.deleteById(id);
    }

    public BalanceDTO getUserBalance (User user, Double days) {
        List<Transaction> userTransactions;

        if (days != null && days > 0) {
            long secondsToSubtract = (long) (days * 24 * 60 * 60);
            LocalDateTime filterDate = LocalDateTime.now().minusSeconds(secondsToSubtract);

            userTransactions = transactionRepository.findAllByUserAndTimestampGreaterThanEqual(user, filterDate);
        } else {
            userTransactions = transactionRepository.findByUser(user);
        }

        double income = userTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double expense = userTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();

        return new BalanceDTO(income, expense, income - expense);
    }
}
