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
 * Tests the {@link SortType}.
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
public class SortTypeTest {
    /**
     * Parameters to test with. The last one in each group is the index, used to set the correct enum member as the
     * value of {@link #type} at test runtime.
     * <p>
     * This is necessary because our enums use string resources in their constructors. This leads to an issue where we
     * can't actually _get_ to the resources at the time this method is called, and thus cannot directly use the enum
     * members in our params here.
     */
    @Parameterized.Parameters(name = "{4}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][] {
                {0, R.id.sort_title, R.string.sort_title, "title", SortType.TITLE},
                {1, R.id.sort_author, R.string.sort_author, "author", SortType.AUTHOR},
                {2, R.id.sort_time_added, R.string.sort_time_added, "lastImportDate", SortType.TIME_ADDED},
                {3, R.id.sort_rating, R.string.sort_rating, "rating", SortType.RATING}
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
     * Current Realm field name; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(3)
    public String realmField;
    /**
     * Current {@link SortType}.
     */
    @Parameterized.Parameter(4)
    private SortType type;

    /**
     * Tests that the current {@link #type}'s getters will returns the correct values.
     */
    @Test
    public void typeReturnsCorrectValues() {
        assertThat(type.getNum(), is(num));
        assertThat(type.getResId(), is(resId));
        assertThat(type.getName(), is(name));
        assertThat(type.getRealmField(), is(realmField));
    }

    /**
     * Tests that {@link SortType#fromNumber(int)} works correctly.
     */
    @Test
    public void fromNumber() {
        assertThat(SortType.fromNumber(num), is(type));
        assertThat(SortType.fromNumber(num + 1), not(type));
    }

    /**
     * Tests that {@link SortType#fromResId(int)} works correctly.
     */
    @Test
    public void fromResId() {
        assertThat(SortType.fromResId(resId), is(type));
        assertThat(SortType.fromResId(resId + 1), not(type));
    }

    /**
     * Tests that {@link SortType#fromName(int)} works correctly.
     */
    @Test
    public void fromName() {
        assertThat(SortType.fromName(name), is(type));
        assertThat(SortType.fromName(-1), not(type));
    }
}
