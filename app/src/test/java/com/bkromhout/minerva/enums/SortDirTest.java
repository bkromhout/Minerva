package com.bkromhout.minerva.enums;

import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;
import io.realm.Sort;
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
 * Tests the {@link SortDir}.
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
@PrepareForTest({C.class})
@SuppressStaticInitializationFor({"com.bkromhout.minerva.C"})
public class SortDirTest {
    /**
     * Expected return value for {@link SortDir#names()}.
     */
    private static String[] eNames = new String[] {"Ascending", "Descending"};

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
                {0, R.id.sort_asc, "Ascending", Sort.ASCENDING, 0},
                {1, R.id.sort_desc, "Descending", Sort.DESCENDING, 1}
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
     * Current Realm Sort enum member; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(3)
    public Sort realmSort;
    /**
     * Current enum member index, used to get the correct value for {@link #type} at runtime.
     */
    @Parameterized.Parameter(4)
    public int typeIdx;
    /**
     * Current {@link SortDir}.
     */
    private SortDir type;

    @Before
    public void before() {
        // Mock C.getStr() so that we don't have to use resources.
        PowerMockito.mockStatic(C.class);
        when(C.getStr(R.string.sort_asc)).thenReturn("Ascending");
        when(C.getStr(R.string.sort_desc)).thenReturn("Descending");

        // Resolve the enum member using an index parameter.
        type = SortDir.values()[typeIdx];
    }

    /**
     * Tests that {@link SortDir#names()} works correctly.
     */
    @Test
    public void namesArrayCorrect() {
        assertThat(SortDir.names(), is(eNames));
    }

    /**
     * Tests that the current {@link #type}'s getters will returns the correct values.
     */
    @Test
    public void typeReturnsCorrectValues() {
        assertThat(type.getNum(), is(num));
        assertThat(type.getResId(), is(resId));
        assertThat(type.getName(), is(name));
        assertThat(type.getRealmSort(), is(realmSort));
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
     * Tests that {@link SortDir#fromName(String)} works correctly.
     */
    @Test
    public void fromName() {
        assertThat(SortDir.fromName(name), is(type));
        assertThat(SortDir.fromName(null), not(type));
    }
}
