package com.meltwater.smsc.service;

import com.meltwater.smsc.model.Redelivery;
import com.meltwater.smsc.repository.RedeliveryRepository;
import com.meltwater.smsc.repository.SubscriptionRepository;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class MessagingServiceTest {

    private static final String NAME_1 = "number1";
    private static final String NAME_2 = "number2";
    private static final String NUMBER_1 = "+36991212321";
    private static final String NUMBER_2 = "+36991234321";
    private static final String NUMBER_3 = "+36991234567";
    private static final String MESSAGE = "anyMessage";

    private SubscriptionRepository subscriptionRepository;
    private RedeliveryRepository redeliveryRepository;
    private MessagingService messagingService;

    @Before
    public void setUp() {
        subscriptionRepository = new SubscriptionRepository();
        redeliveryRepository = new RedeliveryRepository();

        subscriptionRepository.save(NAME_1, NUMBER_1);
        subscriptionRepository.save(NAME_2, NUMBER_2);

        messagingService = new MessagingService(subscriptionRepository, redeliveryRepository);
    }

    @Test
    public void shouldSendMessageWhenSubscriptionAvailable() {
        messagingService.send(NUMBER_1, NUMBER_2, MESSAGE);

        List<Redelivery> actualRedeliveries = redeliveryRepository.findAll();
        assertTrue(actualRedeliveries.isEmpty());
    }

    @Test
    public void shouldSaveMessageWhenSubscriptionIsNotAvailable() {
        subscriptionRepository.delete(NAME_2);

        messagingService.send(NUMBER_1, NUMBER_2, MESSAGE);

        List<Redelivery> actualRedeliveries = redeliveryRepository.findAll();
        Redelivery actualRedelivery = actualRedeliveries.get(0);
        assertEquals(1, actualRedeliveries.size());
        assertRedelivery(actualRedelivery, NUMBER_1, NUMBER_2);
    }

    @Test
    public void shouldTryToRedeliverAllMessagesToActualSubscriptions() {
        Redelivery number1ToNumber2 = new Redelivery(NUMBER_1, NUMBER_2, MESSAGE, Instant.now());
        Redelivery number1ToNumber3 = new Redelivery(NUMBER_1, NUMBER_3, MESSAGE, Instant.now());
        redeliveryRepository.save(number1ToNumber2);
        redeliveryRepository.save(number1ToNumber3);

        messagingService.redeliver();

        List<Redelivery> actualRedelivery = redeliveryRepository.findAll();
        assertRedelivery(actualRedelivery.get(0), NUMBER_1, NUMBER_3);
    }

    @Test
    public void shouldRemoveExpiredMessages() {
        Instant fiveMinutesAgo = Instant.now().minus(Duration.ofMinutes(6));
        Redelivery number1ToNumber2 = new Redelivery(NUMBER_1, NUMBER_2, MESSAGE, fiveMinutesAgo);
        redeliveryRepository.save(number1ToNumber2);

        messagingService.redeliver();

        List<Redelivery> actualRedeliveries = redeliveryRepository.findAll();
        assertTrue(actualRedeliveries.isEmpty());
    }

    private void assertRedelivery(Redelivery actualRedelivery, String expectedSource, String expectedDestination) {
        assertEquals(actualRedelivery.getSource(), expectedSource);
        assertEquals(actualRedelivery.getDestination(), expectedDestination);
    }

}
