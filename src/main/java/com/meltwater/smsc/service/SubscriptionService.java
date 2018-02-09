package com.meltwater.smsc.service;

import com.meltwater.smsc.exception.NumberMustBeSubscribedException;
import com.meltwater.smsc.exception.NumberNotRegisteredException;
import com.meltwater.smsc.model.Account;
import com.meltwater.smsc.repository.AccountRepository;
import com.meltwater.smsc.repository.NumberGroupRepository;
import com.meltwater.smsc.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SubscriptionService {

    private SubscriptionRepository subscriptionRepository;
    private AccountRepository accountRepository;
    private NumberGroupRepository numberGroupRepository;
    private MessagingService messagingService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, AccountRepository accountRepository, NumberGroupRepository numberGroupRepository, MessagingService messagingService) {
        this.subscriptionRepository = subscriptionRepository;
        this.accountRepository = accountRepository;
        this.numberGroupRepository = numberGroupRepository;
        this.messagingService = messagingService;
    }

    public void subscribeNumber(String name) {
        log.info("Subsribe name: " + name);

        Account account = getAccountNumberByName(name);

        subscriptionRepository.save(account.getName(), account.getNumber());
    }

    public void unsubscribeNumber(String name) {
        log.info("Unsubscribe name: " + name);
        subscriptionRepository.delete(name);
    }

    public void sendMessage(String sourceName, List<String> destinationNames, String message) {
        log.info("Sending message(s) {} -> {} : {}", sourceName, destinationNames, message);

        isValidSubscription(sourceName);

        Account source = getAccountNumberByName(sourceName);

        destinationNames.stream()
                .forEach(destination -> sendMessageToDestinations(source, destination, message));
    }

    public void sendGroupMessage(String sourceName, String groupName, String message) {
        log.debug("Sending group message {} -> {} : {}", sourceName, groupName, message);

        isValidSubscription(sourceName);

        Account source = getAccountNumberByName(sourceName);

        List<Account> allDestinationAccounts = getAllAccountsByGroup(groupName);

        allDestinationAccounts.forEach(destination -> sendMessageToDestinations(source, destination.getName(), message));
    }

    public void broadCastMessage(String sourceName, String message) {
        log.info("Broadcasting message {} : {}", sourceName, message);

        isValidSubscription(sourceName);

        Account source = getAccountNumberByName(sourceName);
        accountRepository.findAll().forEach(destination -> sendMessageToDestinations(source, destination.getName(), message));
    }


    private void isValidSubscription(String sourceName) {
        if (subscriptionRepository.countByName(sourceName) == 0) {
            throw new NumberMustBeSubscribedException("Number must subscribe to send message");
        }
    }

    private Account getAccountNumberByName(String sourceName) {
        return accountRepository
                .findByName(sourceName)
                .orElseThrow(throwNumberNotRegistered());
    }

    private List<Account> getAllAccountsByGroup(String groupName) {
        Optional<List<String>> numbersByGroup = numberGroupRepository.findByGroupName(groupName);
        List<Account> allDestinationAccounts = new ArrayList<>();
        numbersByGroup.ifPresent(numbers -> {
            List<String> wildcardNumbers = getWildcardNumbers(numbers);
            numbers.removeAll(wildcardNumbers);
            allDestinationAccounts.addAll(accountRepository.findAllByNumbers(numbers));
            allDestinationAccounts.addAll(unfold(wildcardNumbers));
        });
        return allDestinationAccounts;
    }

    private List<Account> unfold(List<String> numbers) {
        return numbers.stream()
                .map(n -> n.replace("*",""))
                .map(accountRepository::findByNumbersLike)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<String> getWildcardNumbers(List<String> numbers) {
        return numbers.stream()
                        .filter(number -> number.endsWith("*"))
                        .collect(Collectors.toList());
    }

    private Supplier<NumberNotRegisteredException> throwNumberNotRegistered() {
        return () -> new NumberNotRegisteredException("Number is not registered");
    }

    private void sendMessageToDestinations(Account source, String destination, String message) {
        accountRepository
                .findByName(destination)
                .ifPresent(dest -> {
                    messagingService.send(source.getNumber(), dest.getNumber(), message);
                });
    }

}
