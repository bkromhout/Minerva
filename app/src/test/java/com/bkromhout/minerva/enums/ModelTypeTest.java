package com.bkromhout.minerva.enums;

import com.bkromhout.minerva.realm.*;
import io.realm.RealmModel;
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
 * Tests the {@link ModelType}.
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
public class ModelTypeTest {
    /**
     * Parameters to test with.
     */
    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][] {
                {RBook.class, ModelType.BOOK},
                {RBookList.class, ModelType.BOOK_LIST},
                {RBookListItem.class, ModelType.BOOK_LIST_ITEM},
                {RTag.class, ModelType.TAG},
        });
    }

    /**
     * Current associated Class; should correspond to {@link #type}.
     */
    @Parameterized.Parameter(0)
    public Class<? extends RealmModel> clazz;
    /**
     * Current {@link ModelType}.
     */
    @Parameterized.Parameter(1)
    public ModelType type;

    @Test
    public void fromRealmClass() {
        assertThat(ModelType.fromRealmClass(clazz), is(type));
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromInvalidRealmClass() {
        ModelType.fromRealmClass(RImportLog.class);
    }
}
