package com.bkromhout.minerva.events;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests the {@link BookCardClickEvent} class.
 */
public class BookCardClickEventTest {
    /**
     * Event to test with.
     */
    private static BookCardClickEvent event;

    @BeforeClass
    public static void setUp() {
        // Create events using constructors.
        event = new BookCardClickEvent(BookCardClickEvent.Type.NORMAL, "/test/path", 5, vh.getLayoutPosition());
    }

    @AfterClass
    public static void tearDown() {
        event = null;
    }

    @Test
    public void test() {
        assertThat(event.getType(), is(BookCardClickEvent.Type.NORMAL));
        assertThat(event.getRelPath(), is("/test/path"));
        assertThat(event.getAdapterPosition(), is(5));
    }
}
