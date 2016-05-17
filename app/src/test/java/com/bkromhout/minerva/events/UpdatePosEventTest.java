package com.bkromhout.minerva.events;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests the {@link UpdatePosEvent} class.
 */
public class UpdatePosEventTest {
    /**
     * Event to test with.
     */
    private static UpdatePosEvent event;

    @BeforeClass
    public static void setUp() {
        // Create events using constructors.
        event = new UpdatePosEvent(5);
    }

    @AfterClass
    public static void tearDown() {
        event = null;
    }

    @Test
    public void test() {
        assertThat(event.getPosition(), is(5));
    }
}
