package org.camunda.otel.interceptors;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.slf4j.MDC;

import static org.camunda.otel.interceptors.OtelProps.*;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class OtelInterceptor implements ServerInterceptor {

    public OtelInterceptor() {}

    public <T, U> ServerCall.Listener<T> interceptCall(ServerCall<T, U> call, Metadata headers, ServerCallHandler<T, U> next) {
        var traceparent = headers.get(Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER));
        String version;
        String traceId;
        String spanId;
        String flags;
        if (traceparent == null || traceparent.isEmpty()) {
            version = "00";
            traceId = UUID.randomUUID().toString().replace("-", "");
            spanId = Long.toString(ThreadLocalRandom.current().nextLong());
            flags = "01";
        } else {
            var slicedTraceparent = traceparent.split("-");
            version = slicedTraceparent[0];
            traceId = slicedTraceparent[1];
            spanId = slicedTraceparent[2];
            flags = slicedTraceparent[3];
        }
        MDC.put(VERSION, version);
        MDC.put(TRACE_ID, traceId);
        MDC.put(SPAN_ID, spanId);
        MDC.put(TRACE_FLAGS, flags);
        try {
            final ServerCall.Listener<T> delegate = next.startCall(new OtelServerCall<>(call, MDC.getCopyOfContextMap()), headers);
            return new OtelListener<>(delegate, MDC.getCopyOfContextMap());
        } finally {
            MDC.remove(VERSION);
            MDC.remove(TRACE_ID);
            MDC.remove(SPAN_ID);
            MDC.remove(TRACE_FLAGS);
        }
    }
}
