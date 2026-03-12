package com.trivine.llc.api.repository.projection;

public interface VerifyCounts {
    long getVerifiedCount();
    long getUnverifiedCount();
    long getTotalCount();
}