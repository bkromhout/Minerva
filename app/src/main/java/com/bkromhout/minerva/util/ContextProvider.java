package com.bkromhout.minerva.util;

import android.app.Activity;
import com.bkromhout.minerva.Minerva;

/**
 * Base for interfaces which need a method that provides a Context. Prevents clashing.
 */
public interface ContextProvider {
    /**
     * Called to obtain access to an Activity context. If the Application context is sufficient, prefer using {@link
     * Minerva#getAppCtx()}.
     * @return Activity context.
     */
    Activity getCtx();
}
