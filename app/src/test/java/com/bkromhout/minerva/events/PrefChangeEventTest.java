package com.bkromhout.minerva.events;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests the {@link PrefChangeEvent} class.
 */
public class PrefChangeEventTest {
    /**
     * Event to test with.
     */
    private static PrefChangeEvent event;

    @BeforeClass
    public static void setUp() {
        // Create events using constructors.
        event = new PrefChangeEvent("PREF_KEY");
    }

    @AfterClass
    public static void tearDown() {
        event = null;
    }

    @Test
    public void test() {
        assertThat(event.getPrefName(), is("PREF_KEY"));
    }
}
