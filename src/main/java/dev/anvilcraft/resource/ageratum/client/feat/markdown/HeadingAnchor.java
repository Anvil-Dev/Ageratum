package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

/** The common anchor spelling used by guide links and their destination headings. */
public final class HeadingAnchor {
    private static final String ORDINAL_PREFIX = "~ageratum/heading/";

    public static String forOrdinal(int ordinal) {
        return ORDINAL_PREFIX + ordinal;
    }

    /** A positive source-heading ordinal or zero for an ordinary author-written fragment. */
    public static int ordinal(String fragment) {
        if (!fragment.startsWith(ORDINAL_PREFIX)) return 0;
        try {
            return Math.max(0, Integer.parseInt(fragment.substring(ORDINAL_PREFIX.length())));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private HeadingAnchor() {
    }

    public static String fromTitle(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder anchor = new StringBuilder();
        boolean separator = false;
        for (int offset = 0; offset < normalized.length();) {
            int ch = normalized.codePointAt(offset);
            offset += Character.charCount(ch);
            if (Character.isLetterOrDigit(ch)) {
                if (separator && !anchor.isEmpty()) anchor.append('-');
                anchor.appendCodePoint(ch);
                separator = false;
            } else if (Character.isWhitespace(ch) || ch == '-' || ch == '_') {
                separator = true;
            }
        }
        return anchor.toString();
    }

    public static String fromFragment(String fragment) {
        try {
            return fromTitle(URLDecoder.decode(fragment, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException ignored) {
            return fromTitle(fragment);
        }
    }
}
