package pk.mr.pasir_rozek_mateusz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GroupResponseDTO {

    private Long id;
    private String name;
    private Long ownerId;
}
