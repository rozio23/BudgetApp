package pk.mr.pasir_rozek_mateusz.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MembershipDTO {

    @NotBlank(message = "Email użytkownika nie może być pusty")
    @Email(message = "Email użytkownika musi być poprawnym adresem email")
    private String userEmail;

    @NotNull(message = "Id grupy nie może być puste")
    private Long groupId;
}
