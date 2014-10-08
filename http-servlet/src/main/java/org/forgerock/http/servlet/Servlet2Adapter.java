/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.http.servlet;

import java.util.concurrent.CountDownLatch;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An adapter for use in Servlet 2.x containers.
 *
 * @since 1.0.0
 */
final class Servlet2Adapter extends ServletApiVersionAdapter {

    /**
     * Synchronization implementation. Package private because it is used as the
     * fall-back implementation in Servlet 3 when async is not supported.
     */
    final static class Servlet2Synchronizer implements ServletSynchronizer {
        private final HttpServletRequest httpRequest;
        private final HttpServletResponse httpResponse;
        private final CountDownLatch requestCompletionLatch = new CountDownLatch(1);

        Servlet2Synchronizer(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
            this.httpRequest = httpRequest;
            this.httpResponse = httpResponse;
        }

        @Override
        public void addAsyncListener(Runnable runnable) {
            // Do nothing - this dispatcher is blocking.
        }

        @Override
        public void awaitIfNeeded() throws Exception {
            requestCompletionLatch.await();
        }

        @Override
        public boolean isAsync() {
            return false;
        }

        @Override
        public void signal() {
            requestCompletionLatch.countDown();
        }

        @Override
        public void signalAndComplete() {
            requestCompletionLatch.countDown();
        }

        @Override
        public void signalAndComplete(final Throwable t) {
//            fail(httpRequest, httpResponse, t); //FIXME is this still needed?
            requestCompletionLatch.countDown();
        }
    }

    Servlet2Adapter() {
        // Nothing to do.
    }

    @Override
    public ServletSynchronizer createServletSynchronizer(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return new Servlet2Synchronizer(httpRequest, httpResponse);
    }
}