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
     * Parameters to test with.
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][] {
                {SortType.TITLE, 0, R.id.sort_title, R.string.sort_title, new String[] {"title"}},
                {SortType.AUTHOR, 1, R.id.sort_author, R.string.sort_author, new String[] {"author"}},
                {SortType.TIME_ADDED, 2, R.id.sort_time_added, R.string.sort_time_added, new String[] {"lastImportDate"}},
                {SortType.RATING, 3, R.id.sort_rating, R.string.sort_rating, new String[] {"rating"}},
                {SortType.REL_PATH, 4, R.id.sort_rel_path, R.string.sort_rel_path, new String[] {"relPath", "title"}}
        });
    }

    /**
     * Current {@link SortType}.
     */
    @Parameterized.Parameter()
    public SortType type;
    /**
     * Current number; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(1)
    public int num;
    /**
     * Current resource ID; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(2)
    public int resId;
    /**
     * Current name; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(3)
    public int name;
    /**
     * Current Realm field names; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(4)
    public String[] realmFields;

    /**
     * Tests that the current {@link #type}'s getters will returns the correct values.
     */
    @Test
    public void typeReturnsCorrectValues() {
        assertThat(type.getNum(), is(num));
        assertThat(type.getResId(), is(resId));
        assertThat(type.getName(), is(name));
        assertThat(type.getRealmFields(), is(realmFields));
        assertThat(type.getNumRealmFields(), is(realmFields.length));
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
