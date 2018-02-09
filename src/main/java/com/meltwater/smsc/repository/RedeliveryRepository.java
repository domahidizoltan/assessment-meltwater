package com.meltwater.smsc.repository;

import com.meltwater.smsc.model.Redelivery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class RedeliveryRepository {

    private List<Redelivery> redeliveries = new ArrayList<>();

    public List<Redelivery> findAll() {
        return redeliveries;
    }

    public void save(Redelivery redelivery) {
        redeliveries.add(redelivery);
    }

    public Optional<Redelivery> findByItem(Redelivery redelivery) {
        int index = redeliveries.indexOf(redelivery);

        if (index > -1) {
            return Optional.of(redeliveries.get(index));
        } else {
            return Optional.empty();
        }

    }

    public void delete(Redelivery redelivery) {
        redeliveries.remove(redelivery);
    }
}
