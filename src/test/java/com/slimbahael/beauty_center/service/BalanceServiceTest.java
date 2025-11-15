package com.slimbahael.beauty_center.service;

import com.slimbahael.beauty_center.exception.BadRequestException;
import com.slimbahael.beauty_center.exception.ResourceNotFoundException;
import com.slimbahael.beauty_center.model.BalanceTransaction;
import com.slimbahael.beauty_center.model.User;
import com.slimbahael.beauty_center.repository.BalanceTransactionRepository;
import com.slimbahael.beauty_center.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BalanceTransactionRepository balanceTransactionRepository;

    @Mock
    private StripeService stripeService; // required by constructor

    @InjectMocks
    private BalanceService balanceService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("user-1");
        user.setBalance(new BigDecimal("50.00"));
    }

    @Test
    void creditBalancePersistsUpdatedUserAndTransaction() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceTransactionRepository.save(any(BalanceTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BalanceTransaction result = balanceService.creditBalance(
                "user-1",
                new BigDecimal("20.00"),
                "Top up",
                "CREDIT",
                "ref-1");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getBalance()).isEqualByComparingTo("70.00");
        assertThat(savedUser.getLastBalanceUpdate()).isNotNull();

        ArgumentCaptor<BalanceTransaction> txCaptor = ArgumentCaptor.forClass(BalanceTransaction.class);
        verify(balanceTransactionRepository).save(txCaptor.capture());
        BalanceTransaction tx = txCaptor.getValue();
        assertThat(tx.getBalanceBefore()).isEqualByComparingTo("50.00");
        assertThat(tx.getBalanceAfter()).isEqualByComparingTo("70.00");
        assertThat(tx.getTransactionType()).isEqualTo("CREDIT");
        assertThat(tx.getOrderId()).isEqualTo("ref-1");
        assertThat(tx.getCreatedAt()).isNotNull();
        assertThat(result).isSameAs(tx);
    }

    @Test
    void debitBalanceReducesUserBalance() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceTransactionRepository.save(any(BalanceTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BalanceTransaction result = balanceService.debitBalance(
                "user-1",
                new BigDecimal("15.00"),
                "Purchase",
                "DEBIT",
                "order-7");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getBalance()).isEqualByComparingTo("35.00");

        ArgumentCaptor<BalanceTransaction> txCaptor = ArgumentCaptor.forClass(BalanceTransaction.class);
        verify(balanceTransactionRepository).save(txCaptor.capture());
        BalanceTransaction tx = txCaptor.getValue();
        assertThat(tx.getBalanceBefore()).isEqualByComparingTo("50.00");
        assertThat(tx.getBalanceAfter()).isEqualByComparingTo("35.00");
        assertThat(tx.getTransactionType()).isEqualTo("DEBIT");
        assertThat(tx.getOrderId()).isEqualTo("order-7");
        assertThat(result).isSameAs(tx);
    }

    @Test
    void creditBalanceRejectsNonPositiveAmount() {
        assertThrows(BadRequestException.class, () -> balanceService.creditBalance(
                "user-1", BigDecimal.ZERO, "desc", "CREDIT", null));
    }

    @Test
    void debitBalanceThrowsForInsufficientFunds() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> balanceService.debitBalance(
                "user-1",
                new BigDecimal("80.00"),
                "Purchase",
                "DEBIT",
                null));
    }

    @Test
    void debitBalanceThrowsWhenUserMissing() {
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> balanceService.debitBalance(
                "user-1",
                new BigDecimal("10.00"),
                "desc",
                "DEBIT",
                null));
    }
}
