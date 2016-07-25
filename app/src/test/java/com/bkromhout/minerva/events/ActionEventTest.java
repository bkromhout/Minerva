package com.bkromhout.minerva.events;

import com.bkromhout.minerva.R;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests the {@link ActionEvent} class.
 */
public class ActionEventTest {
    /**
     * Created using {@link ActionEvent#ActionEvent(int, Object)}.
     */
    private static ActionEvent event1;
    /**
     * Created using {@link ActionEvent#ActionEvent(int, Object, int)}.
     */
    private static ActionEvent event2;

    @BeforeClass
    public static void setUp() {
        // Create events using constructors.
        event1 = new ActionEvent(R.id.action_new_list, "Data 1");
        event2 = new ActionEvent(R.id.action_new_tag, "Data 2", 5);
    }

    @AfterClass
    public static void tearDown() {
        event1 = null;
        event2 = null;
    }

    @Test
    public void twoParamCtor() {
        assertThat(event1.getActionId(), is(R.id.action_new_list));
        assertThat(event1.getData(), is("Data 1"));
        assertThat(event1.getPosToUpdate(), is(-1));
    }

    @Test
    public void threeParamCtor() {
        assertThat(event2.getActionId(), is(R.id.action_new_tag));
        assertThat(event2.getData(), is("Data 2"));
        assertThat(event2.getPosToUpdate(), is(5));
    }
}
