/*
 * Copyright (c) 2015 Vladislav Zablotsky
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.javaplugs.jsf;

import javax.faces.component.UIViewRoot;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PreDestroyViewMapEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.ViewMapListener;
import java.lang.ref.WeakReference;
import org.apache.log4j.Logger;

/**
 * Listener for processing ViewMapDestroydEvent
 * that indicates the argument root just had its associated view map destroyed.
 *
 * @see javax.faces.event.PreDestroyViewMapEvent
 *
 * @author Vladislav Zablotsky
 * @author Michail Nikolaev (original codebase author)
 */
public class ViewScopeViewMapListener implements ViewMapListener {

    private static final Logger logger = Logger.getLogger(ViewScope.class);

    private final String name;

    private final Runnable callback;

    private boolean callbackCalled = false;

    private final WeakReference<UIViewRoot> uiViewRootWeakReference;

    private final ViewScope viewScope;

    public ViewScopeViewMapListener(UIViewRoot root, String name, Runnable callback, ViewScope viewScope) {
        this.name = name;
        this.callback = callback;
        this.uiViewRootWeakReference = new WeakReference<>(root);
        this.viewScope = viewScope;
    }

    public synchronized void doCallback() {
        logger.trace("Going call callback for bean " + name);
        if (!callbackCalled) {
            try {
                callback.run();
            } finally {
                callbackCalled = true;
            }
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean isListenerForSource(Object source) {
        return (source == uiViewRootWeakReference.get());
    }

    @Override
    public void processEvent(SystemEvent event) throws AbortProcessingException {
        if (event instanceof PreDestroyViewMapEvent) {
            logger.trace("Going call callback for bean " + name);
            doCallback();
            viewScope.unregisterListener(this);
        }
    }

}
