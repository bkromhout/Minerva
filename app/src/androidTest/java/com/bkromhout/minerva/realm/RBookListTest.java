package com.bkromhout.minerva.realm;

import android.support.test.runner.AndroidJUnit4;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.data.UniqueIdFactory;
import com.bkromhout.minerva.test.TestBookFactory;
import com.bkromhout.minerva.test.TestRealmConfigurationFactory;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests the {@link RBookList} class.
 */
@RunWith(AndroidJUnit4.class)
public class RBookListTest {
    private static final int NUM_TEST_BOOKS = 5;
    // Should include all test books, sorted by title in ascending order.
    private static final String RUQ_STR_1 = "RBook#$_Ruqus_RUQ_$##$_Ruqus_RUQ_$#title<<>>ASC";
    // Should include only the first test book, whose title is "Test Book 1".
    private static final String RUQ_STR_2 = "RBook#$_Ruqus_RUQ_$#NORMAL|||RBook|||title|||Test Book 1::STRING|||" +
            "com.bkromhout.ruqus.transformers.EqualTo#$_Ruqus_RUQ_$#";

    @Rule
    public final TestRealmConfigurationFactory configFactory = new TestRealmConfigurationFactory();

    private TestBookFactory testBookFactory;
    private Realm realm;
    private RealmConfiguration realmConfig;
    private List<RBook> testBooks;

    @Before
    public void setUp() {
        testBookFactory = new TestBookFactory();
        realmConfig = configFactory.createConfiguration();
        realm = Realm.getInstance(realmConfig);
        UniqueIdFactory.getInstance().setUpTempIds(realm);
        // Create and add books to realm to test with.
        realm.beginTransaction();
        testBooks = realm.copyToRealm(TestBookFactory.toRBooks(testBookFactory.generateMultiple(NUM_TEST_BOOKS)));
        realm.commitTransaction();
    }

    @After
    public void tearDown() {
        testBooks = null;
        UniqueIdFactory.getInstance().tearDownTempIds();
        if (realm != null) realm.close();
    }

    @Test
    public void testNormalList() {
        // Create list and do initial checks.
        realm.beginTransaction();
        RBookList list = realm.copyToRealm(new RBookList("list"));
        realm.commitTransaction();

        assertThat(list.getNextPos(), is(0L));
        assertThat(list.isSmartList(), is(false));
        assertThat(list.getSmartListRuqString(), nullValue());
        assertThat(list.getListItems().size(), is(0));

        // Add books and verify.
        list.addBooks(realm, testBooks);

        assertThat(list.getNextPos(), is(C.LIST_ITEM_GAP * NUM_TEST_BOOKS));
        assertItemsCorrect(list, 1, 2, 3, 4, 5);

        // Remove books and verify.
        List<RBook> toRemove = new ArrayList<>();
        toRemove.add(testBooks.get(0));
        toRemove.add(testBooks.get(2));
        list.removeBooks(realm, toRemove);

        assertThat(list.getNextPos(), is(C.LIST_ITEM_GAP * NUM_TEST_BOOKS));
        assertItemsCorrect(list, 2, 4, 5);

        // Re-add books and verify that we didn't add duplicates.
        list.addBooks(realm, testBooks);

        assertThat(list.getNextPos(), is(C.LIST_ITEM_GAP * (NUM_TEST_BOOKS + 2)));
        assertItemsCorrect(list, 2, 4, 5, 1, 3);
    }

    @Test
    public void testSmartList() {
        realm.beginTransaction();
        RBookList smartList1 = new RBookList("smartList1");
        smartList1.setSmartListRuqString(RUQ_STR_1);
        RBookList smartList2 = new RBookList("smartList2");
        smartList2.setSmartListRuqString(RUQ_STR_2);
        realm.commitTransaction();

        assertThat(smartList1.isSmartList(), is(true));
        assertThat(smartList2.isSmartList(), is(true));
    }

    /**
     * Checks that the items in {@code list} are correct by checking that there are the same number of them as the
     * number of {@code nums}, and that they are in the same order as {@code nums}.
     * @param list {@link RBookList} whose items will be checked.
     * @param nums List of numbers which were used when generating the books that the {@code list}'s items point to.
     */
    private void assertItemsCorrect(RBookList list, int... nums) {
        List<RBookListItem> items = list.getListItems();
        assertThat(items.size(), is(nums.length));
        for (int i = 0; i < nums.length; i++)
            assertThat(items.get(i).getBook().getRelPath(), is(TestBookFactory.BASE_TEST_PATH + nums[i]));
    }
}
