package org.camunda.otel.interceptors;

import io.grpc.Metadata;

public final class OtelHeaders {

    public static Metadata.Key<String> X_TRACE_ID = Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);
    public static Metadata.Key<String> X_SPAN_ID = Metadata.Key.of("x-span-id", Metadata.ASCII_STRING_MARSHALLER);

}
