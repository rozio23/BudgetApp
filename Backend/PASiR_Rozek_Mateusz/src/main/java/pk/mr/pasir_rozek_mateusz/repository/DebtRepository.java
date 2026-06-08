package pk.mr.pasir_rozek_mateusz.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pk.mr.pasir_rozek_mateusz.model.Debt;
import java.util.List;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {

    List<Debt> findByGroupId(Long groupId);

    void deleteByGroupId(Long groupId);
}
