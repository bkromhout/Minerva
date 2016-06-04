package com.bkromhout.minerva.realm;

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
import static org.hamcrest.core.Is.is;

/**
 * Tests the {@link RBook} class.
 */
@RunWith(AndroidJUnit4.class)
public class RBookTest {
    @Rule
    public final TestRealmConfigurationFactory configFactory = new TestRealmConfigurationFactory();

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
        RBook book2 = realm.copyToRealm(new RBook(superBook));
        book1.isNew = false;
        realm.commitTransaction();

        assertThat(book1.isNew, is(false));
        assertThat(book1.isUpdated, is(false));

        realm.beginTransaction();
        book1.updateFromOtherRBook(realm, book2);
        realm.commitTransaction();
        assertThat(book1.isUpdated, is(false));
    }
}
