package com.meltwater.smsc.repository;

import com.meltwater.smsc.model.Account;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class AccountRepository {

    private final List<Account> accounts = new ArrayList();

    public void save(Account account) {
        accounts.add(account);
    }

    public void delete(Account account) {
        accounts.remove(account);
    }

    public List<Account> findAll() {
        return accounts;
    }

    public Optional<Account> findByNumber(String number) {
        return accounts.stream()
                .filter(ac -> number.equals(ac.getNumber()))
                .findFirst();
    }

    public Optional<Account> findByName(String name) {
        return accounts.stream()
                .filter(ac -> name.equals(ac.getName()))
                .findFirst();
    }

    public List<Account> findAllByNumbers(List<String> numbers) {
        ArrayList<Account> filteredAccounts = new ArrayList<>(this.accounts);
        filteredAccounts.retainAll(numbers);
        return filteredAccounts;
    }

    public List<Account> findByNumbersLike(String number) {
        return accounts.stream()
                .filter(a -> a.getNumber().startsWith(number))
                .collect(Collectors.toList());
    }
}
