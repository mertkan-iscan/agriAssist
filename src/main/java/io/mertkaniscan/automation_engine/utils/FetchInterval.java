package io.mertkaniscan.automation_engine.utils;

public enum FetchInterval {
    ONE_MINUTE(60),
    FIVE_MINUTES(300),
    TEN_MINUTES(600),
    FIFTEEN_MINUTES(900),
    THIRTY_MINUTES(1800),
    ONE_HOUR(3600);

    private final int seconds;

    FetchInterval(int seconds) {
        this.seconds = seconds;
    }

    public int getSeconds() {
        return seconds;
    }

    public long toMilliseconds() {
        return seconds * 1000L;
    }
}
