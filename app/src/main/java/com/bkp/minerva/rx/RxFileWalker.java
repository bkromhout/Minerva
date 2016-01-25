package com.bkp.minerva.rx;

import com.bkp.minerva.util.Util;
import rx.Observable;
import rx.Subscriber;

import java.io.File;
import java.util.List;

/**
 * Recursively walks down from a given starting directory and emits File objects for any files whose extensions match
 * the given extensions.
 * <p>
 * TODO make this recurse using actual RxJava code.
 */
public class RxFileWalker implements Observable.OnSubscribe<File> {
    /**
     * Root File object to start walking at. Should be a directory.
     */
    private final File root;
    /**
     * List of file extensions. Only files whose extensions are in this list will be emitted. If the list is null, all
     * files will be emitted; if the list is empty, no files will be emitted.
     */
    private final List<String> extensions;

    /**
     * Create a new {@link RxFileWalker}.
     * @param root       The directory to start at.
     * @param extensions The extensions to limit which files to emit. If null, will emit all files.
     */
    public RxFileWalker(File root, List<String> extensions) {
        this.root = root;
        this.extensions = extensions;
    }

    @Override
    public void call(Subscriber<? super File> sub) {
        // Check root dir.
        if (root == null || !root.exists() || !root.isDirectory() || !root.canRead()) {
            if (!sub.isUnsubscribed()) sub.onError(new IllegalArgumentException("Bad root!"));
            return;
        }

        // If the extensions list is empty, we wouldn't match anything, so just complete now.
        if (extensions != null && extensions.isEmpty()) {
            if (!sub.isUnsubscribed()) sub.onCompleted();
            return;
        }

        // Walk the directory, recursing into any subdirectories as well.
        walk(root, sub);
        if (!sub.isUnsubscribed()) sub.onCompleted();
    }

    /**
     * Walk the given directory, recursing into any subdirectories.
     * <p>
     * Will return ASAP if {@code sub.isUnsubscribed() == true}.
     * @param root Directory to walk.
     * @param sub  The subscriber to emit Files to.
     */
    public void walk(File root, Subscriber<? super File> sub) {
        // Return ASAP if we have no subscribers.
        if (sub.isUnsubscribed()) return;

        // List files and directories in this directory, then iterate through and check all of them.
        File[] list = root.listFiles();
        if (list == null) return;
        for (File f : list) {
            // Return ASAP if we have no subscribers.
            if (sub.isUnsubscribed()) return;

            // Check this File object.
            if (f.isDirectory()) {
                // A directory, we'll walk it too.
                walk(f, sub);
            } else {
                // It's a file and its extension is in the list (or the list of extensions is null), so emit it.
                if ((extensions == null || extensions.contains(Util.getExtFromFName(f.getName())))
                        && !sub.isUnsubscribed()) sub.onNext(f);

            }
        }
    }
}
