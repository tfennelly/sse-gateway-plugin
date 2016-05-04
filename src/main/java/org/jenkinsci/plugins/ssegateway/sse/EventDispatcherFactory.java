/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
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
package org.jenkinsci.plugins.ssegateway.sse;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@Restricted(NoExternalUse.class)
public class EventDispatcherFactory {

    public static final String DISPATCHER_SESSION_KEY = EventDispatcher.class.getName();
    
    private static Class<? extends EventDispatcher> runtimeClass;
    
    static {
        try {
            if (isAsyncSupported()) {
                runtimeClass = (Class<? extends EventDispatcher>) Class.forName(EventDispatcherFactory.class.getPackage().getName() + ".AsynchEventDispatcher");
            } else {
                runtimeClass = (Class<? extends EventDispatcher>) Class.forName(EventDispatcherFactory.class.getPackage().getName() +   ".SynchEventDispatcher");
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unexpected Exception.", e);
        }
    }
    
    public static EventDispatcher start(HttpServletRequest request, HttpServletResponse response) {
        try {
            EventDispatcher instance = getDispatcher(request.getSession());
            instance.start(request, response);
            instance.setDefaultHeaders();
            instance.dispatchEvent("open", null);
            return instance;
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected Exception.", e);
        }
    }

    /**
     * Get the session {@link EventDispatcher} from the {@link HttpSession}.
     * <p>
     * Creates a new {@link EventDispatcher} (and binds it to the {@link HttpSession}) if
     * non exists.
     *     
     * @param session The {@link HttpSession}.
     * @return The session {@link EventDispatcher}.
     */
    public synchronized static EventDispatcher getDispatcher(@Nonnull HttpSession session) {
        EventDispatcher dispatcher = (EventDispatcher) session.getAttribute(DISPATCHER_SESSION_KEY);
        if (dispatcher == null) {
            try {
                dispatcher = runtimeClass.newInstance();
                session.setAttribute(DISPATCHER_SESSION_KEY, dispatcher);
            } catch (Exception e) {
                throw new IllegalStateException("Unexpected Exception.", e);
            }
        }
        return dispatcher;
    }

    private static boolean isAsyncSupported() {
        // We can use a system property for test overriding.
        String asyncSupportedProp = System.getProperty("jenkins.eventbus.web.asyncSupported");
        if (asyncSupportedProp != null) {
            return asyncSupportedProp.equals("true");
        }
        
        try {
            HttpServletRequest.class.getMethod("startAsync");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}