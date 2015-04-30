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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openig.audit.decoration;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.ResponseException;
import org.forgerock.http.protocol.Status;
import org.forgerock.openig.http.Exchange;
import org.forgerock.util.promise.Promises;
import org.mockito.Mock;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class AuditFilterTest extends AbstractAuditTest {

    @Mock
    private Filter delegate;

    @Mock
    private Handler handler;

    @Test
    public void shouldEmitAuditEventsWhenCompleted() throws Exception {
        AuditFilter audit = new AuditFilter(auditSystem, source, delegate, singleton("tag"));
        Exchange exchange = new Exchange();
        when(delegate.filter(exchange, null, handler))
                .thenReturn(Promises.<Response, ResponseException>newResultPromise(new Response()));

        audit.filter(exchange, null, handler).getOrThrow();

        verify(auditSystem, times(2)).onAuditEvent(captor.capture());
        assertThatEventIncludes(captor.getAllValues().get(0), exchange, "tag", "request");
        assertThatEventIncludes(captor.getAllValues().get(1), exchange, "tag", "response", "completed");
    }

    @Test
    public void shouldEmitAuditEventsWhenFailed() throws Exception {
        ResponseException exception = new ResponseException(Status.INTERNAL_SERVER_ERROR);
        when(delegate.filter(any(Exchange.class), any(Request.class), eq(handler)))
                .thenReturn(Promises.<Response, ResponseException>newExceptionPromise(exception));

        AuditFilter audit = new AuditFilter(auditSystem, source, delegate, singleton("tag"));

        Exchange exchange = new Exchange();
        try {
            audit.filter(exchange, null, handler).getOrThrow();
            failBecauseExceptionWasNotThrown(ResponseException.class);
        } catch (ResponseException e) {
            verify(auditSystem, times(2)).onAuditEvent(captor.capture());
            assertThatEventIncludes(captor.getAllValues().get(0), exchange, "tag", "request");
            assertThatEventIncludes(captor.getAllValues().get(1), exchange, "tag", "response", "exception");
        }
    }
}
