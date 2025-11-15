package com.slimbahael.beauty_center.repository;

import com.slimbahael.beauty_center.model.BalanceTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface BalanceTransactionRepository extends MongoRepository<BalanceTransaction, String> {

    List<BalanceTransaction> findByUserIdOrderByCreatedAtDesc(String userId);

    List<BalanceTransaction> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status);

    List<BalanceTransaction> findByUserIdAndTransactionTypeOrderByCreatedAtDesc(String userId, String transactionType);

    List<BalanceTransaction> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(String userId, Date startDate, Date endDate);

    @Query("{ 'userId': ?0, 'transactionType': ?1, 'status': ?2 }")
    List<BalanceTransaction> findByUserIdAndTypeAndStatus(String userId, String transactionType, String status);

    List<BalanceTransaction> findByOrderId(String orderId);

    boolean existsByOrderId(String paymentIntentId);
}