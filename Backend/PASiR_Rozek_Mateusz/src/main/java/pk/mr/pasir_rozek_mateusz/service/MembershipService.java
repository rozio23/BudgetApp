package pk.mr.pasir_rozek_mateusz.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import pk.mr.pasir_rozek_mateusz.dto.MembershipDTO;
import pk.mr.pasir_rozek_mateusz.model.Group;
import pk.mr.pasir_rozek_mateusz.model.Membership;
import pk.mr.pasir_rozek_mateusz.model.User;
import pk.mr.pasir_rozek_mateusz.repository.GroupRepository;
import pk.mr.pasir_rozek_mateusz.repository.MembershipRepository;
import pk.mr.pasir_rozek_mateusz.repository.UserRepository;

import java.nio.file.AccessDeniedException;
import java.util.List;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public MembershipService(
            MembershipRepository membershipRepository,
            GroupRepository groupRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService) {
        this.membershipRepository = membershipRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public List<Membership> getGroupMembers(Long groupId) {
        assertCurrentUserIsGroupMember(groupId);
        return membershipRepository.findByGroupId(groupId);
    }

    public Membership addMember(MembershipDTO membershipDTO) {
        assertCurrentUserIsGroupOwner(membershipDTO.getGroupId());

        User user = userRepository.findByEmail(membershipDTO.getUserEmail())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono użytkownika o emailu: " + membershipDTO.getUserEmail()));

        Group group = groupRepository.findById(membershipDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono grupy o ID: " + membershipDTO.getGroupId()));

        // Validation: check if user is already member of group
        boolean alreadyMember = membershipRepository.findByGroupId(group.getId()).stream()
                .anyMatch(membership -> membership.getUser().getId().equals(user.getId()));

        if (alreadyMember) {
            throw new IllegalStateException("Użytkownik jest już członkiem tej grupy.");
        }

        Membership membership = new Membership();
        membership.setUser(user);
        membership.setGroup(group);

        return membershipRepository.save(membership);
    }

    public void removeMember(Long membershipId) {
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Członkostwo nie istnieje"));

        User currentUser = currentUserService.getCurrentUser();
        User groupOwner = membership.getGroup().getOwner();

        if (!currentUser.getId().equals(groupOwner.getId())) {
            try {
                throw new AccessDeniedException("Tylko właściciel grupy może usuwać członków.");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        if (membership.getUser().getId().equals(groupOwner.getId())) {
            throw new IllegalStateException("Nie można usunąć właściciela z jego grupy.");
        }

        membershipRepository.delete(membership);
    }

    public void assertCurrentUserIsGroupMember(Long groupId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono grupy o ID: " + groupId));

        User currentUser = currentUserService.getCurrentUser();
        assertUserIsGroupMember(groupId, currentUser.getId());
    }

    public void assertCurrentUserIsGroupOwner(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono grupy o ID: " + groupId));

        User currentUser = currentUserService.getCurrentUser();
        if (!group.getOwner().getId().equals(currentUser.getId())) {
            try {
                throw new AccessDeniedException("Tylko właściciel grupy może wykonać tę operację.");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void assertUserIsGroupMember(Long groupId, Long userId) {
        if (!membershipRepository.existsByGroupIdAndUserId(groupId, userId)) {
            try {
                throw new AccessDeniedException("Użytkownik nie jest członkiem tej grupy.");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}