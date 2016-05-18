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
 * Tests the {@link BookCardTypeTest}.
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
@PrepareForTest({C.class})
@SuppressStaticInitializationFor({"com.bkromhout.minerva.C"})
public class BookCardTypeTest {
    /**
     * Expected return value for {@link BookCardType#names()}.
     */
    private static String[] eNames = new String[] {"Normal", "No Cover", "Compact"};

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
                {0, R.id.card_normal, "Normal", 0},
                {1, R.id.card_no_cover, "No Cover", 1},
                {2, R.id.card_compact, "Compact", 2}
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
     * Current enum member index, used to get the correct value for {@link #type} at runtime.
     */
    @Parameterized.Parameter(3)
    public int typeIdx;
    /**
     * Current {@link BookCardType}.
     */
    private BookCardType type;

    @Before
    public void before() {
        // Mock C.getStr() so that we don't have to use resources.
        PowerMockito.mockStatic(C.class);
        when(C.getStr(R.string.card_normal)).thenReturn("Normal");
        when(C.getStr(R.string.card_no_cover)).thenReturn("No Cover");
        when(C.getStr(R.string.card_compact)).thenReturn("Compact");

        // Resolve the enum member using an index parameter.
        type = BookCardType.values()[typeIdx];
    }

    /**
     * Tests that {@link BookCardType#names()} works correctly.
     */
    @Test
    public void namesArrayCorrect() {
        assertThat(BookCardType.names(), is(eNames));
    }

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
     * Tests that {@link BookCardType#fromName(String)} works correctly.
     */
    @Test
    public void fromName() {
        assertThat(BookCardType.fromName(name), is(type));
        assertThat(BookCardType.fromName(null), not(type));
    }
}
