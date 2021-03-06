package org.bal.quote.config;


import brave.Tracing;
import brave.context.slf4j.MDCScopeDecorator;
import brave.grpc.GrpcTracing;
import brave.propagation.ThreadLocalCurrentTraceContext;
import io.grpc.ServerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;

@ComponentScan(basePackages = {"org.bal.quote.server", "org.bal.quote.repository"})
@SpringBootConfiguration
public class Configuration {

    @Value("${zipkin-server.host}")
    private String zipkinHost;

    @Value("${zipkin-server.port}")
    private int zipkinPort;


    /**
     * Configuration for how to buffer spans into messages for Zipkin
     */
    @Bean
    public Reporter<Span> reporter() {
        return AsyncReporter.builder(sender()).build();
    }

    @Bean
    public Tracing tracing() {
        return Tracing.newBuilder()
                .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder()
                        .addScopeDecorator(MDCScopeDecorator.create())
                        .build())
                .localServiceName("quote-service")
                .spanReporter(reporter()).build();
    }

    /**
     * Configuration for how to send spans to Zipkin
     */
    @Bean
    public Sender sender() {
        return OkHttpSender.create("http://" + zipkinHost + ":" + zipkinPort + "/api/v2/spans");
    }

    @Bean
    public GrpcTracing grpcTracing() {
        return GrpcTracing.create(tracing());
    }

    @Bean(name = "grpcServerInterceptor")
    public ServerInterceptor grpcServerInterceptor() {

        return grpcTracing().newServerInterceptor();
    }


}
