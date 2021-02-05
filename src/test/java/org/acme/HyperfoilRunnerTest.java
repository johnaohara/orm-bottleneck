package org.acme;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class HyperfoilRunnerTest extends HyperfoilRunnerBase {

    @Test
    public void testSingleRequest() throws URISyntaxException, MojoExecutionException {
        this.runHyperfoilTest("single-request.hf.yaml");
    }

    @Test
    public void testLoad() throws URISyntaxException, MojoExecutionException {
        this.runHyperfoilTest("generate-load.hf.yaml");
    }

}
