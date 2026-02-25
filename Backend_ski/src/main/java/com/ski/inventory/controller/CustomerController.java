package com.ski.inventory.controller;

import com.ski.inventory.model.Customer;
import com.ski.inventory.repository.CustomerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/technician/customers")
@PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
@CrossOrigin(origins = "*")
public class CustomerController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_ALLOWED_PATTERN = Pattern.compile("^\\+?[\\d\\s\\-()]+$");

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public ResponseEntity<List<CustomerSummaryResponse>> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        return ResponseEntity.ok(customers.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomer(@PathVariable Long id) {
        Optional<Customer> opt = customerRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDetailResponse(opt.get()));
    }

    @PostMapping
    public ResponseEntity<?> createCustomer(@RequestBody CreateCustomerRequest body) {
        String name = body.name() != null ? body.name().trim() : null;
        String email = body.email() != null ? body.email().trim() : null;
        String phone = body.phone() != null ? body.phone().trim() : null;
        String address = body.address() != null && !body.address().isBlank() ? body.address().trim() : null;
        String validationError = validateContact(name, email, phone);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(new ErrorMessage(validationError));
        }
        Customer c = new Customer();
        c.setName(name);
        c.setEmail(email);
        c.setPhone(phone);
        c.setAddress(address);
        c = customerRepository.save(c);
        return ResponseEntity.ok(toResponse(c));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable Long id, @RequestBody UpdateCustomerRequest body) {
        Optional<Customer> opt = customerRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String name = body.name() != null ? body.name().trim() : null;
        String email = body.email() != null ? body.email().trim() : null;
        String phone = body.phone() != null ? body.phone().trim() : null;
        String address = body.address() != null && !body.address().isBlank() ? body.address().trim() : null;
        String validationError = validateContact(name, email, phone);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(new ErrorMessage(validationError));
        }
        Customer c = opt.get();
        c.setName(name);
        c.setEmail(email);
        c.setPhone(phone);
        c.setAddress(address);
        c = customerRepository.save(c);
        return ResponseEntity.ok(toResponse(c));
    }

    /** Vrací chybovou zprávu nebo null pokud je validace OK. */
    private String validateContact(String name, String email, String phone) {
        if (name == null || name.isBlank()) {
            return "Jméno je povinné.";
        }
        if (email == null || email.isBlank()) {
            return "E-mail je povinný.";
        }
        if (phone == null || phone.isBlank()) {
            return "Telefon je povinný.";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Neplatný formát e-mailu.";
        }
        String phoneDigitsOnly = phone.replaceAll("\\D", "");
        if (phoneDigitsOnly.length() < 9 || phoneDigitsOnly.length() > 15) {
            return "Telefonní číslo musí obsahovat 9–15 číslic.";
        }
        if (!PHONE_ALLOWED_PATTERN.matcher(phone).matches()) {
            return "Telefonní číslo může obsahovat jen číslice, mezeru, pomlčku, závorky a znak + na začátku.";
        }
        return null;
    }

    private CustomerSummaryResponse toResponse(Customer c) {
        return new CustomerSummaryResponse(c.getId(), c.getCustomerNumber(), c.getName(), c.getPhone(), c.getEmail());
    }

    private CustomerDetailResponse toDetailResponse(Customer c) {
        return new CustomerDetailResponse(c.getId(), c.getCustomerNumber(), c.getName(), c.getPhone(), c.getEmail(), c.getAddress());
    }

    public record CreateCustomerRequest(String name, String email, String phone, String address) {}
    public record UpdateCustomerRequest(String name, String email, String phone, String address) {}
    public record CustomerSummaryResponse(Long id, String customerNumber, String name, String phone, String email) {}
    public record CustomerDetailResponse(Long id, String customerNumber, String name, String phone, String email, String address) {}
    public record ErrorMessage(String message) {}
}
