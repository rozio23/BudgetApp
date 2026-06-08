package pk.mr.pasir_rozek_mateusz.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import pk.mr.pasir_rozek_mateusz.config.NotificationWebSocketHandler;
import pk.mr.pasir_rozek_mateusz.dto.DebtDTO;
import pk.mr.pasir_rozek_mateusz.dto.GroupExpenseNotification; // PAMIĘTAJ O TYM IMPORCIE!
import pk.mr.pasir_rozek_mateusz.model.*;
import pk.mr.pasir_rozek_mateusz.repository.DebtRepository;
import pk.mr.pasir_rozek_mateusz.repository.GroupRepository;
import pk.mr.pasir_rozek_mateusz.repository.TransactionRepository;
import pk.mr.pasir_rozek_mateusz.repository.UserRepository;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DebtService {

    private final DebtRepository debtRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final MembershipService membershipService;
    private final CurrentUserService currentUserService;
    private final TransactionRepository transactionRepository;
    private final NotificationWebSocketHandler notificationHandler;

    public DebtService(
            DebtRepository debtRepository,
            GroupRepository groupRepository,
            UserRepository userRepository,
            MembershipService membershipService,
            CurrentUserService currentUserService,
            TransactionRepository transactionRepository,
            NotificationWebSocketHandler notificationHandler) {
        this.debtRepository = debtRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.membershipService = membershipService;
        this.currentUserService = currentUserService;
        this.transactionRepository = transactionRepository;
        this.notificationHandler = notificationHandler;
    }

    public List<Debt> getGroupDebts(Long groupId) {
        membershipService.assertCurrentUserIsGroupMember(groupId);
        return debtRepository.findByGroupId(groupId);
    }

    public Debt createDebt(DebtDTO debtDTO) {
        Group group = groupRepository.findById(debtDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można utworzyć długu. Grupa o ID " + debtDTO.getGroupId() + " nie istnieje."));

        User debtor = userRepository.findById(debtDTO.getDebtorId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można utworzyć długu. Dłużnik o ID " + debtDTO.getDebtorId() + " nie istnieje."));

        User creditor = userRepository.findById(debtDTO.getCreditorId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można utworzyć długu. Wierzyciel o ID " + debtDTO.getCreditorId() + " nie istnieje."));

        membershipService.assertCurrentUserIsGroupMember(group.getId());
        membershipService.assertUserIsGroupMember(group.getId(), debtor.getId());
        membershipService.assertUserIsGroupMember(group.getId(), creditor.getId());

        if (debtor.getId().equals(creditor.getId())) {
            throw new IllegalStateException("Dłużnik i wierzyciel muszą być różnymi użytkownikami.");
        }

        User currentUser = currentUserService.getCurrentUser();
        assertCurrentUserCanManageDebt(group, debtor, creditor, currentUser);

        Debt debt = new Debt();
        debt.setGroup(group);
        debt.setDebtor(debtor);
        debt.setCreditor(creditor);
        debt.setAmount(debtDTO.getAmount());
        debt.setTitle(debtDTO.getTitle());

        return debtRepository.save(debt);
    }

    public void deleteDebt(Long debtId) {
        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można usunąć długu. Dług o ID " + debtId + " nie istnieje."));

        membershipService.assertCurrentUserIsGroupMember(debt.getGroup().getId());
        User currentUser = currentUserService.getCurrentUser();
        assertCurrentUserCanManageDebt(debt.getGroup(), debt.getDebtor(), debt.getCreditor(), currentUser);
        debtRepository.delete(debt);
    }

    private void assertCurrentUserCanManageDebt(Group group, User debtor, User creditor, User currentUser) {
        boolean isGroupOwner = group.getOwner().getId().equals(currentUser.getId());
        boolean isDebtParticipant = debtor.getId().equals(currentUser.getId())
                || creditor.getId().equals(currentUser.getId());

        if (!isGroupOwner && !isDebtParticipant) {
            try {
                throw new AccessDeniedException(
                        "Tylko właściciel grupy albo uczestnik długu może wykonać te operacje.");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Debt markDebtAsPaid(Long debtId) {
        Debt debt = getDebtForCurrentGroupMember(debtId);
        User currentUser = currentUserService.getCurrentUser();
        if (!debt.getDebtor().getId().equals(currentUser.getId())) {
            try {
                throw new AccessDeniedException("Tylko dluznik moze oznaczyc dlug jako oplacony.");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }
        debt.setPaidByDebtor(true);
        debt.setConfirmedByCreditor(false);
        Debt savedDebt = debtRepository.save(debt);

        // --- POWIADOMIENIE DLA WIERZYCIELA ---
        String message = String.format("%s oznaczył dług \"%s\" (%.2f zł) jako opłacony. Oczekuje na potwierdzenie.",
                currentUser.getEmail(), debt.getTitle(), debt.getAmount());

        GroupExpenseNotification notification = new GroupExpenseNotification(
                "DEBT_PAID",
                debt.getGroup().getId(),
                debt.getGroup().getName(),
                debt.getTitle(),
                debt.getAmount(),
                debt.getAmount(),
                currentUser.getEmail(),
                message
        );

        notificationHandler.sendNotification(debt.getCreditor().getEmail(), notification);
        // -------------------------------------

        return savedDebt;
    }

    public Debt confirmDebtPayment(Long debtId) {
        Debt debt = getDebtForCurrentGroupMember(debtId);
        User currentUser = currentUserService.getCurrentUser();
        if (!debt.getCreditor().getId().equals(currentUser.getId())) {
            try {
                throw new AccessDeniedException("Tylko wierzyciel moze potwierdzic splate dlugu.");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }
        if (!debt.isPaidByDebtor()) {
            throw new IllegalStateException(
                    "Dlug musi zostac najpierw oznaczony jako oplacony przez dluznika.");
        }
        debt.setConfirmedByCreditor(true);
        Debt savedDebt = debtRepository.save(debt);

        Transaction creditorIncome = new Transaction();
        creditorIncome.setUser(debt.getCreditor());
        creditorIncome.setAmount(debt.getAmount());
        creditorIncome.setType(pk.mr.pasir_rozek_mateusz.model.TransactionType.INCOME);
        creditorIncome.setTimestamp(java.time.LocalDateTime.now());
        creditorIncome.setNotes("Zwrot długu w grupie od: " + debt.getDebtor().getEmail());
        transactionRepository.save(creditorIncome);

        Transaction debtorExpense = new Transaction();
        debtorExpense.setUser(debt.getDebtor());
        debtorExpense.setAmount(debt.getAmount());
        debtorExpense.setType(TransactionType.EXPENSE);
        debtorExpense.setTimestamp(LocalDateTime.now());
        debtorExpense.setNotes("Spłata długu w grupie dla: " + debt.getCreditor().getEmail());
        transactionRepository.save(debtorExpense);

        // --- POWIADOMIENIE DLA DŁUŻNIKA ---
        String message = String.format("%s potwierdził spłatę Twojego długu \"%s\" (%.2f zł).",
                currentUser.getEmail(), debt.getTitle(), debt.getAmount());

        GroupExpenseNotification notification = new GroupExpenseNotification(
                "DEBT_CONFIRMED",
                debt.getGroup().getId(),
                debt.getGroup().getName(),
                debt.getTitle(),
                debt.getAmount(),
                debt.getAmount(),
                currentUser.getEmail(),
                message
        );

        notificationHandler.sendNotification(debt.getDebtor().getEmail(), notification);
        // ----------------------------------

        return savedDebt;
    }

    private Debt getDebtForCurrentGroupMember(Long debtId) {
        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono dlugu o ID " + debtId + "."));
        membershipService.assertCurrentUserIsGroupMember(debt.getGroup().getId());
        return debt;
    }
}