package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants.Patterns;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.InlineMarkdownParser;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import javax.annotation.Nullable;

/** Indexes literal ranges, paired extension delimiters and reference definitions before inline text is resolved. */
final class MarkdownBlockSource {
    final List<String> lines;
    final Map<String, String> references;
    private final Map<Integer, Fence> fences = new HashMap<>();
    private final Map<Integer, Extension> extensions = new HashMap<>();
    private final Set<Integer> extensionClosers = new HashSet<>();
    private final Set<Integer> definitions = new HashSet<>();

    MarkdownBlockSource(String body, Predicate<ResourceLocation> isInline) {
        lines = Arrays.asList(body.split("\n", -1));
        List<Integer> candidates = new ArrayList<>();
        ArrayDeque<OpenExtension> stack = new ArrayDeque<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (Patterns.INDENTED_CODE_PATTERN.matcher(line).matches()) continue;
            String trimmed = line.trim();

            Matcher fence = Patterns.CODE_FENCE_PATTERN.matcher(trimmed);
            if (fence.matches()) {
                int close = findFenceEnd(i + 1, fence.group(1));
                fences.put(i, new Fence(close, fence.group(2)));
                i = close;
                continue;
            }
            if (trimmed.equals(":::") || trimmed.startsWith("</")) {
                for (OpenExtension open : stack) {
                    if (open.block().matchesCloseLine(trimmed)) {
                        // An unclosed inner tag stays literal when an enclosing tag closes.
                        while (stack.pop() != open) {
                            // Discard only unmatched delimiters, not their source lines.
                        }
                        extensions.put(open.start(), new Extension(open.block(), i));
                        extensionClosers.add(i);
                        break;
                    }
                }
                continue;
            }

            SelfClosingBlockExtensionState tag = extensionTag(trimmed, isInline);
            if (tag instanceof BlockExtensionState block) {
                stack.push(new OpenExtension(i, block));
            } else if (tag != null) {
                extensions.put(i, new Extension(tag, i));
            } else {
                Matcher definition = Patterns.LINK_REF_DEF_PATTERN.matcher(line);
                if (definition.matches()) {
                    candidates.add(i);
                }
            }
        }
        // Unpaired openers have no range, so the block parser leaves them in the document as ordinary text.
        references = collectReferences(candidates, body.indexOf('`') >= 0);
    }

    private Map<String, String> collectReferences(List<Integer> candidates, boolean hasBackticks) {
        Map<String, String> links = new HashMap<>();
        int candidate = 0;
        while (candidate < candidates.size()) {
            int first = candidates.get(candidate);
            int start = first;
            int end = first + 1;
            if (hasBackticks) {
                while (start > 0 && !isParagraphBoundary(start - 1)) start--;
                while (end < lines.size() && !isParagraphBoundary(end)) end++;
            }
            String prose = text(start, end);
            BitSet code = new BitSet();
            if (prose.indexOf('`') >= 0) InlineMarkdownParser.forEachCodeSpan(prose, code::set);
            int offset = 0;
            for (int i = start; i < end; i++) {
                if (candidate < candidates.size() && i == candidates.get(candidate)) {
                    if (!code.get(offset)) {
                        Matcher definition = Patterns.LINK_REF_DEF_PATTERN.matcher(lines.get(i));
                        if (definition.matches()) {
                            definitions.add(i);
                            links.put(definition.group(1).toLowerCase(Locale.ROOT), definition.group(2));
                        }
                    }
                    candidate++;
                }
                offset += lines.get(i).length() + 1;
            }
        }
        return Map.copyOf(links);
    }

    /** Block syntax interrupts inline code spans; definition lines can belong to the surrounding prose. */
    private boolean isParagraphBoundary(int index) {
        String line = lines.get(index);
        return line.isBlank() || fences.containsKey(index) || extensions.containsKey(index) || extensionClosers.contains(index)
            || Patterns.CODE_FENCE_PATTERN.matcher(line.trim()).matches()
            || Patterns.HEADER_PATTERN.matcher(line).matches() || Patterns.SETEXT_H1_PATTERN.matcher(line).matches()
            || Patterns.SETEXT_H2_PATTERN.matcher(line).matches() || Patterns.HORIZONTAL_RULE_PATTERN.matcher(line).matches()
            || Patterns.BLOCKQUOTE_PATTERN.matcher(line).matches() || Patterns.TABLE_ROW_PATTERN.matcher(line).matches()
            || Patterns.UNORDERED_LIST_PATTERN.matcher(line).matches() || Patterns.ORDERED_LIST_PATTERN.matcher(line).matches()
            || Patterns.FALLBACK_IMAGE_PATTERN.matcher(line).matches() || Patterns.LEGACY_LATEX_LINE_PATTERN.matcher(line).matches();
    }

    private int findFenceEnd(int from, String marker) {
        while (from < lines.size()) {
            String line = lines.get(from);
            if (!Patterns.INDENTED_CODE_PATTERN.matcher(line).matches()) {
                String closer = line.trim();
                if (closer.length() >= marker.length() && closer.chars().allMatch(c -> c == marker.charAt(0))) {
                    break;
                }
            }
            from++;
        }
        return from;
    }

    private static @Nullable SelfClosingBlockExtensionState extensionTag(String line, Predicate<ResourceLocation> isInline) {
        if (line.startsWith(":::")) {
            Matcher matcher = Patterns.EXTENSION_COLON_OPEN_PATTERN.matcher(line);
            if (!matcher.matches()) return null;
            ResourceLocation id = extensionId(matcher.group(1));
            return id == null ? null : BlockExtensionState.colon(id, rawParams(matcher));
        }
        if (!line.startsWith("<") || line.startsWith("</")) return null;
        Matcher matcher = Patterns.EXTENSION_TAG_OPEN_PATTERN.matcher(line);
        if (!matcher.matches()) return null;
        ResourceLocation id = extensionId(matcher.group(1));
        if (id == null || isInline.test(id)) return null;

        String raw = rawParams(matcher);
        Map<String, String> params = ExtensionParamParser.parse(raw);
        if ("/".equals(matcher.group(3)) || id.toString().equals("ageratum:directory")) {
            return new SelfClosingBlockExtensionState(id, raw, params);
        }
        return BlockExtensionState.tag(id, raw, params);
    }

    private static String rawParams(Matcher matcher) {
        String params = matcher.group(2);
        return params == null ? "" : params.trim();
    }

    private static @Nullable ResourceLocation extensionId(String name) {
        return ResourceLocation.tryParse(name.contains(":") ? name : "ageratum:" + name);
    }

    String text(int start, int end) {
        return String.join("\n", lines.subList(start, end));
    }

    @Nullable Fence fence(int line) {
        return fences.get(line);
    }

    @Nullable Extension extension(int line) {
        return extensions.get(line);
    }

    boolean isDefinition(int line) {
        return definitions.contains(line);
    }

    record Fence(int close, String info) {
    }

    record Extension(SelfClosingBlockExtensionState block, int close) {
    }

    private record OpenExtension(int start, BlockExtensionState block) {
    }
}
