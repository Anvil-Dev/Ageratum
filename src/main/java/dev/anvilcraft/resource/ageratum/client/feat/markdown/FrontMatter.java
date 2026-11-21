package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/** The handbook's YAML front matter subset and its normalized Markdown body. */
record FrontMatter(Map<String, Object> values, String body) {
    static FrontMatter read(String markdown) {
        String normalized = markdown.replace("\r\n", "\n").replace('\r', '\n');
        int openingEnd = normalized.indexOf('\n');
        if (openingEnd < 0 || !normalized.substring(0, openingEnd).trim().equals("---")) {
            return new FrontMatter(Map.of(), normalized);
        }
        for (int start = openingEnd + 1; start <= normalized.length();) {
            int newline = normalized.indexOf('\n', start);
            int end = newline < 0 ? normalized.length() : newline;
            if (normalized.substring(start, end).trim().equals("---")) {
                String yaml = normalized.substring(openingEnd + 1, start);
                List<String> lines = Arrays.asList(yaml.split("\n", -1));
                String body = newline < 0 ? "" : normalized.substring(newline + 1);
                return new FrontMatter(parseYamlMap(lines, 0, 0).map(), body);
            }
            if (newline < 0) break;
            start = newline + 1;
        }
        return new FrontMatter(Map.of(), normalized);
    }

    private static int countYamlIndent(String line) {
        int width = 0;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == ' ') {
                width++;
                continue;
            }
            if (ch == '\t') {
                width += 2;
                continue;
            }
            break;
        }
        return width;
    }

    private static @Nullable Object parseYamlScalar(String valuePart) {
        String value = valuePart.trim();
        List<Object> inlineList = tryParseInlineYamlList(value);
        if (inlineList != null) {
            return inlineList;
        }
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        if ("null".equalsIgnoreCase(value) || "~".equals(value)) {
            return null;
        }
        try {
            if (value.contains(".") || value.contains("e") || value.contains("E")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private static ParseYamlMapResult parseYamlMap(List<String> yamlLines, int startIndex, int expectedIndent) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        int index = startIndex;
        while (index < yamlLines.size()) {
            String line = yamlLines.get(index);
            if (line == null || line.isBlank() || line.trim().startsWith("#")) {
                index++;
                continue;
            }

            int indent = countYamlIndent(line);
            if (indent < expectedIndent) {
                break;
            }
            if (indent > expectedIndent) {
                index++;
                continue;
            }

            String trimmed = line.trim();
            if (trimmed.startsWith("- ")) {
                break;
            }

            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
                index++;
                continue;
            }

            String key = trimmed.substring(0, colon).trim();
            if (key.isEmpty()) {
                index++;
                continue;
            }

            String valuePart = trimmed.substring(colon + 1).trim();
            if (!valuePart.isEmpty()) {
                result.put(key, parseYamlScalar(valuePart));
                index++;
                continue;
            }

            int nextIndex = skipYamlIgnorableLines(yamlLines, index + 1);
            if (nextIndex >= yamlLines.size()) {
                result.put(key, new LinkedHashMap<String, Object>());
                index++;
                continue;
            }

            int childIndent = countYamlIndent(yamlLines.get(nextIndex));
            if (childIndent <= indent) {
                result.put(key, new LinkedHashMap<String, Object>());
                index++;
                continue;
            }

            String childTrimmed = yamlLines.get(nextIndex).trim();
            if (childTrimmed.startsWith("- ")) {
                ParseYamlListResult listResult = parseYamlList(yamlLines, nextIndex, childIndent);
                result.put(key, listResult.list());
                index = listResult.nextIndex();
                continue;
            }

            ParseYamlMapResult mapResult = parseYamlMap(yamlLines, nextIndex, childIndent);
            result.put(key, mapResult.map());
            index = mapResult.nextIndex();
        }
        return new ParseYamlMapResult(Collections.unmodifiableMap(result), index);
    }

    private static ParseYamlListResult parseYamlList(List<String> yamlLines, int startIndex, int expectedIndent) {
        List<Object> result = new ArrayList<>();
        int index = startIndex;
        while (index < yamlLines.size()) {
            String line = yamlLines.get(index);
            if (line == null || line.isBlank() || line.trim().startsWith("#")) {
                index++;
                continue;
            }

            int indent = countYamlIndent(line);
            if (indent < expectedIndent) {
                break;
            }
            if (indent != expectedIndent) {
                index++;
                continue;
            }

            String trimmed = line.trim();
            if (!trimmed.startsWith("- ")) {
                break;
            }

            String valuePart = trimmed.substring(2).trim();
            if (!valuePart.isEmpty()) {
                result.add(parseYamlScalar(valuePart));
                index++;
                continue;
            }

            int nextIndex = skipYamlIgnorableLines(yamlLines, index + 1);
            if (nextIndex >= yamlLines.size()) {
                result.add("");
                index++;
                continue;
            }

            int childIndent = countYamlIndent(yamlLines.get(nextIndex));
            if (childIndent <= indent) {
                result.add("");
                index++;
                continue;
            }

            String childTrimmed = yamlLines.get(nextIndex).trim();
            if (childTrimmed.startsWith("- ")) {
                ParseYamlListResult listResult = parseYamlList(yamlLines, nextIndex, childIndent);
                result.add(listResult.list());
                index = listResult.nextIndex();
                continue;
            }

            ParseYamlMapResult mapResult = parseYamlMap(yamlLines, nextIndex, childIndent);
            result.add(mapResult.map());
            index = mapResult.nextIndex();
        }
        return new ParseYamlListResult(Collections.unmodifiableList(result), index);
    }

    private static int skipYamlIgnorableLines(List<String> yamlLines, int index) {
        int current = index;
        while (current < yamlLines.size()) {
            String line = yamlLines.get(current);
            if (line != null && !line.isBlank() && !line.trim().startsWith("#")) {
                break;
            }
            current++;
        }
        return current;
    }

    private static @Nullable List<Object> tryParseInlineYamlList(String value) {
        if (!value.startsWith("[") || !value.endsWith("]")) {
            return null;
        }

        String inner = value.substring(1, value.length() - 1).trim();
        if (inner.isEmpty()) {
            return List.of();
        }

        List<String> tokens = splitInlineYamlList(inner);
        if (tokens.isEmpty()) {
            return List.of();
        }

        List<Object> values = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            values.add(parseYamlScalar(token));
        }
        return Collections.unmodifiableList(values);
    }

    private static List<String> splitInlineYamlList(String value) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceDepth = 0;
        int bracketDepth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                current.append(ch);
                continue;
            }
            if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                current.append(ch);
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote) {
                if (ch == '{') {
                    braceDepth++;
                } else if (ch == '}') {
                    braceDepth = Math.max(0, braceDepth - 1);
                } else if (ch == '[') {
                    bracketDepth++;
                } else if (ch == ']') {
                    bracketDepth = Math.max(0, bracketDepth - 1);
                } else if (ch == ',' && braceDepth == 0 && bracketDepth == 0) {
                    String token = current.toString().trim();
                    if (!token.isEmpty()) {
                        parts.add(token);
                    }
                    current.setLength(0);
                    continue;
                }
            }
            current.append(ch);
        }

        String token = current.toString().trim();
        if (!token.isEmpty()) {
            parts.add(token);
        }
        return parts;
    }

    private record ParseYamlMapResult(Map<String, Object> map, int nextIndex) {
    }

    private record ParseYamlListResult(List<Object> list, int nextIndex) {
    }
}
