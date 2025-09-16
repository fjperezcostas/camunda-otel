package org.camunda.otel.interceptors;

import io.grpc.ForwardingServerCallListener;
import io.grpc.ServerCall;

import static org.camunda.otel.interceptors.OtelProps.*;

import org.slf4j.MDC;

import java.util.Map;


public class OtelListener<T> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<T> {
    private final Map<String, String> mdc;

    public OtelListener(ServerCall.Listener<T> delegate, Map<String, String> mdc) {
        super(delegate);
        this.mdc = mdc;
    }

    public void onMessage(T message) {
        addTracingContext(() -> super.onMessage(message));
    }

    public void onHalfClose() {
        addTracingContext(super::onHalfClose);
    }

    public void onCancel() {
        addTracingContext(() -> closeSpan(super::onCancel));
    }

    public void onComplete() {
        addTracingContext(() -> closeSpan(super::onComplete));
    }

    public void onReady() {
        addTracingContext(super::onReady);
    }

    private void addTracingContext(Runnable function) {
        MDC.put(VERSION, mdc.get(VERSION));
        MDC.put(TRACE_ID, mdc.get(TRACE_ID));
        MDC.put(SPAN_ID, mdc.get(SPAN_ID));
        MDC.put(TRACE_FLAGS, mdc.get(TRACE_FLAGS));
        try {
            function.run();
        } finally {
            MDC.remove(VERSION);
            MDC.remove(TRACE_ID);
            MDC.remove(SPAN_ID);
            MDC.remove(TRACE_FLAGS);
        }
    }

    private void closeSpan(Runnable function) {
        try {
            function.run();
        } finally {
            // do nothing
        }
    }

}
