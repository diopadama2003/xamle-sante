package sn.uasz.xamle.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.AuthResponse;
import sn.uasz.xamle.dto.LoginRequest;
import sn.uasz.xamle.dto.RegisterRequest;
import sn.uasz.xamle.model.Role;
import sn.uasz.xamle.model.Utilisateur;
import sn.uasz.xamle.repository.UtilisateurRepository;
import sn.uasz.xamle.security.JwtService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UtilisateurRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {

        // Seuls PATIENT et PROFESSIONNEL peuvent s'inscrire eux-mêmes
        if (req.role() != Role.PATIENT && req.role() != Role.PROFESSIONNEL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce rôle ne peut pas être choisi à l'inscription");
        }
        if (repo.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé");
        }

        Utilisateur u = repo.save(Utilisateur.builder()
                .nom(req.nom())
                .prenom(req.prenom())
                .email(req.email())
                .motDePasse(encoder.encode(req.motDePasse()))
                .telephone(req.telephone())
                .role(req.role())
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(u));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        Utilisateur u = repo.findByEmail(req.email())
                .filter(Utilisateur::isActif)
                .filter(user -> encoder.matches(req.motDePasse(), user.getMotDePasse()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect"));
        return toResponse(u);
    }

    private AuthResponse toResponse(Utilisateur u) {
        return new AuthResponse(jwtService.generateToken(u),
                u.getEmail(), u.getNom(), u.getPrenom(), u.getRole());
    }
}