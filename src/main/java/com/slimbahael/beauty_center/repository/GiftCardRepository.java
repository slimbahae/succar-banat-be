package com.slimbahael.beauty_center.repository;

import com.slimbahael.beauty_center.model.GiftCard;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface GiftCardRepository extends MongoRepository<GiftCard, String> {

    Optional<GiftCard> findByCodeHash(String codeHash);

    List<GiftCard> findByPurchaserEmailOrderByCreatedAtDesc(String purchaserEmail);

    List<GiftCard> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);

    List<GiftCard> findByStatusOrderByCreatedAtDesc(String status);

    List<GiftCard> findByTypeAndStatusOrderByCreatedAtDesc(String type, String status);

    @Query("{'expirationDate': {'$lt': ?0}, 'status': 'ACTIVE'}")
    List<GiftCard> findExpiredActiveGiftCards(Date currentDate);

    @Query("{'expirationDate': {'$lt': ?0}, 'status': 'ACTIVE'}")
    long countExpiredActiveGiftCards(Date currentDate);

    Optional<GiftCard> findByVerificationToken(String verificationToken);

    List<GiftCard> findByPaymentIntentId(String paymentIntentId);

    @Query("{'createdAt': {'$gte': ?0, '$lte': ?1}}")
    List<GiftCard> findByCreatedAtBetween(Date startDate, Date endDate);

    long countByStatusAndType(String status, String type);

    @Query("{'status': 'ACTIVE', 'isLocked': false}")
    List<GiftCard> findActiveUnlockedGiftCards();
}