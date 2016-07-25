package com.bkromhout.minerva.enums;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests the {@link MarkType}.
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
public class MarkTypeTest {
    /**
     * Parameters to test with.
     */
    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][] {
                {"isNew", "New", MarkType.NEW},
                {"isUpdated", "Updated", MarkType.UPDATED}
        });
    }

    /**
     * Current field name; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(0)
    public String fieldName;
    /**
     * Current tag name; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(1)
    public String tagName;
    /**
     * Current {@link MarkType}.
     */
    @Parameterized.Parameter(2)
    public MarkType type;

    /**
     * Tests that the current {@link #type}'s getters will returns the correct values.
     */
    @Test
    public void typeReturnsCorrectValues() {
        assertThat(type.getFieldName(), is(fieldName));
        //assertThat(type.getTagName(), is(tagName));
    }
}
