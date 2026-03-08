package com.ski.inventory.service;

import com.ski.inventory.model.Customer;
import com.ski.inventory.model.Order;
import com.ski.inventory.model.User;
import com.ski.inventory.model.UserRole;
import com.ski.inventory.repository.CustomerRepository;
import com.ski.inventory.repository.OrderRepository;
import com.ski.inventory.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PasswordService passwordService;
    @Mock
    private JwtService jwtService;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                userRepository, orderRepository, customerRepository,
                passwordService, jwtService);
    }

    @Test
    void authenticate_validCredentials_returnsAuthResponse() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPasswordHash("hash");
        user.setRole(UserRole.ADMIN);
        user.setFullName("Admin");
        user.setEmail("admin@test.cz");
        user.setActive(true);

        when(userRepository.findByUsernameAndActiveTrue("admin")).thenReturn(Optional.of(user));
        when(passwordService.verifyPassword("hash", "secret")).thenReturn(true);
        when(jwtService.generateToken("admin", "ADMIN")).thenReturn("jwt-token");

        var response = authenticationService.authenticate("admin", "secret");

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.role()).isEqualTo("ADMIN");
    }

    @Test
    void authenticate_invalidPassword_throwsBadCredentials() {
        User user = new User();
        user.setUsername("admin");
        user.setPasswordHash("hash");
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        when(userRepository.findByUsernameAndActiveTrue("admin")).thenReturn(Optional.of(user));
        when(passwordService.verifyPassword("hash", "wrong")).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.authenticate("admin", "wrong"))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }

    @Test
    void authenticate_userNotFound_throwsBadCredentials() {
        when(userRepository.findByUsernameAndActiveTrue("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.authenticate("unknown", "pass"))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }

    @Test
    void authenticateCustomer_validOrderAndPhone_returnsAuthResponse() {
        Customer customer = new Customer();
        customer.setId(10L);
        customer.setName("Jan Novák");
        customer.setEmail("jan@test.cz");
        customer.setPhone("123456789");
        customer.setCustomerNumber("C001");

        Order order = new Order();
        order.setOrderNumber("ORD-001");
        order.setCustomer(customer);

        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));
        when(jwtService.generateToken("10", "CUSTOMER")).thenReturn("customer-jwt");

        var response = authenticationService.authenticateCustomer("ORD-001", "123456789");

        assertThat(response.token()).isEqualTo("customer-jwt");
        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.role()).isEqualTo("CUSTOMER");
    }

    @Test
    void authenticateCustomer_phoneWithCountryCode_matches() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setPhone("420123456789");
        customer.setCustomerNumber("C1");
        Order order = new Order();
        order.setOrderNumber("O1");
        order.setCustomer(customer);

        when(orderRepository.findByOrderNumber("O1")).thenReturn(Optional.of(order));
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("token");

        var response = authenticationService.authenticateCustomer("O1", "123456789");
        assertThat(response).isNotNull();
    }

    @Test
    void authenticateCustomer_orderNotFound_throwsBadCredentials() {
        when(orderRepository.findByOrderNumber("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.authenticateCustomer("NONEXISTENT", "123"))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }

    @Test
    void register_newUser_savesAndReturnsUser() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordService.hashPassword("password")).thenReturn("hashed");
        User saved = new User();
        saved.setId(1L);
        saved.setUsername("newuser");
        saved.setPasswordHash("hashed");
        saved.setRole(UserRole.TECHNICIAN);
        saved.setActive(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        User result = authenticationService.register("newuser", "password", UserRole.TECHNICIAN, "Full Name", "a@b.cz");

        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getPasswordHash()).isEqualTo("hashed");
        assertThat(result.getRole()).isEqualTo(UserRole.TECHNICIAN);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_usernameExists_throws() {
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() ->
                authenticationService.register("existing", "pass", UserRole.ADMIN, "A", "a@b.cz"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }
}
