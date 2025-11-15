package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.dto.GiftCardPurchaseRequest;
import com.slimbahael.beauty_center.exception.BadRequestException;
import com.slimbahael.beauty_center.model.BalanceTransaction;
import com.slimbahael.beauty_center.model.GiftCard;
import com.slimbahael.beauty_center.model.User;
import com.slimbahael.beauty_center.repository.GiftCardRepository;
import com.slimbahael.beauty_center.repository.UserRepository;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GiftCardServiceTest {

    @Mock private GiftCardRepository giftCardRepository;
    @Mock private UserRepository userRepository;
    @Mock private BalanceService balanceService;
    @Mock private EmailService emailService;
    @Mock private StripeService stripeService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private GiftCardService giftCardService;

    private GiftCardPurchaseRequest purchaseRequest;

    @BeforeEach
    void setUp() {
        purchaseRequest = new GiftCardPurchaseRequest(
                new BigDecimal("50.00"),
                "BALANCE",
                "buyer@example.com",
                "Buyer",
                "friend@example.com",
                "Friend",
                "Enjoy!",
                "pi_123"
        );
    }

    @Test
    void createGiftCardPersistsEntityAndSendsEmails() {
        when(stripeService.isPaymentSucceeded("pi_123")).thenReturn(true);
        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getAmount()).thenReturn(5000L); // cents
        when(stripeService.getPaymentIntent("pi_123")).thenReturn(paymentIntent);
        when(giftCardRepository.findByPaymentIntentId("pi_123")).thenReturn(Collections.emptyList());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");
        when(giftCardRepository.save(any(GiftCard.class))).thenAnswer(invocation -> {
            GiftCard saved = invocation.getArgument(0);
            saved.setId("gc_12345678");
            return saved;
        });
        when(userRepository.findByEmail("buyer@example.com"))
                .thenReturn(Optional.of(User.builder().id("buyer-id").build()));

        giftCardService.createGiftCard(purchaseRequest);

        ArgumentCaptor<GiftCard> giftCardCaptor = ArgumentCaptor.forClass(GiftCard.class);
        verify(giftCardRepository).save(giftCardCaptor.capture());
        GiftCard savedCard = giftCardCaptor.getValue();
        assertThat(savedCard.getType()).isEqualTo("BALANCE");
        assertThat(savedCard.getAmount()).isEqualByComparingTo("50.00");
        assertThat(savedCard.getPaymentIntentId()).isEqualTo("pi_123");
        assertThat(savedCard.getCodeHash()).isEqualTo("hashed-code");

        verify(balanceService).addTransaction(
                argThat(user -> "buyer-id".equals(user.getId())),
                eq(new BigDecimal("50.00")),
                eq("GIFT_CARD_PURCHASE"),
                startsWith("Achat carte cadeau")
        );

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendGiftCardPurchaseConfirmation(eq("buyer@example.com"), any(GiftCard.class), codeCaptor.capture());
        verify(emailService).sendGiftCardReceived(eq("friend@example.com"), any(GiftCard.class), anyString());
        verify(emailService, never()).sendAdminServiceGiftCardNotification(any());

        assertThat(codeCaptor.getValue()).isNotBlank();
    }

    @Test
    void redeemGiftCardCreditsBalanceAndLocksCard() {
        GiftCard giftCard = GiftCard.builder()
                .id("gc_12345678")
                .codeHash("hashed")
                .type("BALANCE")
                .amount(new BigDecimal("30.00"))
                .status("ACTIVE")
                .expirationDate(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
                .redemptionAttempts(0)
                .isLocked(false)
                .purchaserEmail("buyer@example.com")
                .recipientEmail("friend@example.com")
                .build();

        when(giftCardRepository.findActiveUnlockedGiftCards()).thenReturn(List.of(giftCard));
        when(passwordEncoder.matches("SECRET", "hashed")).thenReturn(true);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(User.builder().id("user-1").email("u@example.com").phoneNumber("+1000000000").build()));
        BalanceTransaction transaction = BalanceTransaction.builder().id("txn-1").build();
        when(balanceService.creditBalance(eq("user-1"), eq(new BigDecimal("30.00")), anyString(), eq("GIFT_CARD_REDEEM"), eq("gc_12345678")))
                .thenReturn(transaction);
        when(giftCardRepository.save(any(GiftCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BalanceTransaction result = giftCardService.redeemGiftCard("SECRET", "user-1", "127.0.0.1");

        assertThat(result).isSameAs(transaction);

        ArgumentCaptor<GiftCard> cardCaptor = ArgumentCaptor.forClass(GiftCard.class);
        verify(giftCardRepository).save(cardCaptor.capture());
        GiftCard updated = cardCaptor.getValue();
        assertThat(updated.getStatus()).isEqualTo("REDEEMED");
        assertThat(updated.getRedeemedByUserId()).isEqualTo("user-1");
        assertThat(updated.getRedeemedAt()).isNotNull();

        verify(emailService).sendGiftCardRedemptionConfirmation(eq("u@example.com"), eq(updated));
        verify(emailService).sendGiftCardRedeemedNotification(eq("buyer@example.com"), eq(updated));
    }

    @Test
    void redeemGiftCardRejectsServiceTypeForBalanceUse() {
        GiftCard giftCard = GiftCard.builder()
                .id("gc_3")
                .codeHash("hashed")
                .type("SERVICE")
                .amount(new BigDecimal("30.00"))
                .status("ACTIVE")
                .expirationDate(Date.from(Instant.now().plus(5, ChronoUnit.DAYS)))
                .redemptionAttempts(0)
                .isLocked(false)
                .build();

        when(giftCardRepository.findActiveUnlockedGiftCards()).thenReturn(List.of(giftCard));
        when(passwordEncoder.matches("SECRET", "hashed")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> giftCardService.redeemGiftCard("SECRET", "user-1", "127.0.0.1"));

        verify(balanceService, never()).creditBalance(any(), any(), any(), any(), any());
    }

    @Test
    void verifyGiftCardForAdminLocksAfterMaxAttempts() {
        GiftCard giftCard = GiftCard.builder()
                .id("gc_4")
                .verificationAttempts(10)
                .type("BALANCE")
                .status("ACTIVE")
                .amount(new BigDecimal("10.00"))
                .build();

        when(giftCardRepository.findByVerificationToken("token"))
                .thenReturn(Optional.of(giftCard));
        when(giftCardRepository.save(any(GiftCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GiftCard result = giftCardService.verifyGiftCardForAdmin("token");

        assertThat(result.getVerificationAttempts()).isEqualTo(11);
        assertThat(result.getIsLocked()).isTrue();
        assertThat(result.getLockedReason()).isEqualTo("Trop de tentatives de vérification");
    }
}
