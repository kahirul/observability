package co.paikama.obs

import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.annotation.Observed
import io.micrometer.observation.aop.ObservedAspect
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.semconv.ResourceAttributes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate


@SpringBootApplication
@RestController
class ObsApplication {
    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder.rootUri("https://httpbin.org").build()
    }
}

fun main(args: Array<String>) {
    runApplication<ObsApplication>(*args)
}

@RestController
class ObsController(val obsService: ObsService) {
    companion object {
        val LOG: Logger = LoggerFactory.getLogger(ObsController::class.java)
    }

    @GetMapping("/hello")
    fun hello(): Any {
        LOG.info("Calling hello service")

        return obsService.hello()
    }
}

@Service
class ObsService(val restTemplate: RestTemplate) {
    companion object {
        val LOG: Logger = LoggerFactory.getLogger(ObsService::class.java)
    }

    @Observed
    fun hello(): Any {
        LOG.info("Calling remote API")

        return restTemplate.postForEntity("/post", "Hello, Cloud!", Any::class.java)
    }

}

@Configuration
class ObservabilityConfig {

    @Bean
    fun observedAspect(observationRegistry: ObservationRegistry): ObservedAspect {
        return ObservedAspect(observationRegistry)
    }
}

@Configuration
class OpenTelemetryConfig {
    @Bean
    fun openTelemetry(
        sdkLoggerProvider: SdkLoggerProvider,
        sdkTracerProvider: SdkTracerProvider,
        contextPropagators: ContextPropagators,
    ): OpenTelemetry {
        val openTelemetrySdk = OpenTelemetrySdk.builder()
            .setLoggerProvider(sdkLoggerProvider)
            .setTracerProvider(sdkTracerProvider)
            .setPropagators(contextPropagators)
            .build()
        OpenTelemetryAppender.install(openTelemetrySdk)
        return openTelemetrySdk
    }

    @Bean
    fun otelSdkLoggerProvider(
        environment: Environment,
        logRecordProcessors: ObjectProvider<LogRecordProcessor>,
    ): SdkLoggerProvider {
        val applicationName = environment.getProperty("spring.application.name", "Obs-default")
        val springResource: Resource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, applicationName))
        val builder = SdkLoggerProvider.builder()
            .setResource(Resource.getDefault().merge(springResource))
        logRecordProcessors.orderedStream().forEach { processor: LogRecordProcessor ->
            builder.addLogRecordProcessor(
                processor
            )
        }
        return builder.build()
    }

    @Bean
    fun otelLogRecordProcessor(): LogRecordProcessor {
        return BatchLogRecordProcessor
            .builder(
                OtlpGrpcLogRecordExporter.builder()
                    .setEndpoint("http://localhost:4317")
                    .build()
            )
            .build()
    }
}