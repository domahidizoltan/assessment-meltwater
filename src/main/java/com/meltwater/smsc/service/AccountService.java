package com.meltwater.smsc.service;

import com.meltwater.smsc.exception.InvalidAccountArgumentsException;
import com.meltwater.smsc.exception.InvalidGroupNameException;
import com.meltwater.smsc.exception.InvalidGroupNumberPatternException;
import com.meltwater.smsc.model.Account;
import com.meltwater.smsc.repository.AccountRepository;
import com.meltwater.smsc.repository.NumberGroupRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AccountService {

    private AccountRepository accountRepository;
    private NumberGroupRepository numberGroupRepository;

    public AccountService(AccountRepository accountRepository, NumberGroupRepository numberGroupRepository) {
        this.accountRepository = accountRepository;
        this.numberGroupRepository = numberGroupRepository;
    }

    public void registerNumber(Account account) {
        log.info("Registering account: " + account);

        validateAccount(account);

        removeAccountIfAlreadyExists(account);

        accountRepository.save(account);
    }

    public void registerGroup(String groupName, List<String> numberPatterns) {
        log.info("Registering group {} with numbers: {}", groupName, numberPatterns);

        validateGroup(groupName, numberPatterns);

        List<String> mergedNumberPatterns = mergeWithExistingNumberPatterns(groupName, numberPatterns);

        numberGroupRepository.save(groupName, mergedNumberPatterns);
    }


    private void validateAccount(Account account) {
        if (hasInvalidName(account) || hasInvalidNumberFormat(account)) {
            throw  new InvalidAccountArgumentsException("Invalid name or number format");
        }
    }

    private boolean hasInvalidNumberFormat(Account account) {
        return !account.getNumber().matches("^\\+\\d{11}$");
    }

    private boolean hasInvalidName(Account account) {
        return !account.getName().matches("^number\\d+$");
    }

    private void removeAccountIfAlreadyExists(Account account) {
        Optional<Account> existingAccount = accountRepository.findByNumber(account.getNumber());
        existingAccount.ifPresent(accountRepository::delete);
    }

    private void validateGroup(String groupName, List<String> numberPatterns) {
        hasInvalidGroupName(groupName);
        hasInvalidNumberPatterns(numberPatterns);
    }

    private void hasInvalidNumberPatterns(List<String> numberPatterns) {
        if (!numberPatterns.stream().anyMatch(p -> p.matches("^\\+\\d{2,11}\\*?$"))) {
            throw new InvalidGroupNumberPatternException("Invalid number pattern");
        }
    }

    private void hasInvalidGroupName(String groupName) {
        if (!groupName.matches("^group\\d+$")) {
            throw new InvalidGroupNameException("Invalid group name");
        }
    }

    private List<String> mergeWithExistingNumberPatterns(String groupName, List<String> numberPatterns) {
        Optional<List<String>> existingPatterns = numberGroupRepository.findByGroupName(groupName);
        List<String> mergedList = new ArrayList<>(numberPatterns);
        existingPatterns.ifPresent(p -> {
            numberGroupRepository.deleteByGroupName(groupName);
            mergedList.addAll(p);
        });

        return mergedList;
    }
}
