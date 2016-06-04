package com.bkromhout.minerva.events;

import com.bkromhout.minerva.R;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests the {@link MissingPermEvent} class.
 */
public class MissingPermEventTest {
    /**
     * Event to test with.
     */
    private static MissingPermEvent event;

    @BeforeClass
    public static void setUp() {
        // Create events using constructors.
        event = new MissingPermEvent("storage", R.id.action_read);
    }

    @AfterClass
    public static void tearDown() {
        event = null;
    }

    @Test
    public void test() {
        assertThat(event.getPermission(), is("storage"));
        assertThat(event.getActionId(), is(R.id.action_read));
    }
}
