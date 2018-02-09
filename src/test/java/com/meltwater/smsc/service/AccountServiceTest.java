package com.meltwater.smsc.service;

import com.meltwater.smsc.exception.InvalidAccountArgumentsException;
import com.meltwater.smsc.exception.InvalidGroupNameException;
import com.meltwater.smsc.exception.InvalidGroupNumberPatternException;
import com.meltwater.smsc.model.Account;
import com.meltwater.smsc.repository.AccountRepository;
import com.meltwater.smsc.repository.NumberGroupRepository;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AccountServiceTest {

    private static final String NUMBER_1 = "+36991212321";
    private static final String NUMBER_4 = "+36991212121";
    private static final Account ACCOUNT_1 = new Account("number1", NUMBER_1);
    private static final String GROUP_1 = "group1";
    private static final String PATTERN_1 = "+3699123*";
    private static final List<String> PATTERNS = Arrays.asList(NUMBER_1, PATTERN_1);

    private AccountRepository accountRepository;
    private NumberGroupRepository numberGroupRepository;

    private AccountService accountService;

    @Before
    public void setUp() {
        accountRepository = new AccountRepository();
        numberGroupRepository = new NumberGroupRepository();

        accountService = new AccountService(accountRepository, numberGroupRepository);
    }

    @Test
    public void shouldRegisterNumber() {
        accountService.registerNumber(ACCOUNT_1);

        Optional<Account> expectedAccount = accountRepository.findByNumber(NUMBER_1);
        assertEquals(ACCOUNT_1, expectedAccount.get());
    }

    @Test
    public void shouldOverwriteNumberWhenNumberAlreadyExists() {
        accountService.registerNumber(ACCOUNT_1);

        accountService.registerNumber(ACCOUNT_1);

        Optional<Account> expectedAccount = accountRepository.findByNumber(NUMBER_1);
        assertEquals(ACCOUNT_1, expectedAccount.get());
    }

    @Test(expected = InvalidAccountArgumentsException.class)
    public void shouldThrowExceptionWhenNameNotStartsWithNumber() {
        Account invalidNameAccount = new Account("invalidName", NUMBER_1);

        accountService.registerNumber(invalidNameAccount);
    }
    
    @Test(expected = InvalidAccountArgumentsException.class)
    public void shouldThrowExceptionWhenNumberFormatIsInvalid() {
        Account invalidNumberFormat = new Account("number1", "3699");

        accountService.registerNumber(invalidNumberFormat);
    }

    @Test
    public void shouldCreateAccountGroups() {
        accountService.registerGroup(GROUP_1, PATTERNS);

        Optional<List<String>> numberPatterns = numberGroupRepository.findByGroupName(GROUP_1);
        assertTrue(numberPatterns.get().containsAll(PATTERNS));
    }

    @Test
    public void shouldAddNumberToAnExistingGroup() {
        accountService.registerGroup(GROUP_1, PATTERNS);

        accountService.registerGroup(GROUP_1, Arrays.asList(NUMBER_4));

        Optional<List<String>> numberPatterns = numberGroupRepository.findByGroupName(GROUP_1);
        List<String> expectedPatterns = new ArrayList<>(PATTERNS);
        expectedPatterns.add(NUMBER_4);
        assertTrue(numberPatterns.get().containsAll(expectedPatterns));
    }

    @Test(expected = InvalidGroupNameException.class)
    public void shouldThrowExceptionWhenGroupNameIsInvalid() {
        accountService.registerGroup("invalidGroup", PATTERNS);
    }

    @Test(expected = InvalidGroupNumberPatternException.class)
    public void shouldThrowExceptionWhenGroupNumberPatternIsInvalid() {
        accountService.registerGroup(GROUP_1, Arrays.asList("789+"));
    }
}
