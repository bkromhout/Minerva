package com.bkromhout.minerva.enums;

import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link SortType}.
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
@PrepareForTest({C.class})
@SuppressStaticInitializationFor({"com.bkromhout.minerva.C"})
public class SortTypeTest {
    /**
     * Expected return value for {@link SortType#names()}.
     */
    private static String[] eNames = new String[] {"Title", "Author", "Time Added", "Rating"};

    /**
     * Parameters to test with. The last one in each group is the index, used to set the correct enum member as the
     * value of {@link #type} at test runtime.
     * <p>
     * This is necessary because our enums use string resources in their constructors. This leads to an issue where we
     * can't actually _get_ to the resources at the time this method is called, and thus cannot directly use the enum
     * members in our params here.
     */
    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][] {
                {0, R.id.sort_title, "Title", "title", 0},
                {1, R.id.sort_author, "Author", "author", 1},
                {2, R.id.sort_time_added, "Time Added", "lastImportDate", 2},
                {3, R.id.sort_rating, "Rating", "rating", 3}
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
    public String name;
    /**
     * Current Realm field name; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(3)
    public String realmField;
    /**
     * Current enum member index, used to get the correct value for {@link #type} at runtime.
     */
    @Parameterized.Parameter(4)
    public int typeIdx;
    /**
     * Current {@link SortType}.
     */
    private SortType type;

    @Before
    public void before() {
        // Mock C.getStr() so that we don't have to use resources.
        PowerMockito.mockStatic(C.class);
        when(C.getStr(R.string.sort_title)).thenReturn("Title");
        when(C.getStr(R.string.sort_author)).thenReturn("Author");
        when(C.getStr(R.string.sort_time_added)).thenReturn("Time Added");
        when(C.getStr(R.string.sort_rating)).thenReturn("Rating");

        // Resolve the enum member using an index parameter.
        type = SortType.values()[typeIdx];
    }

    /**
     * Tests that {@link SortType#names()} works correctly.
     */
    @Test
    public void namesArrayCorrect() {
        assertThat(SortType.names(), is(eNames));
    }

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
     * Tests that {@link SortType#fromName(String)} works correctly.
     */
    @Test
    public void fromName() {
        assertThat(SortType.fromName(name), is(type));
        assertThat(SortType.fromName(null), not(type));
    }
}
