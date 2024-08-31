package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/camunda/zeebe/clients/go/v8/pkg/entities"
	"github.com/camunda/zeebe/clients/go/v8/pkg/worker"
	"github.com/camunda/zeebe/clients/go/v8/pkg/zbc"
	"go.opentelemetry.io/contrib/instrumentation/google.golang.org/grpc/otelgrpc"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/jaeger"
	"go.opentelemetry.io/otel/propagation"
	"go.opentelemetry.io/otel/sdk/resource"
	"go.opentelemetry.io/otel/sdk/trace"
	semconv "go.opentelemetry.io/otel/semconv/v1.12.0"
	"google.golang.org/grpc"
)

func main() {
	tp, err := initTracer()
	if err != nil {
		log.Fatalf("failed to initialize tracer: %v", err)
	}
	defer func() { _ = tp.Shutdown(context.Background()) }()
	otel.SetTextMapPropagator(propagation.TraceContext{})
	client, err := zbc.NewClient(&zbc.ClientConfig {
		GatewayAddress: "zeebe:26500",
		UsePlaintextConnection: true,
		DialOpts: []grpc.DialOption{
			grpc.WithUnaryInterceptor(otelgrpc.UnaryClientInterceptor()),
			grpc.WithStreamInterceptor(otelgrpc.StreamClientInterceptor()),
		},
	})
	if err != nil {
		panic(any(err))
	}
	log.Println("starting job-worker...")
	jobWorker := client.NewJobWorker().JobType("otel-job").Handler(handleJob).Open()
	log.Println("job-worker started successfully")
	closeJob := make(chan os.Signal, 1)
	signal.Notify(closeJob, syscall.SIGINT, syscall.SIGTERM)
	go func() {
		<-closeJob
		log.Println("shutting down job-worker...")
		jobWorker.Close()
		jobWorker.AwaitClose()
		log.Println("job-worker shutting down completed")
		os.Exit(0)
	}()
	select {}
}

func handleJob(client worker.JobClient, job entities.Job) {
	tracer := otel.Tracer("otel-job-worker-trace")
	ctx, span := tracer.Start(context.Background(), "otel-job-worker-span")
	defer span.End()
	traceId := span.SpanContext().TraceID().String()
	spanId :=  span.SpanContext().SpanID().String()
	log.Printf("[trace-id: %s span-id: %s] starting job %d", traceId, spanId, job.GetKey())
	time.Sleep(100 * time.Millisecond) // simulating the worker is doing something...
	request, err := client.NewCompleteJobCommand().JobKey(job.GetKey()).VariablesFromMap(nil)
	if err != nil {
		log.Printf("trace-id: %s span-id: %s] job %d failed", traceId, spanId, job.GetKey())
		_, err := client.NewFailJobCommand().JobKey(job.GetKey()).Retries(job.Retries - 1).Send(ctx)
				if err != nil {
			panic(any(err))
		}
		return
	}
	_, err = request.Send(ctx)
	if err != nil {
		panic(any(err))
	}
	log.Printf("[trace-id: %s span-id: %s] job %d completed successfully", traceId, spanId, job.GetKey())
}

func initTracer() (*trace.TracerProvider, error) {
	exp, err := jaeger.New(jaeger.WithCollectorEndpoint(jaeger.WithEndpoint("http://jaeger:14268/api/traces")))
	if err != nil {
		return nil, err
	}
	tp := trace.NewTracerProvider(
		trace.WithBatcher(exp),
		trace.WithResource(resource.NewWithAttributes(
			semconv.SchemaURL,
			semconv.ServiceNameKey.String("otel-job-worker"),
		)),
	)
	otel.SetTracerProvider(tp)
	return tp, nil
}
