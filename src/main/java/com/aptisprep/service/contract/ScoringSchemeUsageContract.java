package com.aptisprep.service.contract;

/**
 * Owner 3 supplies the usage signal so scoring history cannot be edited in place.
 * Owner 1 never reads mock-exam repositories directly.
 */
public interface ScoringSchemeUsageContract {
    boolean isUsed(Integer schemeId);
}
