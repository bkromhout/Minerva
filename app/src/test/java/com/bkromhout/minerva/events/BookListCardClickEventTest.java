package com.bkromhout.minerva.events;

import com.bkromhout.minerva.R;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests the {@link BookListCardClickEvent} class.
 */
public class BookListCardClickEventTest {
    /**
     * Created using {@link BookListCardClickEvent#BookListCardClickEvent(BookListCardClickEvent.Type, String, int)}.
     */
    private static BookListCardClickEvent event1;
    /**
     * Created using {@link BookListCardClickEvent#BookListCardClickEvent(BookListCardClickEvent.Type, String, int,
     * int)}.
     */
    private static BookListCardClickEvent event2;

    @BeforeClass
    public static void setUp() {
        // Create events using constructors.
        event1 = new BookListCardClickEvent(BookListCardClickEvent.Type.NORMAL, "List 1", 5);
        event2 = new BookListCardClickEvent(BookListCardClickEvent.Type.LONG, "List 2", R.id.action_clear, 6);
    }

    @AfterClass
    public static void tearDown() {
        event1 = null;
        event2 = null;
    }

    @Test
    public void threeParamCtor() {
        assertThat(event1.getType(), is(BookListCardClickEvent.Type.NORMAL));
        assertThat(event1.getListName(), is("List 1"));
        assertThat(event1.getActionId(), is(-1));
        assertThat(event1.getPosition(), is(5));
    }

    @Test
    public void fourParamCtor() {
        assertThat(event2.getType(), is(BookListCardClickEvent.Type.LONG));
        assertThat(event2.getListName(), is("List 2"));
        assertThat(event2.getActionId(), is(R.id.action_clear));
        assertThat(event2.getPosition(), is(6));
    }
}
