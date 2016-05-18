package com.bkromhout.minerva.enums;

import com.bkromhout.minerva.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests the {@link BookCardTypeTest}.
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
public class BookCardTypeTest {
    /**
     * Parameters to test with. The last one in each group is the index, used to set the correct enum member as the
     * value of {@link #type} at test runtime.
     * <p>
     * This is necessary because our enums use string resources in their constructors. This leads to an issue where we
     * can't actually _get_ to the resources at the time this method is called, and thus cannot directly use the enum
     * members in our params here.
     */
    @Parameterized.Parameters(name = "{3}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][] {
                {0, R.id.card_normal, R.string.card_normal, BookCardType.NORMAL},
                {1, R.id.card_no_cover, R.string.card_no_cover, BookCardType.NO_COVER},
                {2, R.id.card_compact, R.string.card_compact, BookCardType.COMPACT}
        });
    }

    /**
     * Current number; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(0)
    public int num;
    /**
     * Current resource ID; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(1)
    public int resId;
    /**
     * Current name; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(2)
    public int name;
    /**
     * Current {@link BookCardType}.
     */
    @Parameterized.Parameter(3)
    public BookCardType type;

    /**
     * Tests that the current {@link #type}'s getters will returns the correct values.
     */
    @Test
    public void typeReturnsCorrectValues() {
        assertThat(type.getNum(), is(num));
        assertThat(type.getResId(), is(resId));
        assertThat(type.getName(), is(name));
    }

    /**
     * Tests that {@link BookCardType#fromNumber(int)} works correctly.
     */
    @Test
    public void fromNumber() {
        assertThat(BookCardType.fromNumber(num), is(type));
        assertThat(BookCardType.fromNumber(num + 1), not(type));
    }

    /**
     * Tests that {@link BookCardType#fromResId(int)} works correctly.
     */
    @Test
    public void fromResId() {
        assertThat(BookCardType.fromResId(resId), is(type));
        assertThat(BookCardType.fromResId(resId + 1), not(type));
    }

    /**
     * Tests that {@link BookCardType#fromName(int)} works correctly.
     */
    @Test
    public void fromName() {
        assertThat(BookCardType.fromName(name), is(type));
        assertThat(BookCardType.fromName(-1), not(type));
    }
}
