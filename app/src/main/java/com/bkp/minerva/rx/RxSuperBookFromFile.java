package com.bkp.minerva.rx;

import com.bkp.minerva.util.SuperBook;
import com.bkp.minerva.util.Util;
import rx.Observable;

import java.io.File;

/**
 * Transforms Files into {@link SuperBook}s using epublib calls.
 * <p>
 * TODO get the hash of the file and put it into the superbook
 */
public class RxSuperBookFromFile implements Observable.Transformer<File, SuperBook> {
    /**
     * Library path.
     */
    private final String libPath;

    /**
     * Create a new {@link RxSuperBookFromFile}.
     * @param libPath Library path.
     */
    public RxSuperBookFromFile(String libPath) {
        this.libPath = libPath;
    }

    @Override
    public Observable<SuperBook> call(Observable<File> fileObservable) {
        return fileObservable.map((file) -> {
            String relPath = file.getAbsolutePath().replace(libPath, "");
            return new SuperBook(Util.readEpubFile(file), relPath);
        });
    }
}
