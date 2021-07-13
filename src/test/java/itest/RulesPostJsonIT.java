/*
 * Copyright The Cryostat Authors
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or data
 * (collectively the "Software"), free of charge and under any and all copyright
 * rights in the Software, and any and all patent rights owned or freely
 * licensable by each licensor hereunder covering either (i) the unmodified
 * Software as contributed to or provided by such licensor, or (ii) the Larger
 * Works (as defined below), to deal in both
 *
 * (a) the Software, and
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software (each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 * The above copyright notice and either this complete permission notice or at
 * a minimum a reference to the UPL must be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package itest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.cryostat.net.web.http.HttpMimeType;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import itest.bases.ExternalTargetsTest;
import itest.util.Podman;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
class RulesPostJsonIT extends ExternalTargetsTest {

    static final List<String> CONTAINERS = new ArrayList<>();
    static final Map<String, String> NULL_RESULT = new HashMap<>();

    final String jmxServiceUrl =
            String.format("service:jmx:rmi:///jndi/rmi://%s:9093/jmxrmi", Podman.POD_NAME);
    final String jmxServiceUrlEncoded = jmxServiceUrl.replaceAll("/", "%2F");

    static {
        NULL_RESULT.put("result", null);
    }

    static JsonObject testRule;

    @BeforeAll
    static void setup() throws Exception {
        testRule = new JsonObject();
        testRule.put("name", "Test_Rule");
        testRule.put("description", "AutoRulesIT automated rule");
        testRule.put("eventSpecifier", "template=Continuous,type=TARGET");
        testRule.put(
                "matchExpression",
                "target.annotations.cryostat.JAVA_MAIN=='es.andrewazor.demo.Main'");
    }

    @AfterAll
    static void cleanup() throws Exception {
        for (String id : CONTAINERS) {
            Podman.kill(id);
        }
    }

    @Test
    @Order(1)
    void testAddRuleThrowsWhenJsonAttributesNull() throws Exception {
        CompletableFuture<JsonObject> postResponse = new CompletableFuture<>();

        webClient
                .post("/api/v2/rules")
                .putHeader(HttpHeaders.CONTENT_TYPE.toString(), HttpMimeType.JSON.mime())
                .sendJsonObject(
                        null,
                        ar -> {
                            if (assertRequestStatus(ar, postResponse)) {
                                MatcherAssert.assertThat(
                                        ar.result().statusCode(), Matchers.equalTo(400));
                            }
                        });
    }

    @Test
    @Order(2)
    void testAddRuleThrowsWhenMimeNull() throws Exception {
        CompletableFuture<JsonObject> postResponse = new CompletableFuture<>();

        webClient
                .post("/api/v2/rules")
                .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "")
                .sendJsonObject(
                        testRule,
                        ar -> {
                            if (assertRequestStatus(ar, postResponse)) {
                                MatcherAssert.assertThat(
                                        ar.result().statusCode(), Matchers.equalTo(415));
                            }
                        });
    }

    @Test
    @Order(3)
    void testAddRuleThrowsWhenMimeUnsupported() throws Exception {
        CompletableFuture<JsonObject> postResponse = new CompletableFuture<>();

        webClient
                .post("/api/v2/rules")
                .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "NOTAMIME;text/plain")
                .sendJsonObject(
                        testRule,
                        ar -> {
                            if (assertRequestStatus(ar, postResponse)) {
                                MatcherAssert.assertThat(
                                        ar.result().statusCode(), Matchers.equalTo(415));
                            }
                        });
    }

    @Test
    @Order(4)
    void testAddRuleThrowsWhenMimeInvalid() throws Exception {
        CompletableFuture<JsonObject> postResponse = new CompletableFuture<>();

        webClient
                .post("/api/v2/rules")
                .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "NOTAMIME")
                .sendJsonObject(
                        testRule,
                        ar -> {
                            if (assertRequestStatus(ar, postResponse)) {
                                MatcherAssert.assertThat(
                                        ar.result().statusCode(), Matchers.equalTo(415));
                            }
                        });
    }

    @Test
    @Order(5)
    void testAddRuleThrowsWhenRuleNameAlreadyExists() throws Exception {
        CompletableFuture<JsonObject> postResponse = new CompletableFuture<>();

        webClient
                .post("/api/v2/rules")
                .putHeader(HttpHeaders.CONTENT_TYPE.toString(), HttpMimeType.JSON.mime())
                .sendJsonObject(
                        testRule,
                        ar -> {
                            if (assertRequestStatus(ar, postResponse)) {
                                MatcherAssert.assertThat(
                                        ar.result().statusCode(), Matchers.equalTo(201));
                            }
                        });

        webClient
                .post("/api/v2/rules")
                .putHeader(HttpHeaders.CONTENT_TYPE.toString(), HttpMimeType.JSON.mime())
                .sendJsonObject(
                        testRule,
                        ar -> {
                            if (assertRequestStatus(ar, postResponse)) {
                                MatcherAssert.assertThat(
                                        ar.result().statusCode(), Matchers.equalTo(409));
                            }
                        });
    }
}
