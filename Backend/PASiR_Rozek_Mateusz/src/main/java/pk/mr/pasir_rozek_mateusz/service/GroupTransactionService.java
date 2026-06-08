package pk.mr.pasir_rozek_mateusz.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import pk.mr.pasir_rozek_mateusz.config.NotificationWebSocketHandler;
import pk.mr.pasir_rozek_mateusz.dto.GroupExpenseNotification;
import pk.mr.pasir_rozek_mateusz.dto.GroupTransactionDTO;
import pk.mr.pasir_rozek_mateusz.model.*;
import pk.mr.pasir_rozek_mateusz.repository.DebtRepository;
import pk.mr.pasir_rozek_mateusz.repository.GroupRepository;
import pk.mr.pasir_rozek_mateusz.repository.MembershipRepository;
import pk.mr.pasir_rozek_mateusz.repository.TransactionRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static pk.mr.pasir_rozek_mateusz.model.TransactionType.EXPENSE;

import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
public class GroupTransactionService {

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final MembershipService membershipService;
    private final TransactionRepository transactionRepository;
    private final NotificationWebSocketHandler notificationHandler;

    public GroupTransactionService(
            GroupRepository groupRepository,
            MembershipRepository membershipRepository,
            DebtRepository debtRepository,
            MembershipService membershipService,
            TransactionRepository transactionRepository,
            NotificationWebSocketHandler notificationHandler) {
        this.groupRepository = groupRepository;
        this.membershipRepository =membershipRepository;
        this.debtRepository = debtRepository;
        this.membershipService = membershipService;
        this.transactionRepository = transactionRepository;
        this.notificationHandler = notificationHandler;
    }

    public void addGroupTransaction(GroupTransactionDTO transactionDTO, User currentUser) {
        Group group = groupRepository.findById(transactionDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono Grupy"));

        membershipService.assertCurrentUserIsGroupMember(group.getId());

        List<Membership> members = membershipRepository.findByGroupId(group.getId());
        List<Membership> selectedMembers = selectParticipants(transactionDTO, members, currentUser);
        if (selectedMembers.isEmpty()) {
            throw new IllegalStateException("Grupa nie ma czlonkow, nie mozna dodac transakcji.");
        }
        double amountPerUser = transactionDTO.getAmount() / selectedMembers.size();
        boolean expense = "EXPENSE".equals(transactionDTO.getType());
        for (Membership member : selectedMembers){
            User otherUser = member.getUser();
            if (!otherUser.getId().equals(currentUser.getId())){
                Debt debt = new Debt();
                debt.setDebtor(expense ? otherUser : currentUser);
                debt.setCreditor(expense ? currentUser : otherUser);
                debt.setGroup(group);
                debt.setAmount(amountPerUser);
                debt.setTitle(transactionDTO.getTitle());
                debtRepository.save(debt);
                String messageText = String.format("%s dodał wydatek \"%s\" w grupie %s. Twoja część: %.2f zł.",
                        currentUser.getEmail(), transactionDTO.getTitle(), group.getName(), amountPerUser);

                // Tworzenie obiektu DTO
                GroupExpenseNotification notification = new GroupExpenseNotification(
                        group.getId(),
                        group.getName(),
                        transactionDTO.getTitle(),
                        transactionDTO.getAmount(),
                        amountPerUser,
                        currentUser.getEmail(),
                        messageText
                );

                // Wysyłanie do konkretnego użytkownika po jego unikalnym identyfikatorze (zazwyczaj adres e-mail)
                notificationHandler.sendNotification(otherUser.getEmail(), notification);
            }
        }
        Transaction expenseTransaction = new Transaction();
        expenseTransaction.setUser(currentUser); // Płaci zalogowany użytkownik
        expenseTransaction.setAmount(transactionDTO.getAmount()); // Cała kwota (np. 120 zł)
        expenseTransaction.setType(EXPENSE); // Typ to wydatek
        expenseTransaction.setTimestamp(LocalDateTime.now()); // Lub odpowiedni format daty w Twoim projekcie
        expenseTransaction.setNotes("Wydatek grupowy: " + transactionDTO.getTitle());

        transactionRepository.save(expenseTransaction);
    }

    private List<Membership> selectParticipants(
            GroupTransactionDTO transactionDTO,
            List<Membership> members,
            User currentUser) {
        List<Long> selectedUserIds = transactionDTO.getSelectedUserIds();
        if (selectedUserIds == null || selectedUserIds.isEmpty()) {
            return members;
        }
        Set<Long> uniqueSelectedUserIds = new HashSet<>(selectedUserIds);
        List<Membership> selectedMembers = members.stream()
                .filter(membership -> uniqueSelectedUserIds.contains(membership.getUser().getId()))
                .toList();
        if (selectedMembers.size() != uniqueSelectedUserIds.size()) {
            throw new IllegalStateException(
                    "Wszyscy wybrani uzytkownicy musza byc czlonkami grupy.");
        }
        boolean currentUserSelected = selectedMembers.stream()
                .anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));
        if (!currentUserSelected) {
            throw new IllegalStateException(
                    "Aktualny uzytkownik musi byc uczestnikiem transakcji grupowej.");
        }
        if (selectedMembers.size() < 2) {
            throw new IllegalStateException("Transakcja grupowa wymaga co najmniej dwoch uczestnikow.");
        }
        return selectedMembers;
    }
}
