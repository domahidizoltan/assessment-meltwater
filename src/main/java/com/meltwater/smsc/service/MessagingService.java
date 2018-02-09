package com.meltwater.smsc.service;

import com.meltwater.smsc.model.Redelivery;
import com.meltwater.smsc.repository.RedeliveryRepository;
import com.meltwater.smsc.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

@Service
@Slf4j
public class MessagingService {

    private SubscriptionRepository subscriptionRepository;
    private RedeliveryRepository redeliveryRepository;

    public MessagingService(SubscriptionRepository subscriptionRepository, RedeliveryRepository redeliveryRepository) {

        this.subscriptionRepository = subscriptionRepository;
        this.redeliveryRepository = redeliveryRepository;
    }

    public void send(String sourceNumber, String destinationNumber, String message) {
        log.info("Sending message {} -> {} : {}", sourceNumber, destinationNumber, message);

        Redelivery redelivery = new Redelivery(sourceNumber, destinationNumber, message, Instant.now());
        if (bothSidesAreSubscribed(sourceNumber, destinationNumber)) {
            System.out.println(sourceNumber + " -> " + destinationNumber + " : " + message);
            redeliveryRepository.delete(redelivery);
        } else {
            saveForRedelivery(redelivery);
        }

    }

    @Scheduled(fixedRate = 3000)
    public void redeliver() {
        log.info("Redelivering messages");

        ArrayList<Redelivery> redeliveries = new ArrayList<>(redeliveryRepository.findAll());
        log.debug("{} messages needs to be redelivered: {}", redeliveries.size(), redeliveries);

        redeliveries.forEach(r -> send(r.getSource(), r.getDestination(), r.getMessage()));
    }

    private boolean bothSidesAreSubscribed(String sourceNumber, String destinationNumber) {
        return subscriptionRepository.countByNumber(sourceNumber) > 0 && subscriptionRepository.countByNumber(destinationNumber) > 0;
    }

    private void saveForRedelivery(Redelivery redelivery) {
        Optional<Redelivery> savedRedelivery = redeliveryRepository.findByItem(redelivery);
        if (!savedRedelivery.isPresent()) {
            redeliveryRepository.save(redelivery);
        }
    }

}
