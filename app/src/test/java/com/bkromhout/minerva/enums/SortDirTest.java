package com.bkromhout.minerva.enums;

import com.bkromhout.minerva.R;
import io.realm.Sort;
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
 * Tests the {@link SortDir}.
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
public class SortDirTest {
    /**
     * Parameters to test with.
     */
    @Parameterized.Parameters(name = "{4}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][] {
                {0, R.id.sort_asc, R.string.sort_asc, Sort.ASCENDING, SortDir.ASC},
                {1, R.id.sort_desc, R.string.sort_desc, Sort.DESCENDING, SortDir.DESC}
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
     * Current Realm Sort enum member; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(3)
    public Sort realmSort;
    /**
     * Current {@link SortDir}.
     */
    @Parameterized.Parameter(4)
    public SortDir type;

    /**
     * Tests that the current {@link #type}'s getters will returns the correct values.
     */
    @Test
    public void typeReturnsCorrectValues() {
        assertThat(type.getNum(), is(num));
        assertThat(type.getResId(), is(resId));
        assertThat(type.getName(), is(name));
        assertThat(type.getRealmSort(3), is(new Sort[] {realmSort, realmSort, realmSort}));
    }

    /**
     * Tests that {@link SortDir#fromNumber(int)} works correctly.
     */
    @Test
    public void fromNumber() {
        assertThat(SortDir.fromNumber(num), is(type));
        assertThat(SortDir.fromNumber(num + 1), not(type));
    }

    /**
     * Tests that {@link SortDir#fromResId(int)} works correctly.
     */
    @Test
    public void fromResId() {
        assertThat(SortDir.fromResId(resId), is(type));
        assertThat(SortDir.fromResId(resId + 1), not(type));
    }

    /**
     * Tests that {@link SortDir#fromName(int)} works correctly.
     */
    @Test
    public void fromName() {
        assertThat(SortDir.fromName(name), is(type));
        assertThat(SortDir.fromName(-1), not(type));
    }
}
