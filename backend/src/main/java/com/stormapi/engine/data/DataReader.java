package com.stormapi.engine.data;

import java.util.List;
import java.util.Map;

/**
 * Reads parameterized test data from a source (CSV or JSON).
 *
 * <p>Each row is a {@code Map<String,String>} where keys are column/field names
 * and values are their string representations. Template resolution replaces
 * {@code {{variableName}}} placeholders with values from the row.
 *
 * <p>Implementations MUST:
 * <ul>
 *   <li>Enforce a hard limit of {@value #MAX_ROWS} rows</li>
 *   <li>Never return null — return {@code List.of()} for empty input</li>
 *   <li>Throw {@link IllegalArgumentException} for parse failures</li>
 * </ul>
 */
public interface DataReader {

    /** Maximum data rows to prevent OOM from large uploads. */
    int MAX_ROWS = 1000;

    /**
     * Parses raw text content into a list of keyed rows.
     *
     * @param content raw text (CSV or JSON)
     * @return parsed rows, each row as a String→String map
     * @throws IllegalArgumentException if content is malformed
     */
    List<Map<String, String>> read(String content);

    /**
     * Returns the format this reader handles (e.g., "CSV", "JSON").
     */
    String getFormat();
}
