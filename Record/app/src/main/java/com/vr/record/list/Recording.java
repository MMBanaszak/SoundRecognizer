package com.vr.record.list;

import java.io.Serializable;

public class Recording implements Serializable {

    private final String name;
    private final Long timestamp;

    public Recording(String name, Long timestamp) {
        this.name = name;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}
