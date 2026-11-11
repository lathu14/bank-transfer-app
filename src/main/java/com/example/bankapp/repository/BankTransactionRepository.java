
package com.example.bankapp.repository;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.model.TransactionType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    // Get all transactions in latest order
    @Query("SELECT t FROM BankTransaction t ORDER BY t.createdAt DESC, t.id DESC")
    List<BankTransaction> findAllLatest();

    // Get transaction history for an account
    @Query("SELECT t FROM BankTransaction t WHERE t.fromAccount = :accountNumber OR t.toAccount = :accountNumber ORDER BY t.createdAt DESC, t.id DESC")
    List<BankTransaction> findHistory(
            @Param("accountNumber") String accountNumber);

    // Get transactions by status
    @Query("SELECT t FROM BankTransaction t WHERE t.status = :status ORDER BY t.createdAt DESC, t.id DESC")
    List<BankTransaction> findByStatusLatest(
            @Param("status") TransactionStatus status);

    // Count transactions by status
    @Query("SELECT COUNT(t) FROM BankTransaction t WHERE t.status = :status")
    long countByStatus(
            @Param("status") TransactionStatus status);

    // Get large transfers
    @Query("SELECT t FROM BankTransaction t WHERE t.transactionType = :type AND t.status = :status AND t.amount > :minimumAmount ORDER BY t.createdAt DESC, t.id DESC")
    List<BankTransaction> findLargeTransfers(
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("minimumAmount") BigDecimal minimumAmount);

    // Calculate total sent by account
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM BankTransaction t WHERE t.transactionType = :type AND t.status = :status AND t.fromAccount = :accountNumber")
    BigDecimal getTotalSentByAccount(
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("accountNumber") String accountNumber);
}
