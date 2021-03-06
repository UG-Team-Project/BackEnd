package pl.ug.virtualofficebackend.api.security;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pl.ug.virtualofficebackend.domain.security.boundary.LoginDto;
import pl.ug.virtualofficebackend.domain.security.boundary.RoleService;
import pl.ug.virtualofficebackend.domain.security.boundary.UserSecurityService;
import pl.ug.virtualofficebackend.domain.security.entity.Role;
import pl.ug.virtualofficebackend.domain.security.internal.config.JwtTokenProvider;
import pl.ug.virtualofficebackend.domain.security.internal.exception.UserDtoValidationException;
import pl.ug.virtualofficebackend.domain.security.internal.exception.WrongTokenException;
import pl.ug.virtualofficebackend.domain.security.internal.service.AuthValidationErrorService;
import pl.ug.virtualofficebackend.domain.user.boundary.UserDto;
import pl.ug.virtualofficebackend.domain.user.entity.User;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * trzeba dodać dwie role do tabeli Role: jedna z id=1, name=USER, i id=2, name=ADMIN
 * <p>
 * bez tokena wejdziecie tylko na dwie strony: /registration oraz /login
 * <p>
 * /registration przyjmuje dto z wartościami:
 * {
 * "name":"name3",
 * "surname": "surname3",
 * "username": "username3",
 * "password": "123",
 * "matchingPassword": "123",
 * "email": "asd3@gmail.com",
 * "country":"country3"
 * "office": { "id": "1" }
 * }
 * <p>
 * jeśli uda się zarejestrować, zwraca token w formacie: Bearer WŁAŚCIWY_TOKEN
 * <p>
 * /login przyjmuje takiego dtoka:
 * {
 * "username":"username3",
 * "password":"123"
 * }
 * zwraca to samo, co register
 * <p>
 * i teraz trzeba wsadzać ten token do headerów przy każdym zapytaniu API w formacie:
 * Authorization: Bearer WŁAŚCIWY_TOKEN
 * <p>
 * są zrobione dwa specjalne endpointy do testowania tego:
 * /user - zwraca Hello user, jeśli osoba jest zalogowana(w headerze był token)
 * /admin - zwraca Hello admin, jeśli osoba jest zalogowana I jej rola to ADMIN(roleId = 2)
 */

@RestController
public class AuthController {
    private UserSecurityService userSecurityService;
    private JwtTokenProvider tokenProvider;
    private AuthValidationErrorService authValidationErrorService;
    private AuthenticationManager authenticationManager;
    private RoleService roleService;

    @Autowired
    public AuthController(
            UserSecurityService userSecurityService,
            JwtTokenProvider tokenProvider,
            AuthValidationErrorService authValidationErrorService,
            AuthenticationManager authenticationManager,
            RoleService roleService) {
        this.userSecurityService = userSecurityService;
        this.tokenProvider = tokenProvider;
        this.authValidationErrorService = authValidationErrorService;
        this.authenticationManager = authenticationManager;
        this.roleService = roleService;
    }

    @CrossOrigin
    @PostMapping("/login")
    @ApiOperation(value = "Authenticate user with login and password. Returns Bearer Token if correct, otherwise print exception message.")
    public String authenticateUser(@Valid @RequestBody LoginDto loginDto, BindingResult result) {
        authValidationErrorService.validateFromBindingResult(result);

        Authentication authentication = createAuthentication(loginDto);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return createJwtToken(authentication);
    }

    private Authentication createAuthentication(@RequestBody @Valid LoginDto loginDto) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getUsername(),
                        loginDto.getPassword()
                )
        );
    }

    private String createJwtToken(Authentication authentication) {
        return "Bearer " + tokenProvider.generateToken(authentication);
    }

    /*
        This method always creates admin account..
     */

    @CrossOrigin
    @PostMapping("/registration")
    @ApiOperation(value = "Creates user account. Returns Bearer Token if everything was correct, otherwise print exception message.")
    public String registerUserAccount(@RequestBody UserDto accountDto) throws UserDtoValidationException {
        if(this.roleService.getAll().size() == 0) {
            System.out.println("Add roles");

            this.roleService.save(new Role("ADMIN"));
            this.roleService.save(new Role("MANAGER"));
            this.roleService.save(new Role("USER"));
        }

        System.out.println("Registering user account with information: " + accountDto);

        userSecurityService.registerNewUserAccount(accountDto);

        Authentication authentication = createAuthentication(new LoginDto(accountDto.getUsername(), accountDto.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return createJwtToken(authentication);
    }

    @GetMapping("/registrationConfirm")
    public void confirmRegistration(@RequestParam("token") final String token) throws WrongTokenException {
        userSecurityService.validateVerificationToken(token);

        final User user = userSecurityService.getUser(token);

        authWithoutPassword(user);
    }

    private void authWithoutPassword(User user) {
        List<String> role = new ArrayList<>();

        role.add(user.getRole().getName());

        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, getAuthorities(role));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private List<GrantedAuthority> getAuthorities(List<String> roles) {
        return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    //TEST ENDPOINT
    @GetMapping("/admin")
    public String helloAdmin() {
        return "Hello admin";
    }

    //TEST ENDPOINT
    @GetMapping("/user")
    public String helloUser() {
        return "Hello user";
    }

}
