package com.meltwater.smsc.service;

import com.meltwater.smsc.exception.NumberNotRegisteredException;
import com.meltwater.smsc.model.Account;
import com.meltwater.smsc.repository.AccountRepository;
import com.meltwater.smsc.repository.NumberGroupRepository;
import com.meltwater.smsc.repository.SubscriptionRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SubscriptionServiceTest {

    private static final String NAME_1 = "number1";
    private static final String NAME_2 = "number2";
    private static final String NAME_3 = "number3";
    private static final String GROUP_1 = "group1";
    private static final String MESSAGE = "message";
    private static final String NUMBER_1 = "+36991212321";
    private static final String NUMBER_2 = "+36991234321";
    private static final String NUMBER_3 = "+36991234567";

    private SubscriptionRepository subscriptionRepository;
    private AccountRepository accountRepository;
    private NumberGroupRepository numberGroupRepository;
    private MessagingService messagingServiceMock = mock(MessagingService.class);

    private SubscriptionService subscriptionService;

    @Before
    public void setUp() {
        accountRepository = new AccountRepository();
        numberGroupRepository = new NumberGroupRepository();
        subscriptionRepository = new SubscriptionRepository();
        Mockito.reset(messagingServiceMock);

        prepareData();

        numberGroupRepository.save(GROUP_1, Arrays.asList("+3699123*"));

        subscriptionService = new SubscriptionService(subscriptionRepository, accountRepository, numberGroupRepository, messagingServiceMock);
    }

    @Test
    public void shouldSubscribeNumber() {
        subscriptionRepository.delete(NAME_1);

        subscriptionService.subscribeNumber(NAME_1);

        long expectedSubscriptions = subscriptionRepository.countByName(NAME_1);
        assertEquals(1, expectedSubscriptions);
    }

    @Test(expected = NumberNotRegisteredException.class)
    public void shouldThrowExceptionOnSubscriptionWhenNumberIsNotRegistered() {
        Account name1 = accountRepository.findByName(NAME_1).get();
        accountRepository.delete(name1);

        subscriptionService.subscribeNumber(NAME_1);
    }

    @Test
    public void shouldUnsubscribeNumber() {
        subscriptionService.subscribeNumber(NAME_1);

        subscriptionService.unsubscribeNumber(NAME_1);

        long expectedSubscriptions = subscriptionRepository.countByName(NAME_1);
        assertEquals(0, expectedSubscriptions);
    }

    @Test
    public void shouldNotThrowExceptionOnUnsubscriptionWhenNumberNotSubscribed() {
        subscriptionService.unsubscribeNumber(NAME_1);

        long expectedSubscriptions = subscriptionRepository.countByName(NAME_1);
        assertEquals(0, expectedSubscriptions);
    }

    @Test
    public void shouldSendMessageToNumber() {
        subscriptionService.sendMessage(NAME_1, Arrays.asList(NAME_2), MESSAGE);

        verify(messagingServiceMock).send(eq(NUMBER_1), eq(NUMBER_2), eq(MESSAGE));
    }

    @Test
    public void shouldSendMessageToNumbers() {
        subscriptionService.sendMessage(NAME_1, Arrays.asList(NAME_2, NAME_3), MESSAGE);

        verify(messagingServiceMock).send(eq(NUMBER_1), eq(NUMBER_2), eq(MESSAGE));
        verify(messagingServiceMock).send(eq(NUMBER_1), eq(NUMBER_3), eq(MESSAGE));
    }

    @Test
    public void shouldSendMessageToGroup() {
        subscriptionService.sendGroupMessage(NAME_1, GROUP_1, MESSAGE);

        verify(messagingServiceMock).send(eq(NUMBER_1), eq(NUMBER_2), eq(MESSAGE));
        verify(messagingServiceMock).send(eq(NUMBER_1), eq(NUMBER_3), eq(MESSAGE));
    }

    @Test
    public void shouldBroadcastMessage() {
        subscriptionService.broadCastMessage(NAME_1, MESSAGE);

        verify(messagingServiceMock).send(eq(NUMBER_1), eq(NUMBER_2), eq(MESSAGE));
        verify(messagingServiceMock).send(eq(NUMBER_1), eq(NUMBER_3), eq(MESSAGE));
    }

    private void prepareData() {
        accountRepository.save(new Account(NAME_1, NUMBER_1));
        accountRepository.save(new Account(NAME_2, NUMBER_2));
        accountRepository.save(new Account(NAME_3, NUMBER_3));
        subscriptionRepository.save(NAME_1, NUMBER_1);
        subscriptionRepository.save(NAME_2, NUMBER_2);
        subscriptionRepository.save(NAME_3, NUMBER_3);
    }

}
