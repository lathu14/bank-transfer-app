
package com.example.bankapp.service;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;

class BankServiceTest {

    private CustomerRepository customerRepository;
    private BankTransactionRepository transactionRepository;
    private BankService bankService;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        transactionRepository = mock(BankTransactionRepository.class);

        bankService = new BankService(
                customerRepository,
                transactionRepository
        );
    }

    @Test
    void transferDebitsFeeAndCreditsReceiverAndFeeAccount() {

        when(customerRepository.getByAccountNumber(anyString()))
                .thenReturn(Optional.of(new Customer()));

        when(customerRepository.debit(
                "100001", new BigDecimal("101.00")
        )).thenReturn(1);

        when(customerRepository.credit(
                "100002", new BigDecimal("100.00")
        )).thenReturn(1);

        when(customerRepository.credit(
                "999999", new BigDecimal("1.00")
        )).thenReturn(1);

        when(transactionRepository.save(any(BankTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankTransaction result =
                bankService.transfer(request("100.00", false));

        verify(customerRepository).debit(
                "100001", new BigDecimal("101.00")
        );

        verify(customerRepository).credit(
                "100002", new BigDecimal("100.00")
        );

        verify(customerRepository).credit(
                "999999", new BigDecimal("1.00")
        );

        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals(new BigDecimal("1.00"), result.getFee());
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
    }

    @Test
    void simulatedFailureStopsBeforeCreditsAndSuccessRecord() {

        when(customerRepository.getByAccountNumber(anyString()))
                .thenReturn(Optional.of(new Customer()));

        when(customerRepository.debit(
                "100001", new BigDecimal("101.00")
        )).thenReturn(1);

        BankException exception = assertThrows(
                BankException.class,
                () -> bankService.transfer(request("100.00", true))
        );

        assertEquals(
                "All changes were rolled back (simulated system failure)",
                exception.getMessage()
        );

        verify(customerRepository, never())
                .credit(anyString(), any(BigDecimal.class));

        verify(transactionRepository, never())
                .save(any(BankTransaction.class));
    }

    @Test
    void insufficientBalanceDoesNotCreditReceiver() {

        when(customerRepository.getByAccountNumber(anyString()))
                .thenReturn(Optional.of(new Customer()));

        when(customerRepository.debit(
                "100001", new BigDecimal("101.00")
        )).thenReturn(0);

        assertThrows(
                InsufficientBalanceException.class,
                () -> bankService.transfer(request("100.00", false))
        );

        verify(customerRepository, never())
                .credit(anyString(), any(BigDecimal.class));

        verify(transactionRepository, never())
                .save(any(BankTransaction.class));
    }

    private TransferRequest request(
            String amount,
            boolean simulateFailure
    ) {
        TransferRequest request = new TransferRequest();

        request.setFromAccount("100001");
        request.setToAccount("100002");
        request.setAmount(new BigDecimal(amount));
        request.setSimulateFailure(simulateFailure);

        return request;
    }
}
