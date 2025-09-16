package org.camunda.otel.interceptors;

import io.grpc.*;
import io.grpc.ServerCall.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class AuthInterceptor implements ServerInterceptor {

    private final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    public AuthInterceptor() {}

    public <T, U> Listener<T> interceptCall(ServerCall<T, U> call, Metadata headers, ServerCallHandler<T, U> next) {
        log.info("starting auth-interceptor...");
        authorize();
        log.info("auth process finished!");
        return next.startCall(call, headers);
    }

    private void authorize() {
        log.info("doing authorization stuff...");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }

}
