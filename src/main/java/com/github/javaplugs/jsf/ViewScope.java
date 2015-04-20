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
import org.apache.log4j.Logger;

/**
 * Implementation of JSF view scope for spring beans.
 * The core idea is to put spring bean into JSF view scope, that you can access from
 * FacesContext.getCurrentInstance().getViewRoot().getViewMap()
 *
 * @author Vladislav Zablotsky
 * @author Michail Nikolaev (original codebase author)
 */
public class ViewScope implements Scope, Serializable, HttpSessionBindingListener {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(ViewScope.class);

    private final WeakHashMap<HttpSession, Set<ViewScopeViewMapListener>> sessionToListeners = new WeakHashMap<>();

    @Override
    public Object get(String name, ObjectFactory objectFactory) {
        Map<String, Object> viewMap = FacesContext.getCurrentInstance().getViewRoot().getViewMap();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        if (viewMap.containsKey(name)) {
            return viewMap.get(name);
        } else {
            synchronized (viewMap) {
                if (viewMap.containsKey(name)) {
                    return viewMap.get(name);
                } else {
                    logger.trace("Creating bean " + name);
                    Object object = objectFactory.getObject();
                    viewMap.put(name, object);
                    return object;
                }
            }
        }
    }

    @Override
    public String getConversationId() {
        return null;
    }

    /**
     * Removing bean from scope and unregister it destruction callback without executing them.
     *
     * @see Scope for more details
     */
    @Override
    public Object remove(String name) {
        Map<String, Object> viewMap = FacesContext.getCurrentInstance().getViewRoot().getViewMap();
        if (viewMap.containsKey(name)) {
            Object removed;
            synchronized (viewMap) {
                if (viewMap.containsKey(name)) {
                    removed = FacesContext.getCurrentInstance().getViewRoot().getViewMap().remove(name);
                } else {
                    return null;
                }
            }

            HttpSession httpSession = (HttpSession)FacesContext.getCurrentInstance().getExternalContext().getSession(true);
            Set<ViewScopeViewMapListener> sessionListeners;
            sessionListeners = sessionToListeners.get(httpSession);
            if (sessionListeners != null) {
                Set<ViewScopeViewMapListener> toRemove = new HashSet<>();
                for (ViewScopeViewMapListener listener : sessionListeners) {
                    if (listener.getName().equals(name)) {
                        toRemove.add(listener);
                        FacesContext.getCurrentInstance().getViewRoot().unsubscribeFromViewEvent(PreDestroyViewMapEvent.class, listener);
                    }
                }
                synchronized (sessionListeners) {
                    sessionListeners.removeAll(toRemove);
                }
            }

            return removed;
        }
        return null;
    }

    /**
     * Register callback to be executed only on whole scope destroying (not single object).
     *
     * @see Scope for more details
     */
    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        logger.trace("registerDestructionCallback for bean " + name);

        UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();
        ViewScopeViewMapListener listener = new ViewScopeViewMapListener(viewRoot, name, callback, this);

        viewRoot.subscribeToViewEvent(PreDestroyViewMapEvent.class, listener);

        HttpSession httpSession = (HttpSession)FacesContext.getCurrentInstance().getExternalContext().getSession(true);

        final Set<ViewScopeViewMapListener> sessionListeners;

        if (sessionToListeners.containsKey(httpSession)) {
            sessionListeners = sessionToListeners.get(httpSession);
        } else {
            synchronized (sessionToListeners) {
                if (sessionToListeners.containsKey(httpSession)) {
                    sessionListeners = sessionToListeners.get(httpSession);
                } else {
                    sessionListeners = new HashSet<>();
                    sessionToListeners.put(httpSession, sessionListeners);
                }
            }
        }

        synchronized (sessionListeners) {
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
        logger.trace("Session event bound " + event.getName());
    }

    /**
     * Seems like it called after our listeners was unbounded from http session.
     * Looks like view scope it destroyed. But should we call callback or not is a big question.
     *
     * @see HttpSessionBindingListener for more details
     */
    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        logger.trace("Session event unbound " + event.getName());
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
            // I just hope that JSF context already done this job
            for (ViewScopeViewMapListener listener : listeners) {
                // As long as our callbacks can run only once - this is not such big deal
                listener.doCallback();
            }
        }
    }

    /**
     * Will remove listener from session set and unregister it from UIViewRoot.
     */
    public void unregisterListener(ViewScopeViewMapListener listener) {
        logger.debug("Removing listener from map");
        HttpSession httpSession = (HttpSession)FacesContext.getCurrentInstance().getExternalContext().getSession(false);
        FacesContext.getCurrentInstance().getViewRoot().unsubscribeFromViewEvent(PreDestroyViewMapEvent.class, listener);
        if (httpSession != null) {
            synchronized (sessionToListeners) {
                if (sessionToListeners.containsKey(httpSession)) {
                    sessionToListeners.get(httpSession).remove(listener);
                }
            }
        }
    }
}
