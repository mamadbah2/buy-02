package sn.dev.user_service.data.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import sn.dev.user_service.data.entities.Role;
import sn.dev.user_service.data.entities.User;
import sn.dev.user_service.data.repositories.UserRepositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class UserSeeding implements CommandLineRunner {

    private final UserRepositories userRepositories;
    private final PasswordEncoder passwordEncoder;
    private final Faker faker = new Faker(Locale.FRENCH);

    // Nombre d'utilisateurs à générer
    private static final int NUMBER_OF_CLIENTS = 500;
    private static final int NUMBER_OF_SELLERS = 200;

    @Override
    public void run(String... args) {
        if (userRepositories.count() == 0) {
            log.info("🚀 Début du seeding des utilisateurs...");
            seedUsers();
            log.info("✅ Seeding terminé avec succès!");
        } else {
            log.info("ℹ️ Les utilisateurs existent déjà, seeding ignoré.");
        }
    }

    private void seedUsers() {
        List<User> users = new ArrayList<>();

        // Générer des clients
        log.info("👥 Génération de {} clients...", NUMBER_OF_CLIENTS);
        for (int i = 0; i < NUMBER_OF_CLIENTS; i++) {
            users.add(createUser(Role.CLIENT, i));
        }

        // Générer des vendeurs
        log.info("🏪 Génération de {} vendeurs...", NUMBER_OF_SELLERS);
        for (int i = 0; i < NUMBER_OF_SELLERS; i++) {
            users.add(createUser(Role.SELLER, i));
        }

        // Sauvegarder tous les utilisateurs
        userRepositories.saveAll(users);
        log.info("✅ {} utilisateurs créés avec succès!", users.size());
        log.info("   - {} clients", NUMBER_OF_CLIENTS);
        log.info("   - {} vendeurs", NUMBER_OF_SELLERS);
    }

    private User createUser(Role role, int index) {
        User user = new User();
        log.info("creating user {} {}", role, index);
        // Générer un nom complet
        String firstName = faker.name().firstName();
        String lastName = faker.name().lastName();
        user.setName(firstName + " " + lastName);
        // Générer un email unique basé sur le nom et l'index
        String emailPrefix = firstName.toLowerCase()
                .replaceAll("[^a-z]", "")
                + "." + lastName.toLowerCase()
                .replaceAll("[^a-z]", "")
                + index;
        user.setEmail(emailPrefix + "@" + faker.internet().domainName());
        // Mot de passe par défaut (hashé)
        // Pour faciliter les tests: "password123"
        user.setPassword(passwordEncoder.encode("password123"));
        // Assigner le rôle
        user.setRole(role);
        // Générer une photo de profil avec Picsum
        // Utiliser un seed unique basé sur l'index pour avoir des photos différentes mais reproductibles
        int seed = (role == Role.CLIENT ? index : index + 1000);
        user.setAvatar(generateAvatarUrl(seed));
        return user;
    }

    private String generateAvatarUrl(int seed) {
        // Photos de profil carrées 400x400 avec un seed unique
        return String.format("https://picsum.photos/seed/user%d/400/400", seed);
    }
}
