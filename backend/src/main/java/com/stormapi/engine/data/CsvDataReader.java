package com.stormapi.engine.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads parameterized test data from CSV text.
 *
 * <p>Expects the first line to be a header row (column names).
 * Values are trimmed. Empty lines are skipped.
 * Enforces {@link DataReader#MAX_ROWS} limit.
 */
public class CsvDataReader implements DataReader {

    @Override
    public List<Map<String, String>> read(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<Map<String, String>> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            // First line = header
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("CSV must contain a header row");
            }

            String[] headers = parseLine(headerLine);
            if (headers.length == 0) {
                throw new IllegalArgumentException("CSV header row is empty");
            }

            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) {
                    continue;
                }

                if (rows.size() >= MAX_ROWS) {
                    throw new IllegalArgumentException(
                            "CSV exceeds maximum of " + MAX_ROWS + " data rows");
                }

                String[] values = parseLine(line);
                Map<String, String> row = new LinkedHashMap<>();

                for (int i = 0; i < headers.length; i++) {
                    String value = i < values.length ? values[i].trim() : "";
                    row.put(headers[i].trim(), value);
                }

                rows.add(row);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse CSV: " + e.getMessage(), e);
        }

        return rows;
    }

    @Override
    public String getFormat() {
        return "CSV";
    }

    /**
     * Parses a CSV line handling quoted fields (double-quote escaping).
     */
    private String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    // Check for escaped quote ""
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++; // skip next quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());

        return fields.toArray(new String[0]);
    }
}
