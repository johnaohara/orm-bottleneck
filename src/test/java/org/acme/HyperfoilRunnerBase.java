package org.acme;

import io.hyperfoil.api.config.Benchmark;
import io.hyperfoil.api.config.BenchmarkData;
import io.hyperfoil.api.statistics.StatisticsSnapshot;
import io.hyperfoil.core.impl.LocalBenchmarkData;
import io.hyperfoil.core.impl.LocalSimulationRunner;
import io.hyperfoil.core.parser.BenchmarkParser;
import io.hyperfoil.core.parser.ParserException;
import io.hyperfoil.core.util.Util;
import io.hyperfoil.maven.RunMojo;
import io.quarkus.runtime.test.TestHttpEndpointProvider;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;

public class HyperfoilRunnerBase  {

    private static final Logger log;


    //    @Parameter(defaultValue = "false", property = "hyperfoil.percentiles")
    private Boolean outputPercentileDistribution = false;

    private Benchmark benchmark;

    static {
        System.setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, "io.vertx.core.logging.Log4j2LogDelegateFactory");
        log = LoggerFactory.getLogger(RunMojo.class);
    }


    public void runHyperfoilTest(String benchmarkFile) throws MojoExecutionException, URISyntaxException {

        URL dir_url = ClassLoader.getSystemResource(benchmarkFile);

        File yaml = new File(dir_url.toURI());

        HashMap<String, StatisticsSnapshot> total = new HashMap<>();

        try {
            benchmark = buildBenchmark(new FileInputStream(yaml), yaml.toPath());

            if (benchmark != null) {
                // We want to log all stats in the same thread to not break the output layout too much.
                LocalSimulationRunner runner = new LocalSimulationRunner(benchmark, (phase, stepId, metric, snapshot, ignored) -> {
                    snapshot.addInto(total.computeIfAbsent(phase.name() + "/" + metric, k -> new StatisticsSnapshot()));
                }, this::printSessionPoolInfo);
                log.info("Running for {}", benchmark.statisticsCollectionPeriod());
                log.info("{} threads", benchmark.defaultThreads());
                runner.run();
            }
        } catch (FileNotFoundException | MojoFailureException e) {
            log.error("Couldn't find yaml file: {}", e, yaml);
            throw new MojoExecutionException("yaml not found: " + yaml.toPath());
        }
        total.forEach(this::printStats);


    }

    private void printSessionPoolInfo(String phase, int min, int max) {
        log.info("Phase {} used {} - {} sessions.", phase, min, max);
    }


    private Benchmark buildBenchmark(InputStream inputStream, Path path) throws MojoFailureException {
        if (inputStream == null)
            log.error("Could not find benchmark configuration");

        try {
            String source = Util.toString(inputStream);
            Benchmark benchmark = BenchmarkParser.instance().buildBenchmark(source, new LocalBenchmarkData(path));

            if (benchmark == null)
                log.info("Failed to parse benchmark configuration");

            return benchmark;
        } catch (ParserException | IOException e) {
            log.error("Error occurred during parsing", e);
            throw new MojoFailureException("Error occurred during parsing: " + e.getMessage(), e);
        }
    }

    private void printStats(String phaseAndMetric, StatisticsSnapshot stats) {
        double durationSeconds = (stats.histogram.getEndTimeStamp() - stats.histogram.getStartTimeStamp()) / 1000d;
        log.info("{}: ", phaseAndMetric);
        log.info("{} requests in {} s, ", stats.histogram.getTotalCount(), durationSeconds);
        log.info("                  Avg     Stdev       Max");
        log.info("Latency:    {} {} {}", Util.prettyPrintNanosFixed((long) stats.histogram.getMean()),
                Util.prettyPrintNanosFixed((long) stats.histogram.getStdDeviation()), Util.prettyPrintNanosFixed(stats.histogram.getMaxValue()));
        log.info("Requests/sec: {}", String.format("%.2f", stats.histogram.getTotalCount() / durationSeconds));

        if (outputPercentileDistribution) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                stats.histogram.outputPercentileDistribution(new PrintStream(baos, true, "UTF-8"), 1000.00);
                String data = new String(baos.toByteArray(), StandardCharsets.UTF_8);

                log.info("\nPercentile Distribution\n\n" + data);
            } catch (UnsupportedEncodingException e) {
                log.error("Could not write Percentile Distribution to log");
            }
        }

        if (stats.errors() > 0) {
            log.info("Socket errors: connect {}, reset {}, timeout {}", stats.connectFailureCount, stats.resetCount, stats.timeouts);
            log.info("Non-2xx or 3xx responses: {}", stats.status_4xx + stats.status_5xx + stats.status_other);
        }
    }


}
