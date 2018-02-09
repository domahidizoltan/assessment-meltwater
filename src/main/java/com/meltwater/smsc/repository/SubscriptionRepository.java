package com.meltwater.smsc.repository;

import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class SubscriptionRepository {

    Map<String, String> subscriptions = new HashMap<>();

    public long countByName(String name) {
        return subscriptions.keySet().stream()
                .filter(s -> s.equals(name))
                .count();
    }

    public long countByNumber(String number) {
        return subscriptions.values().stream()
                .filter(s -> s.equals(number))
                .count();
    }

    public void save(String name, String number) {
        subscriptions.put(name, number);
    }

    public void delete(String name) {
        subscriptions.remove(name);
    }
}
