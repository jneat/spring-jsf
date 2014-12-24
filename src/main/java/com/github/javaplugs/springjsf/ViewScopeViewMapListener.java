/*
 * Copyright (c) 2014 Vladislav Zablotsky
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
package com.github.javaplugs.springjsf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.component.UIViewRoot;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PreDestroyViewMapEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.ViewMapListener;
import java.lang.ref.WeakReference;

/**
 * @author rumatoest
 * @author Michail Nikolaev ate: 21.11.12 Time: 0:37
 */
public class ViewScopeViewMapListener implements ViewMapListener {

    private static final Logger logger = LoggerFactory.getLogger(ViewScopeViewMapListener.class);

    private String name;

    private Runnable callback;

    private boolean callbackCalled = false;

    private WeakReference<UIViewRoot> uiViewRootWeakReference;

    private ViewScope viewScope;

    public ViewScopeViewMapListener(UIViewRoot root, String name, Runnable callback, ViewScope viewScope) {
        this.name = name;
        this.callback = callback;
        this.uiViewRootWeakReference = new WeakReference<>(root);
        this.viewScope = viewScope;
    }

    @Override
    public void processEvent(SystemEvent event) throws AbortProcessingException {
        if (event instanceof PreDestroyViewMapEvent) {
            logger.debug("Going call callback for bean {}", name);
            doCallback();
            viewScope.clearFromListener(this);
        }
    }

    public boolean checkRoot() {
        if (uiViewRootWeakReference.get() == null) {
            doCallback();
            return true;
        }
        return false;
    }

    public synchronized void doCallback() {
        logger.debug("Going call callback for bean {}", name);
        if (!callbackCalled) {
            try {
                callback.run();
            } finally {
                callbackCalled = true;
            }
        }
    }

    @Override
    public boolean isListenerForSource(Object source) {
        return (source == uiViewRootWeakReference.get());
    }
}
