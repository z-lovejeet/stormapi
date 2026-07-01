package com.stormapi.scenario.execution;

import com.stormapi.collection.dto.KeyValuePairDto;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Replaces {@code {{variableName}}} placeholders in scenario step templates
 * with values from the variable store.
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>Matches {@code \{\{(\w+)\}\}} — double curly braces with word characters</li>
 *   <li>If variable exists in store → replaced with value</li>
 *   <li>If variable NOT in store → left as-is ({@code {{varName}}} literal)</li>
 *   <li>Multiple occurrences of the same variable → all replaced</li>
 *   <li>Null/empty template → returned as-is</li>
 * </ul>
 *
 * <p>Thread-safe: stateless, compiled pattern is a {@code static final}.</p>
 */
public class TemplateResolver {

    /**
     * Compiled regex for {{variableName}} placeholders.
     * Group 1 captures the variable name (word characters only).
     */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    /**
     * Resolves all {@code {{variable}}} placeholders in a template string.
     *
     * @param template  the string containing placeholders (may be null)
     * @param variables the variable store (variableName → value)
     * @return resolved string with placeholders replaced
     */
    public String resolve(String template, Map<String, String> variables) {
        if (template == null || template.isEmpty() || variables == null || variables.isEmpty()) {
            return template;
        }

        Matcher matcher = PLACEHOLDER.matcher(template);
        return matcher.replaceAll(match -> {
            String varName = match.group(1);
            String value = variables.get(varName);
            // If not found, leave placeholder as-is
            return value != null ? Matcher.quoteReplacement(value) : match.group();
        });
    }

    /**
     * Resolves placeholders in header values (not keys).
     *
     * @param headers   list of key-value pair DTOs
     * @param variables the variable store
     * @return new map with resolved header values
     */
    public Map<String, String> resolveHeaders(List<KeyValuePairDto> headers,
                                               Map<String, String> variables) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }

        return headers.stream()
                .collect(Collectors.toMap(
                        KeyValuePairDto::key,
                        h -> resolve(h.value(), variables),
                        (existing, replacement) -> replacement
                ));
    }

}
