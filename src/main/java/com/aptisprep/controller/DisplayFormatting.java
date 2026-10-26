package com.aptisprep.controller;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Presentation formatting shared by HTML pages; does not query or modify business data. */
@Component
public class DisplayFormatting {
    public String date(TemporalAccessor value) {
        return format(value, "dd/MM/yyyy");
    }

    public String dateTime(TemporalAccessor value) {
        return format(value, "dd/MM/yyyy HH:mm");
    }

    public String dateTimeWithSeconds(TemporalAccessor value) {
        return format(value, "dd/MM/yyyy HH:mm:ss");
    }

    public String timeAndDate(TemporalAccessor value) {
        return format(value, "HH:mm dd/MM/yyyy");
    }

    private String format(TemporalAccessor value, String pattern) {
        if (value == null) return null;
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
                        .withZone(ZoneId.systemDefault());
        return formatter.format(value);
    }

    public List<Integer> pageNumbers(int start, int end) {
        List<Integer> result = new ArrayList<>();
        int step = start <= end ? 1 : -1;
        for (int number = start; ; number += step) {
            result.add(number);
            if (number == end) return result;
        }
    }

    public String join(Iterable<String> values, String separator) {
        return values == null ? null : String.join(separator, values);
    }
}
