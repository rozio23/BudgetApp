package pk.mr.pasir_rozek_mateusz.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pk.mr.pasir_rozek_mateusz.dto.GroupDTO;
import pk.mr.pasir_rozek_mateusz.model.Group;
import pk.mr.pasir_rozek_mateusz.model.Membership;
import pk.mr.pasir_rozek_mateusz.model.User;
import pk.mr.pasir_rozek_mateusz.repository.DebtRepository;
import pk.mr.pasir_rozek_mateusz.repository.GroupRepository;
import pk.mr.pasir_rozek_mateusz.repository.MembershipRepository;

import java.nio.file.AccessDeniedException;
import java.util.List;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final CurrentUserService currentUserService;

    public GroupService(
            GroupRepository groupRepository,
            MembershipRepository membershipRepository,
            DebtRepository debtRepository,
            CurrentUserService currentUserService) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.debtRepository = debtRepository;
        this.currentUserService = currentUserService;
    }

    public List<Group> getAllGroups() {
        User currentUser = currentUserService.getCurrentUser(); // [cite: 411]
        return groupRepository.findByMemberships_User(currentUser); // [cite: 411]
    }

    public Group createGroup(GroupDTO groupDTO) {
        User owner = currentUserService.getCurrentUser();
        Group group = new Group();
        group.setName(groupDTO.getName());
        group.setOwner(owner);
        Group savedGroup = groupRepository.save(group);
        Membership membership = new Membership();
        membership.setUser(owner);
        membership.setGroup(savedGroup);
        membershipRepository.save(membership);
        return savedGroup;
    }

    @Transactional
    public  void deleteGroup(Long id){
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można usunąć grupy. Grupa o ID " + id + " nie istnieje."));

        User currentUser = currentUserService.getCurrentUser();
        if (!group.getOwner().getId().equals(currentUser.getId())) {
            try {
                throw new AccessDeniedException("Tylko właściciel grupy może ją usunąć.");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        debtRepository.deleteByGroupId(id);
        membershipRepository.deleteByGroupId(id);
        groupRepository.delete(group);
    }
}
