/*
 Copyright 2019 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.skyline.migration.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites a serialized XML document as minimal text edits on the original file, so that comments, attribute order,
 * entity encodings and indentation of everything the migration did not change are kept.
 * <p>
 * The edited text is only used when it is canonically equal to the serialized document (same elements, attributes
 * and text, ignoring formatting, comments, attribute order and namespace declarations). Otherwise the serialized
 * document is used as is.
 */
final class XmlTextDiff {

    private static final Pattern ENTITY = Pattern.compile("&(#[xX][0-9a-fA-F]+|#[0-9]+|amp|lt|gt|quot|apos);");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private XmlTextDiff() {
    }

    /**
     * @return the original text with the minimal edits that turn it into the serialized document, or the serialized
     * document if that is not possible
     */
    static String minimize(String original, String serialized) {
        try {
            El a = rootElement(parse(original));
            El b = rootElement(parse(serialized));
            if (a == null || b == null || !a.name.equals(b.name)) {
                return serialized;
            }
            if (a.hash().equals(b.hash())) {
                return original;
            }
            List<Edit> edits = new ArrayList<>();
            new Differ(original, serialized, edits).diff(a, b);
            String result = applyEdits(original, edits);
            El check = rootElement(parse(result));
            return check != null && check.hash().equals(b.hash()) ? result : serialized;
        } catch (RuntimeException e) {
            return serialized;
        }
    }

    // ── Parser ─────────────────────────────────────────────────────────

    private static final class Attr {
        String name;
        String value;
        int start;
        int end;
    }

    private static final class El {
        String name;
        El parent;
        boolean document;
        final List<Attr> attrs = new ArrayList<>();
        final List<El> children = new ArrayList<>();
        final StringBuilder text = new StringBuilder();
        int start;
        int end;
        int nameEnd;
        int tagCloseStart;
        int startTagEnd;
        int endTagStart;
        boolean selfClosing;
        private String hash;

        Attr attr(String attrName) {
            for (Attr attr : attrs) {
                if (attr.name.equals(attrName)) {
                    return attr;
                }
            }
            return null;
        }

        String normText() {
            return WHITESPACE.matcher(text).replaceAll(" ").trim();
        }

        String hash() {
            if (hash == null) {
                List<Attr> sorted = new ArrayList<>();
                for (Attr attr : attrs) {
                    if (!isNs(attr.name)) {
                        sorted.add(attr);
                    }
                }
                sorted.sort(Comparator.comparing(attr -> attr.name));
                StringBuilder canonical = new StringBuilder();
                append(canonical, name);
                for (Attr attr : sorted) {
                    append(canonical, attr.name);
                    append(canonical, attr.value);
                }
                append(canonical, normText());
                for (El child : children) {
                    append(canonical, child.hash());
                }
                hash = sha1(canonical.toString());
            }
            return hash;
        }

        private static void append(StringBuilder canonical, String part) {
            canonical.append(part.length()).append(':').append(part).append(';');
        }
    }

    private static El parse(String text) {
        El doc = new El();
        doc.document = true;
        doc.name = "#document";
        Deque<El> stack = new ArrayDeque<>();
        stack.push(doc);
        int n = text.length();
        int i = 0;
        while (i < n) {
            int lt = text.indexOf('<', i);
            if (lt == -1) {
                stack.peek().text.append(decodeEntities(text.substring(i)));
                break;
            }
            stack.peek().text.append(decodeEntities(text.substring(i, lt)));
            if (text.startsWith("<!--", lt)) {
                i = skipPast(text, "-->", lt + 4);
            } else if (text.startsWith("<![CDATA[", lt)) {
                int end = text.indexOf("]]>", lt + 9);
                if (end < 0) {
                    throw new IllegalArgumentException("Unterminated CDATA at " + lt);
                }
                stack.peek().text.append(text, lt + 9, end);
                i = end + 3;
            } else if (text.startsWith("<?", lt)) {
                i = skipPast(text, "?>", lt + 2);
            } else if (text.startsWith("<!", lt)) {
                i = skipDeclaration(text, lt);
            } else if (lt + 1 < n && text.charAt(lt + 1) == '/') {
                int gt = text.indexOf('>', lt);
                if (gt < 0) {
                    throw new IllegalArgumentException("Unterminated end tag at " + lt);
                }
                String name = text.substring(lt + 2, gt).trim();
                El el = stack.pop();
                if (el == doc || !el.name.equals(name)) {
                    throw new IllegalArgumentException("Mismatched </" + name + "> at " + lt);
                }
                el.endTagStart = lt;
                el.end = gt + 1;
                i = gt + 1;
            } else {
                i = parseStartTag(text, lt, stack);
            }
        }
        if (stack.size() != 1) {
            throw new IllegalArgumentException("Unclosed <" + stack.peek().name + ">");
        }
        return doc;
    }

