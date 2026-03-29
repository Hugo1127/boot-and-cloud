package com.bootcloud.concurrent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LockComparatorTest {

    @Test
    public void testCompareLocks() {
        LockComparator comparator = new LockComparator();
        assertDoesNotThrow(() -> comparator.compareLocks(10000));
    }

    @Test
    public void testDemonstrateLockUpgrade() {
        LockComparator comparator = new LockComparator();
        assertDoesNotThrow(() -> comparator.demonstrateLockUpgrade());
    }
}
