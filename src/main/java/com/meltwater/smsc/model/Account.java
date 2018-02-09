package com.meltwater.smsc.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Account {

    private final String name;
    private final String number;

}
