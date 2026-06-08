package pk.mr.pasir_rozek_mateusz.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pk.mr.pasir_rozek_mateusz.model.User;
import pk.mr.pasir_rozek_mateusz.repository.UserRepository;

import java.nio.file.AccessDeniedException;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            try {
                throw new AccessDeniedException("Użytkownik nie jest uwierzytelniany");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono użytkownika o emailu: " + email));
    }
}
