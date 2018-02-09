package com.meltwater.smsc.repository;

import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class NumberGroupRepository {

    Map<String, List<String>> groups = new HashMap<String, List<String>>();

    public void save(String groupName, List<String> numberPatterns) {
        groups.put(groupName, new ArrayList<>(numberPatterns));
    }

    public Optional<List<String>> findByGroupName(String groupName) {
        return Optional.ofNullable(groups.get(groupName));
    }

    public void deleteByGroupName(String groupName) {
        groups.remove(groupName);
    }
}
