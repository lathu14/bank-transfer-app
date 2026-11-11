package com.example.bankapp.config;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final CustomerRepository customerRepository;
    @Override
    public void run(String... args) {
        List<Customer> initialCustomers = new ArrayList<>();
        addIfMissing(initialCustomers, "100001", "Kamal Perera", "kamal@example.com", "50000.00");
        addIfMissing(initialCustomers, "100002", "Nimali Fernando", "nimali@example.com", "25000.00");
        addIfMissing(initialCustomers, "100003", "Saman Silva", "saman@example.com", "10000.00");
        addIfMissing(initialCustomers, "999999", "Bank Fee Account", "bank-fees@example.com", "0.00");
        if (!initialCustomers.isEmpty()) {
            customerRepository.saveAll(initialCustomers);
        }
    }
    private void addIfMissing(
            List<Customer> customers,
            String accountNumber,
            String fullName,
            String email,
            String balance) {
        if (customerRepository.countAccounts(accountNumber) == 0) {
            Customer customer = new Customer();
            customer.setAccountNumber(accountNumber);
            customer.setFullName(fullName);
            customer.setEmail(email);
            customer.setBalance(new BigDecimal(balance));
            customers.add(customer);
        }
    }
}