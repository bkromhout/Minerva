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
 * Tests the {@link MainFrag}.
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
public class MainFragTest {
    /**
     * Parameters to test with.
     */
    @Parameterized.Parameters(name = "{3}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][] {
                {0, R.string.nav_item_recent, R.id.nav_recent, MainFrag.RECENT},
                {1, R.string.nav_item_library, R.id.nav_library, MainFrag.LIBRARY},
                {2, R.string.nav_item_all_lists, R.id.nav_all_lists, MainFrag.ALL_LISTS},
                {3, R.string.nav_item_power_search, R.id.nav_power_search, MainFrag.POWER_SEARCH}
        });
    }

    /**
     * Current number; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(0)
    public int index;
    /**
     * Current title string resource ID; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(1)
    public int titleRes;
    /**
     * Current resource ID; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(2)
    public int idRes;
    /**
     * Current {@link MainFrag}.
     */
    @Parameterized.Parameter(3)
    public MainFrag type;

    /**
     * Tests that the current {@link #type}'s getters will returns the correct values.
     */
    @Test
    public void typeReturnsCorrectValues() {
        assertThat(type.getIndex(), is(index));
        assertThat(type.getTitleRes(), is(titleRes));
        assertThat(type.getIdRes(), is(idRes));
    }

    /**
     * Tests that {@link MainFrag#fromIndex(int)} works correctly.
     */
    @Test
    public void fromIndex() {
        assertThat(MainFrag.fromIndex(index), is(type));
        assertThat(MainFrag.fromIndex(index + 1), not(type));
    }
}