    private static int skipPast(String text, String terminator, int from) {
        int end = text.indexOf(terminator, from);
        if (end < 0) {
            throw new IllegalArgumentException("Unterminated markup at " + from);
        }
        return end + terminator.length();
    }

    private static int skipDeclaration(String text, int lt) {
        int depth = 0;
        for (int j = lt + 2; j < text.length(); j++) {
            char c = text.charAt(j);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
            } else if (c == '>' && depth <= 0) {
                return j + 1;
            }
        }
        throw new IllegalArgumentException("Unterminated declaration at " + lt);
    }

    private static int parseStartTag(String text, int lt, Deque<El> stack) {
        int n = text.length();
        int j = lt + 1;
        while (j < n && !isWs(text.charAt(j)) && text.charAt(j) != '>' && text.charAt(j) != '/') {
            j++;
        }
        El el = new El();
        el.name = text.substring(lt + 1, j);
        if (el.name.isEmpty()) {
            throw new IllegalArgumentException("Empty element name at " + lt);
        }
        el.start = lt;
        el.nameEnd = j;
        el.parent = stack.peek();
        while (true) {
            while (j < n && isWs(text.charAt(j))) {
                j++;
            }
            if (j >= n) {
                throw new IllegalArgumentException("Unterminated <" + el.name + ">");
            }
            if (text.startsWith("/>", j)) {
                el.selfClosing = true;
                el.tagCloseStart = j;
                el.startTagEnd = j + 2;
                el.end = j + 2;
                el.parent.children.add(el);
                return j + 2;
            }
            if (text.charAt(j) == '>') {
                el.tagCloseStart = j;
                el.startTagEnd = j + 1;
                el.parent.children.add(el);
                stack.push(el);
                return j + 1;
            }
            Attr attr = new Attr();
            attr.start = j;
            while (j < n && !isWs(text.charAt(j)) && "=>/".indexOf(text.charAt(j)) < 0) {
                j++;
            }
            attr.name = text.substring(attr.start, j);
            while (j < n && isWs(text.charAt(j))) {
                j++;
            }
            if (j >= n || text.charAt(j) != '=') {
                throw new IllegalArgumentException("Attribute " + attr.name + " without value");
            }
            j++;
            while (j < n && isWs(text.charAt(j))) {
                j++;
            }
            char quote = j < n ? text.charAt(j) : 0;
            if (quote != '"' && quote != '\'') {
                throw new IllegalArgumentException("Unquoted attribute " + attr.name);
            }
            int valueEnd = text.indexOf(quote, j + 1);
            if (valueEnd < 0) {
                throw new IllegalArgumentException("Unterminated attribute " + attr.name);
            }
            // Attribute value normalisation: literal line breaks and tabs become spaces
            attr.value = decodeEntities(text.substring(j + 1, valueEnd).replace("\r\n", "\n").replaceAll("[\t\n\r]", " "));
            attr.end = valueEnd + 1;
            el.attrs.add(attr);
            j = valueEnd + 1;
        }
    }

    private static El rootElement(El doc) {
        return doc.children.isEmpty() ? null : doc.children.get(0);
    }

    private static String decodeEntities(String s) {
        if (s.indexOf('&') < 0) {
            return s;
        }
        Matcher m = ENTITY.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String e = m.group(1);
            String replacement;
            switch (e) {
                case "amp": replacement = "&"; break;
                case "lt": replacement = "<"; break;
                case "gt": replacement = ">"; break;
                case "quot": replacement = "\""; break;
                case "apos": replacement = "'"; break;
                default:
                    int cp = e.charAt(1) == 'x' || e.charAt(1) == 'X' ? Integer.parseInt(e.substring(2), 16) : Integer.parseInt(e.substring(1));
                    replacement = new String(Character.toChars(cp));
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static boolean isWs(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private static boolean isNs(String name) {
        return name.equals("xmlns") || name.startsWith("xmlns:");
    }

    private static String sha1(String s) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // ── Indentation ────────────────────────────────────────────────────

    // Whitespace between the start of the line and pos, or "" when something else precedes pos on that line
    private static String lineIndent(String text, int pos) {
        int s = pos;
        while (s > 0 && text.charAt(s - 1) != '\n') {
            s--;
        }
        String prefix = text.substring(s, pos);
        return prefix.matches("[ \t]*") ? prefix : "";
    }

    private static String childIndent(String text, El parent) {
        for (El child : parent.children) {
            String indent = lineIndent(text, child.start);
            if (!indent.isEmpty()) {
                return indent;
            }
        }
        return (parent.document ? "" : lineIndent(text, parent.start)) + "    ";
    }

    private static String reindent(String raw, String from, String to) {
        if (from.isEmpty() && !raw.contains("\n")) {
            return raw;
        }
        String[] lines = raw.split("\n", -1);
        for (int k = 1; k < lines.length; k++) {
            if (!from.isEmpty() && lines[k].startsWith(from)) {
                lines[k] = to + lines[k].substring(from.length());
            }
        }
        return String.join("\n", lines);
    }

    private static int wsBefore(String text, int pos) {
        int s = pos;
        while (s > 0 && isWs(text.charAt(s - 1))) {
            s--;
        }
        return s;
    }

    // ── Edits ──────────────────────────────────────────────────────────

    private static final class Edit {
        final int start;
        final int end;
        final String text;
        final int seq;

        Edit(int start, int end, String text, int seq) {
            this.start = start;
            this.end = end;
            this.text = text;
            this.seq = seq;
        }

        boolean isInsert() {
            return start == end;
        }
    }

    private static String applyEdits(String text, List<Edit> edits) {
        List<Edit> ranges = new ArrayList<>();
        for (Edit edit : edits) {
            if (!edit.isInsert()) {
                ranges.add(edit);
            }
        }
        ranges.sort(Comparator.comparingInt(edit -> edit.start));
        for (int k = 1; k < ranges.size(); k++) {
            if (ranges.get(k).start < ranges.get(k - 1).end) {
                throw new IllegalStateException("Overlapping edits at " + ranges.get(k).start);
            }
        }
        for (Edit insert : edits) {
            for (Edit range : ranges) {
                if (insert.isInsert() && insert.start > range.start && insert.start < range.end) {
                    throw new IllegalStateException("Insert inside edited range at " + insert.start);
                }
            }
        }

        // Back to front. At an equal start, replacements go before inserts so inserts land at the replaced range's
        // start; inserts at the same position are applied last first to keep their order.
        List<Edit> ordered = new ArrayList<>(edits);
        ordered.sort(Comparator.comparingInt((Edit edit) -> -edit.start)
                .thenComparingInt(edit -> edit.isInsert() ? 1 : 0)
                .thenComparingInt(edit -> -edit.seq));
        boolean crlf = text.contains("\r\n");
        StringBuilder out = new StringBuilder(text);
        for (Edit edit : ordered) {
            String replacement = crlf ? edit.text.replace("\r\n", "\n").replace("\n", "\r\n") : edit.text;
            out.replace(edit.start, edit.end, replacement);
        }
        return out.toString();
    }

    private static final class Differ {
        private final String orig;
        private final String out;
        private final List<Edit> edits;

        Differ(String orig, String out, List<Edit> edits) {
            this.orig = orig;
            this.out = out;
            this.edits = edits;
        }

        private void edit(int start, int end, String text) {
            edits.add(new Edit(start, end, text, edits.size()));
        }

        void diff(El ea, El eb) {
            diffAttributes(ea, eb);

            List<El> a = ea.children;
            List<El> b = eb.children;
            boolean sameText = ea.normText().equals(eb.normText());
            if (a.isEmpty() && b.isEmpty()) {
                if (!sameText) {
                    String inner = eb.selfClosing ? "" : out.substring(eb.startTagEnd, eb.endTagStart);
                    if (ea.selfClosing) {
                        edit(ea.tagCloseStart, ea.startTagEnd, ">" + inner + "</" + ea.name + ">");
                    } else {
                        edit(ea.startTagEnd, ea.endTagStart, inner);
                    }
                }
                return;
            }
            if (!sameText) {
                throw new IllegalStateException("Mixed content change in <" + ea.name + ">");
            }

            Map<Integer, El> counterpart = new HashMap<>();
            Set<Integer> removed = new LinkedHashSet<>();
            List<int[]> bounds = new ArrayList<>();
            bounds.add(new int[]{-1, -1});
            for (int[] pair : lcsPairs(a, b)) {
                counterpart.put(pair[1], a.get(pair[0]));
                bounds.add(pair);
            }
            bounds.add(new int[]{a.size(), b.size()});

            // Between two unchanged children, children with the same name are diffed recursively
            for (int k = 0; k < bounds.size() - 1; k++) {
                int ax = bounds.get(k)[0];
                int by = bounds.get(k)[1];
                int ax2 = bounds.get(k + 1)[0];
                int by2 = bounds.get(k + 1)[1];
                int cursor = ax + 1;
                for (int y = by + 1; y < by2; y++) {
                    int found = -1;
                    for (int x = cursor; x < ax2; x++) {
                        if (a.get(x).name.equals(b.get(y).name)) {
                            found = x;
                            break;
                        }
                    }
                    if (found >= 0) {
                        for (int x = cursor; x < found; x++) {
                            removed.add(x);
                        }
                        counterpart.put(y, a.get(found));
                        diff(a.get(found), b.get(y));
                        cursor = found + 1;
                    }
                }
                for (int x = cursor; x < ax2; x++) {
                    removed.add(x);
                }
            }

            // A removed child that comes back elsewhere unchanged is re-inserted with its original text
            Map<String, Deque<El>> movable = new HashMap<>();
            for (int x : removed) {
                El child = a.get(x);
                edit(wsBefore(orig, child.start), child.end, "");
                movable.computeIfAbsent(child.hash(), h -> new ArrayDeque<>()).add(child);
            }

            String indent = childIndent(orig, ea);
            StringBuilder selfClosingBody = new StringBuilder();
            El prev = null;
            for (int y = 0; y < b.size(); y++) {
                if (counterpart.containsKey(y)) {
                    prev = counterpart.get(y);
                    continue;
                }
                El inserted = b.get(y);
                Deque<El> moved = movable.get(inserted.hash());
                String body;
                if (moved != null && !moved.isEmpty()) {
                    El original = moved.poll();
                    body = "\n" + indent + reindent(orig.substring(original.start, original.end), lineIndent(orig, original.start), indent);
                } else {
                    body = "\n" + indent + reindent(out.substring(inserted.start, inserted.end), lineIndent(out, inserted.start), indent);
                }
                if (ea.selfClosing) {
                    selfClosingBody.append(body);
                } else {
                    int pos = prev != null ? prev.end : ea.startTagEnd;
                    edit(pos, pos, body);
                }
            }
            if (selfClosingBody.length() > 0) {
                edit(ea.tagCloseStart, ea.startTagEnd, ">" + selfClosingBody + "\n" + lineIndent(orig, ea.start) + "</" + ea.name + ">");
            }
        }

        private void diffAttributes(El ea, El eb) {
            for (Attr ab : eb.attrs) {
                Attr aa = ea.attr(ab.name);
                String raw = out.substring(ab.start, ab.end);
                if (aa == null) {
                    Attr lastNs = null;
                    for (Attr attr : ea.attrs) {
                        if (isNs(attr.name)) {
                            lastNs = attr;
                        }
                    }
                    if (isNs(ab.name) && lastNs != null) {
                        edit(lastNs.end, lastNs.end, " " + raw);
                    } else if (!ea.attrs.isEmpty()) {
                        Attr last = ea.attrs.get(ea.attrs.size() - 1);
                        String indent = lineIndent(orig, last.start);
                        edit(last.end, last.end, (indent.isEmpty() ? " " : "\n" + indent) + raw);
                    } else {
                        edit(ea.nameEnd, ea.nameEnd, " " + raw);
                    }
                } else if (!isNs(ab.name) && !aa.value.equals(ab.value)) {
                    edit(aa.start, aa.end, raw);
                }
            }
            for (Attr aa : ea.attrs) {
                if (!isNs(aa.name) && eb.attr(aa.name) == null) {
                    edit(wsBefore(orig, aa.start), aa.end, "");
                }
            }
        }

        private List<int[]> lcsPairs(List<El> a, List<El> b) {
            int n = a.size();
            int m = b.size();
            int[][] dp = new int[n + 1][m + 1];
            for (int x = n - 1; x >= 0; x--) {
                for (int y = m - 1; y >= 0; y--) {
                    dp[x][y] = a.get(x).hash().equals(b.get(y).hash()) ? dp[x + 1][y + 1] + 1 : Math.max(dp[x + 1][y], dp[x][y + 1]);
                }
            }
            List<int[]> pairs = new ArrayList<>();
            int x = 0;
            int y = 0;
            while (x < n && y < m) {
                if (a.get(x).hash().equals(b.get(y).hash())) {
                    pairs.add(new int[]{x, y});
                    x++;
                    y++;
                } else if (dp[x + 1][y] >= dp[x][y + 1]) {
                    x++;
                } else {
                    y++;
                }
            }
            return pairs;
        }
    }
}
