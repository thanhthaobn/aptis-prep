package com.aptisprep.mvc.service.catalog;

import java.util.Map;
import java.util.Set;

/** Scoring-scheme usage service contract. */
public interface ScoringSchemeUsageService {
    public boolean isUsed(Integer schemeId);

    public Map<Integer, Set<String>> examTypesByScheme();
}
