package pk.mr.pasir_rozek_mateusz.repository;

import pk.mr.pasir_rozek_mateusz.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pk.mr.pasir_rozek_mateusz.model.User;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByUser(User user);
    List<Transaction> findByUser(User user);
    List<Transaction> findAllByUserAndTimestampGreaterThanEqual(User user, LocalDateTime timestamp);
}
