package com.bkromhout.minerva.events;

import com.bkromhout.minerva.R;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests the {@link TagCardClickEvent} class.
 */
public class TagCardClickEventTest {
    /**
     * Created using {@link TagCardClickEvent#TagCardClickEvent(TagCardClickEvent.Type, String)}.
     */
    private static TagCardClickEvent event1;
    /**
     * Created using {@link TagCardClickEvent#TagCardClickEvent(TagCardClickEvent.Type, String, int)}.
     */
    private static TagCardClickEvent event2;

    @BeforeClass
    public static void setUp() {
        // Create events using constructors.
        event1 = new TagCardClickEvent(TagCardClickEvent.Type.TEXT_COLOR, "Tag 1");
        event2 = new TagCardClickEvent(TagCardClickEvent.Type.ACTIONS, "Tag 2", R.id.action_rename_tag);
    }

    @AfterClass
    public static void tearDown() {
        event1 = null;
        event2 = null;
    }

    @Test
    public void twoParamCtor() {
        assertThat(event1.getType(), is(TagCardClickEvent.Type.TEXT_COLOR));
        assertThat(event1.getName(), is("Tag 1"));
        assertThat(event1.getActionId(), is(-1));
    }

    @Test
    public void threeParamCtor() {
        assertThat(event2.getType(), is(TagCardClickEvent.Type.ACTIONS));
        assertThat(event2.getName(), is("Tag 2"));
        assertThat(event2.getActionId(), is(R.id.action_rename_tag));
    }
}
