package com.example.bankapp.controller;
import com.example.bankapp.dto.AccountOperationRequest;
import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.model.Customer;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
@Controller
@RequiredArgsConstructor
@Slf4j
public class BankController {
    private static final String BANK_FEE_ACCOUNT = "999999";
    private final BankService bankService;
    private final TransactionAuditService auditService;
    @GetMapping("/")
    public String dashboard(
            @RequestParam(defaultValue = "") String keyword,
            Model model) {
        populateDashboard(model, keyword);
        return "bank";
    }
    @PostMapping("/customers")
    public String addCustomer(
            @Valid @ModelAttribute("customer") Customer customer,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateDashboard(model, "");
            model.addAttribute("customer", customer);
            return "bank";
        }
        try {
            bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("success", "Customer added successfully");
        } catch (BankException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/";
    }
    @PostMapping("/transfer")
    public String transfer(
            @Valid @ModelAttribute("transferRequest") TransferRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateDashboard(model, "");
            model.addAttribute("transferRequest", request);
            return "bank";
        }
        try {
            bankService.transfer(request);
            redirectAttributes.addFlashAttribute("success", "Transfer completed successfully");
        } catch (BankException exception) {
            auditService.recordFailure(
                    request.getFromAccount(), request.getToAccount(), request.getAmount(),
                    exception.getMessage());
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        } catch (RuntimeException exception) {
            log.error("Transfer failed unexpectedly; its transaction has been rolled back", exception);
            auditService.recordFailure(
                    request.getFromAccount(), request.getToAccount(), request.getAmount(),
                    "Transfer failed because of an internal system error");
            redirectAttributes.addFlashAttribute(
                    "error", "Transfer failed. All changes were rolled back.");
        }
        return "redirect:/";
    }
    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(
            @PathVariable String accountNumber,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("customer", bankService.getCustomer(accountNumber));
            model.addAttribute("transactions", bankService.getAccountHistory(accountNumber));
            model.addAttribute("totalSent", bankService.getTotalSent(accountNumber));
            return "history";
        } catch (BankException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/";
        }
    }
    @GetMapping("/large-transfers")
    public String largeTransfers(
            @RequestParam(defaultValue = "10000.00")
            @DecimalMin(value = "0.00", message = "Minimum amount cannot be negative")
            BigDecimal min,
            Model model) {
        model.addAttribute("min", min);
        model.addAttribute("transfers", bankService.getLargeTransfers(min));
        return "large-transfers";
    }
    @GetMapping("/deposit")
    public String depositPage(Model model) {
        populateOperationPage(model);
        return "deposit";
    }
    @PostMapping("/deposit")
    public String deposit(
            @Valid @ModelAttribute("operationRequest") AccountOperationRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateOperationPage(model);
            model.addAttribute("operationRequest", request);
            return "deposit";
        }
        try {
            bankService.deposit(request.getAccountNumber(), request.getAmount());
            redirectAttributes.addFlashAttribute("success", "Deposit completed successfully");
        } catch (BankException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/deposit";
    }
    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        populateOperationPage(model);
        return "withdraw";
    }
    @PostMapping("/withdraw")
    public String withdraw(
            @Valid @ModelAttribute("operationRequest") AccountOperationRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateOperationPage(model);
            model.addAttribute("operationRequest", request);
            return "withdraw";
        }
        try {
            bankService.withdraw(request.getAccountNumber(), request.getAmount());
            redirectAttributes.addFlashAttribute("success", "Withdrawal completed successfully");
        } catch (BankException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/withdraw";
    }
    private void populateDashboard(Model model, String keyword) {
        var allCustomers = bankService.getAllCustomers();
        var customers = allCustomers.stream()
                .filter(customer -> !BANK_FEE_ACCOUNT.equals(customer.getAccountNumber()))
                .toList();
        if (!keyword.isBlank()) {
            customers = bankService.searchCustomers(keyword).stream()
                    .filter(customer -> !BANK_FEE_ACCOUNT.equals(customer.getAccountNumber()))
                    .toList();
        }
        model.addAttribute("allCustomers", allCustomers);
        model.addAttribute("customers", customers);
        model.addAttribute("totalCustomers", bankService.getCustomerCount());
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("successCount", bankService.getSuccessfulTransactionCount());
        model.addAttribute("failedCount", bankService.getFailedTransactionCount());
        model.addAttribute("transactions", bankService.getLatestTransactions());
        model.addAttribute("keyword", keyword);
        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }
    }
    private void populateOperationPage(Model model) {
        model.addAttribute("customers", bankService.getAllCustomers().stream()
                .filter(customer -> !BANK_FEE_ACCOUNT.equals(customer.getAccountNumber()))
                .toList());
        if (!model.containsAttribute("operationRequest")) {
            model.addAttribute("operationRequest", new AccountOperationRequest());
        }
    }
}
