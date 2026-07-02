package com.stormapi.engine.data;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsvDataReaderTest {

    private final CsvDataReader reader = new CsvDataReader();

    @Test
    void shouldParseSimpeCsv() {
        String csv = """
                username,password,expected
                alice,pass123,200
                bob,secret,200
                """;

        List<Map<String, String>> rows = reader.read(csv);

        assertEquals(2, rows.size());
        assertEquals("alice", rows.get(0).get("username"));
        assertEquals("pass123", rows.get(0).get("password"));
        assertEquals("200", rows.get(0).get("expected"));
        assertEquals("bob", rows.get(1).get("username"));
    }

    @Test
    void shouldHandleQuotedFields() {
        String csv = "name,value\n"
                + "\"Alice, Bob\",test\n"
                + "simple,\"has \"\"quotes\"\"\"\n";

        List<Map<String, String>> rows = reader.read(csv);

        assertEquals(2, rows.size());
        assertEquals("Alice, Bob", rows.get(0).get("name"));
        assertEquals("has \"quotes\"", rows.get(1).get("value"));
    }

    @Test
    void shouldSkipEmptyLines() {
        String csv = """
                id,name
                1,Alice
                
                2,Bob
                """;

        List<Map<String, String>> rows = reader.read(csv);

        assertEquals(2, rows.size());
    }

    @Test
    void shouldHandleMissingTrailingValues() {
        String csv = """
                a,b,c
                1,2
                """;

        List<Map<String, String>> rows = reader.read(csv);

        assertEquals(1, rows.size());
        assertEquals("1", rows.get(0).get("a"));
        assertEquals("2", rows.get(0).get("b"));
        assertEquals("", rows.get(0).get("c"));
    }

    @Test
    void shouldReturnEmptyForNullContent() {
        assertTrue(reader.read(null).isEmpty());
    }

    @Test
    void shouldReturnEmptyForBlankContent() {
        assertTrue(reader.read("   ").isEmpty());
    }

    @Test
    void shouldThrowForEmptyHeader() {
        assertThrows(IllegalArgumentException.class, () -> reader.read("\n1,2\n"));
    }

    @Test
    void shouldReturnCsvFormat() {
        assertEquals("CSV", reader.getFormat());
    }
}
