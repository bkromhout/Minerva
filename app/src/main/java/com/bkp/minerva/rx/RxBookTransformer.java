package com.bkp.minerva.rx;

import com.bkp.minerva.util.Util;
import nl.siegmann.epublib.domain.Book;
import rx.Observable;

import java.io.File;

/**
 * Transforms Files into Books using epublib calls.
 */
public class RxBookTransformer implements Observable.Transformer<File, Book> {

    @Override
    public Observable<Book> call(Observable<File> fileObservable) {
        return fileObservable.map(Util::readEpubFile);
    }
}
