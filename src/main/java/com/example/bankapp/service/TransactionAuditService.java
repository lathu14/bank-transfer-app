package com.example.bankapp.service;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.model.TransactionType;
import com.example.bankapp.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionAuditService {
    private final BankTransactionRepository transactionRepository;
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BankTransaction recordFailure(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            String description) {
        BankTransaction transaction = new BankTransaction();
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount == null ? BigDecimal.ZERO : amount);
        transaction.setFee(BigDecimal.ZERO);
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setDescription(description);
        return transactionRepository.save(transaction);
    }
}
