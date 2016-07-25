package com.bkromhout.minerva.realm;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.bkromhout.minerva.data.UniqueIdFactory;
import com.bkromhout.minerva.test.TestBookFactory;
import com.bkromhout.minerva.test.TestRealmConfigurationFactory;
import com.bkromhout.minerva.test.TestSuperBook;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests the {@link RBook} class.
 */
@RunWith(AndroidJUnit4.class)
public class RBookTest {
    @Rule
    public final TestRealmConfigurationFactory configFactory = new TestRealmConfigurationFactory(
            InstrumentationRegistry.getContext());

    private TestBookFactory testBookFactory;
    private Realm realm;
    private RealmConfiguration realmConfig;

    @Before
    public void setUp() {
        testBookFactory = new TestBookFactory();
        realmConfig = configFactory.createConfiguration();
        realm = Realm.getInstance(realmConfig);
        UniqueIdFactory.getInstance().setUpTempIds(realm);
    }

    @After
    public void tearDown() {
        UniqueIdFactory.getInstance().tearDownTempIds();
        if (realm != null) realm.close();
    }

    @Test
    public void doesNotUpdateRBook() {
        TestSuperBook superBook = testBookFactory.generate();
        realm.beginTransaction();
        RBook book1 = realm.copyToRealm(new RBook(superBook));
        book1.setNew(false);
        realm.commitTransaction();
        RBook book2 = new RBook(superBook);

        assertThat(book1.isNew(), is(false));
        assertThat(book1.isUpdated(), is(false));

        realm.beginTransaction();
        book1.updateFromOtherRBook(realm, book2);
        realm.commitTransaction();
        assertThat(book1.isUpdated(), is(false));
    }

    @Test
    public void updatesRBook() {
        TestSuperBook superBook1 = testBookFactory.generate();
        TestSuperBook superBook2 = testBookFactory.generate();
        realm.beginTransaction();
        RBook book1 = realm.copyToRealm(new RBook(superBook1));
        book1.setNew(false);
        realm.commitTransaction();
        RBook book2 = new RBook(superBook2);

        assertThat(book1.isNew(), is(false));
        assertThat(book1.isUpdated(), is(false));

        realm.beginTransaction();
        book1.updateFromOtherRBook(realm, book2);
        realm.commitTransaction();
        assertThat(book1.isUpdated(), is(true));
    }
}
