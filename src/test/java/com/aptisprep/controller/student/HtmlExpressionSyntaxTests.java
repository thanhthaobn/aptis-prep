package com.aptisprep.controller.student;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.web.util.HtmlUtils;

class HtmlExpressionSyntaxTests {
    @Test
    void lessonSectionsHaveUniqueLiteralIds() throws Exception {
        String source =
                Files.readString(
                        Path.of("src/main/resources/templates/student/lesson/detail.html"));
        Set<String> ids = new HashSet<>();
        Matcher matcher = Pattern.compile("(?<![\\w:-])id=\"([^\"]+)\"").matcher(source);
        while (matcher.find()) {
            assertTrue(ids.add(matcher.group(1)), "Duplicate id: " + matcher.group(1));
        }
    }

    @Test
    void everyHtmlVariableAndSelectionExpressionHasValidSyntax() throws Exception {
        SpelExpressionParser parser = new SpelExpressionParser();
        try (Stream<Path> paths = Files.walk(Path.of("src/main/resources/templates"))) {
            List<Path> templates = paths.filter(path -> path.toString().endsWith(".html")).toList();
            assertFalse(templates.isEmpty());
            for (Path template : templates) {
                String source = HtmlUtils.htmlUnescape(Files.readString(template));
                for (int index = 0; index + 1 < source.length(); index++) {
                    char prefix = source.charAt(index);
                    if ((prefix != '$' && prefix != '*') || source.charAt(index + 1) != '{')
                        continue;
                    int end = expressionEnd(source, index + 2);
                    String expression = source.substring(index + 2, end);
                    assertDoesNotThrow(
                            () -> parser.parseExpression(expression), template + ": " + expression);
                    index = end;
                }
            }
        }
    }

    private int expressionEnd(String source, int start) {
        int depth = 1;
        char quote = 0;
        for (int index = start; index < source.length(); index++) {
            char current = source.charAt(index);
            if (quote != 0) {
                if (current == quote) {
                    if (index + 1 < source.length() && source.charAt(index + 1) == quote) index++;
                    else quote = 0;
                }
            } else if (current == '\'' || current == '"') quote = current;
            else if (current == '{') depth++;
            else if (current == '}' && --depth == 0) return index;
        }
        throw new AssertionError("Unclosed expression at " + start);
    }
}
