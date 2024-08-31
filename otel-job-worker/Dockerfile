# syntax=docker/dockerfile:1
FROM golang:1.21-alpine
WORKDIR /go/src/job-worker-go
COPY . .
RUN go mod download
RUN go build -o /go/bin/jobworker .
ENTRYPOINT ["/go/bin/jobworker"]
