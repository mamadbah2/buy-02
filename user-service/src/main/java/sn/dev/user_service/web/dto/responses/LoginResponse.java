package sn.dev.user_service.web.dto.responses;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import sn.dev.user_service.data.entities.UserPrincipal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class LoginResponse {
    private final String id;
    private final String email;
    private final String token;
    private List<String> role = new ArrayList<>();

    public LoginResponse(UserDetails userDetails, String token) {
        // Extraire l'ID si c'est un UserPrincipal
        if (userDetails instanceof UserPrincipal userPrincipal) {
            this.id = userPrincipal.getId();
        } else {
            this.id = null; // Ou une valeur par défaut
        }

        email = userDetails.getUsername();
        this.token = token;
        var formattedRole = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));
        role.add(formattedRole);
    }
}
