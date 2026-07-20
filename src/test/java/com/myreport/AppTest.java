package com.myreport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {
    @Test
    void greetingShouldReturnHelloMessage() {
        assertEquals("Hello, MyReport!", ReportApplication.greeting());
    }
}
