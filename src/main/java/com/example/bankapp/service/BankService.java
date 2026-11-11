package com.example.bankapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.model.TransactionType;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {
    
    private static final String BANK_FEE_ACCOUNT = "999999";
    private static final BigDecimal TRANSFER_FEE_RATE = new BigDecimal("0.01");
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final CustomerRepository customerRepository;
    
    
    private final BankTransactionRepository transactionRepository;
    @Transactional(rollbackFor = Exception.class)
    
    public BankTransaction transfer(TransferRequest request) {
        validateAmount(request.getAmount());
        if (Objects.equals(request.getFromAccount(), request.getToAccount())) {
            throw new BankException("Sender and receiver must be different accounts");
        }
        
        if (BANK_FEE_ACCOUNT.equals(request.getFromAccount())
                || BANK_FEE_ACCOUNT.equals(request.getToAccount())) {
            throw new BankException("The bank fee account cannot be used for customer transfers");
        }
        customerRepository.getByAccountNumber(request.getFromAccount())
                .orElseThrow(() -> new AccountNotFoundException(request.getFromAccount()));
        customerRepository.getByAccountNumber(request.getToAccount())
                .orElseThrow(() -> new AccountNotFoundException(request.getToAccount()));
        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal fee = amount.multiply(TRANSFER_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDebit = amount.add(fee);
        
        if (customerRepository.debit(request.getFromAccount(), totalDebit) == 0) {
            throw new InsufficientBalanceException(request.getFromAccount());
        }
        
        if (request.isSimulateFailure()) {
            throw new BankException("All changes were rolled back (simulated system failure)");
        }
        
        if (customerRepository.credit(request.getToAccount(), amount) == 0) {
            throw new AccountNotFoundException(request.getToAccount());
        }
        
        if (customerRepository.credit(BANK_FEE_ACCOUNT, fee) == 0) {
            throw new AccountNotFoundException(BANK_FEE_ACCOUNT);
        }
        BankTransaction transaction = newTransaction(
                TransactionType.TRANSFER,
                request.getFromAccount(),
                request.getToAccount(),
                amount,
                fee,
                "Transfer completed; 1% fee: LKR " + fee.toPlainString());
        return transactionRepository.save(transaction);
    }
    
    @Transactional
    public Customer createCustomer(Customer customer) {
        String accountNumber = customer.getAccountNumber().trim();
        String email = customer.getEmail().trim();
        if (customerRepository.countAccounts(accountNumber) > 0) {
            throw new BankException("Account number " + accountNumber + " already exists");
        }
        if (customerRepository.countEmails(email) > 0) {
            throw new BankException("Email " + email + " is already registered");
        }
        customer.setAccountNumber(accountNumber);
        customer.setEmail(email);
        customer.setBalance(customer.getBalance().setScale(2, RoundingMode.UNNECESSARY));
        return customerRepository.save(customer);
    }
    
    @Transactional
    public BankTransaction deposit(String accountNumber, BigDecimal amount) {
        validateAmount(amount);
        ensureCustomerAccount(accountNumber);
        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.UNNECESSARY);
        if (customerRepository.credit(accountNumber, normalizedAmount) == 0) {
            throw new AccountNotFoundException(accountNumber);
        }
        return transactionRepository.save(newTransaction(
                TransactionType.DEPOSIT, null, accountNumber, normalizedAmount, ZERO,
                "Cash deposit"));
    }
    
    @Transactional
    public BankTransaction withdraw(String accountNumber, BigDecimal amount) {
        validateAmount(amount);
        ensureCustomerAccount(accountNumber);
        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.UNNECESSARY);
        if (customerRepository.debit(accountNumber, normalizedAmount) == 0) {
            throw new InsufficientBalanceException(accountNumber);
        }
        return transactionRepository.save(newTransaction(
                TransactionType.WITHDRAWAL, accountNumber, null, normalizedAmount, ZERO,
                "Cash withdrawal"));
    }
    
    public List<Customer> getAllCustomers() {
        return customerRepository.findAllOrdered();
    }
    
    public List<Customer> searchCustomers(String keyword) {
        return customerRepository.searchByName(keyword);
    }
    
    public Customer getCustomer(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }
    
    public long getCustomerCount() {
        return customerRepository.countRegularCustomers();
    }
    
    public BigDecimal getTotalBalance() {
        BigDecimal total = customerRepository.getTotalBalance();
        return total == null ? ZERO : total;
    }
    
    public List<BankTransaction> getLatestTransactions() {
        return transactionRepository.findAllLatest();
    }
    
    public long getSuccessfulTransactionCount() {
        return transactionRepository.countByStatus(TransactionStatus.SUCCESS);
    }
    
    public long getFailedTransactionCount() {
        return transactionRepository.countByStatus(TransactionStatus.FAILED);
    }
    
    public List<BankTransaction> getAccountHistory(String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }
    
    public BigDecimal getTotalSent(String accountNumber) {
        BigDecimal total = transactionRepository.getTotalSentByAccount(
                TransactionType.TRANSFER, TransactionStatus.SUCCESS, accountNumber);
        return total == null ? ZERO : total;
    }
    
    public List<BankTransaction> getLargeTransfers(BigDecimal minimumAmount) {
        return transactionRepository.findLargeTransfers(
                TransactionType.TRANSFER, TransactionStatus.SUCCESS, minimumAmount);
    }
   
    private void ensureCustomerAccount(String accountNumber) {
        if (BANK_FEE_ACCOUNT.equals(accountNumber)) {
            throw new BankException("The bank fee account cannot be used for cash operations");
        }
        customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }
    
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0
                || amount.stripTrailingZeros().scale() > 2) {
            throw new BankException("Amount must be positive and have no more than 2 decimal places");
        }
    }
    
    private BankTransaction newTransaction(
            TransactionType type,
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            BigDecimal fee,
            String description) {
        BankTransaction transaction = new BankTransaction();
        transaction.setTransactionType(type);
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setFee(fee);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(description);
        return transaction;
    }
}
