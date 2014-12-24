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
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.PreDestroyViewMapEvent;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * @author rumatoest
 * @author m.nikolaev Date: 21.11.12 Time: 0:37
 */
public class ViewScope implements Scope, Serializable, HttpSessionBindingListener {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(ViewScope.class);

    private final WeakHashMap<HttpSession, Set<ViewScopeViewMapListener>> sessionToListeners = new WeakHashMap<>();

    @Override
    public Object get(String name, ObjectFactory objectFactory) {
        Map<String, Object> viewMap = FacesContext.getCurrentInstance().getViewRoot().getViewMap();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (viewMap) {
            if (viewMap.containsKey(name)) {
                return viewMap.get(name);
            } else {
                logger.debug("Creating bean {}", name);
                Object object = objectFactory.getObject();
                viewMap.put(name, object);

                return object;
            }
        }
    }

    @Override
    public Object remove(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getConversationId() {
        return null;
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        logger.debug("registerDestructionCallback for bean {}", name);
        UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();
        ViewScopeViewMapListener listener
            = new ViewScopeViewMapListener(viewRoot, name, callback, this);

        viewRoot.subscribeToViewEvent(PreDestroyViewMapEvent.class, listener);

        HttpSession httpSession = (HttpSession)FacesContext.getCurrentInstance().getExternalContext().getSession(true);
        final Set<ViewScopeViewMapListener> sessionListeners;
        synchronized (sessionToListeners) {
            if (!sessionToListeners.containsKey(httpSession)) {
                sessionToListeners.put(httpSession, new HashSet<ViewScopeViewMapListener>());
            }
            sessionListeners = sessionToListeners.get(httpSession);
        }
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (sessionListeners) {
            Set<ViewScopeViewMapListener> toRemove = new HashSet<>();
            for (ViewScopeViewMapListener viewMapListener : sessionListeners) {
                if (viewMapListener.checkRoot()) {
                    toRemove.add(viewMapListener);
                }
            }
            sessionListeners.removeAll(toRemove);
            sessionListeners.add(listener);
        }
        if (!FacesContext.getCurrentInstance().getExternalContext().getSessionMap().containsKey("sessionBindingListener")) {
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("sessionBindingListener", this);
        }

    }

    @Override
    public Object resolveContextualObject(String key) {
        return null;
    }

    @Override
    public void valueBound(HttpSessionBindingEvent event) {
        logger.debug("Session event bound {}", event.getName());
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        logger.debug("Session event unbound {}", event.getName());
        final Set<ViewScopeViewMapListener> listeners;
        synchronized (sessionToListeners) {
            if (sessionToListeners.containsKey(event.getSession())) {
                listeners = sessionToListeners.get(event.getSession());
                sessionToListeners.remove(event.getSession());
            } else {
                listeners = null;
            }
        }
        if (listeners != null) {
            for (ViewScopeViewMapListener listener : listeners) {
                listener.doCallback();
            }
        }
    }

    public void clearFromListener(ViewScopeViewMapListener listener) {
        logger.debug("Removing listener from map");
        HttpSession httpSession = (HttpSession)FacesContext.getCurrentInstance().getExternalContext().getSession(false);
        if (httpSession != null) {
            synchronized (sessionToListeners) {
                if (sessionToListeners.containsKey(httpSession)) {
                    sessionToListeners.get(httpSession).remove(listener);
                }
            }
        }
    }
}
