package com.meltwater.smsc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "creationDate")
public class Redelivery {

    private final String source;
    private final String destination;
    private final String message;
    private Instant creationDate;

}
