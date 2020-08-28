/*-
 * #%L
 * Container JFR
 * %%
 * Copyright (C) 2020 Red Hat, Inc.
 * %%
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
 * #L%
 */
package itest;

import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.ext.web.client.WebClient;

public class IntegrationTestUtils {

    static final boolean EXTRA_DEBUG =
            Boolean.valueOf(System.getProperty("containerJfrITestExtraDebug", "false"));

    static {
        if (EXTRA_DEBUG) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
            System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "true");
        }
    }

    public static final int WEB_PORT;

    static {
        WEB_PORT = Integer.valueOf(System.getProperty("containerJfrWebSecondaryPort"));
    }

    static final HttpClientOptions HTTP_CLIENT_OPTIONS;

    static {
        HTTP_CLIENT_OPTIONS =
                new HttpClientOptions()
                        .setSsl(false)
                        .setTrustAll(true)
                        .setVerifyHost(false)
                        .setDefaultHost("0.0.0.0")
                        .setDefaultPort(WEB_PORT)
                        .setLogActivity(true);
    }

    private static final Vertx VERTX = Vertx.vertx();
    static final HttpClient HTTP_CLIENT = VERTX.createHttpClient(HTTP_CLIENT_OPTIONS);
    private static final WebClient WEB_CLIENT_INSTANCE = WebClient.wrap(HTTP_CLIENT);

    public static WebClient getWebClient() {
        return WEB_CLIENT_INSTANCE;
    }

    public static FileSystem getFileSystem() {
        return VERTX.fileSystem();
    }
}
