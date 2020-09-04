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
package com.redhat.rhjmc.containerjfr.net.web.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;

import com.google.gson.Gson;

import com.redhat.rhjmc.containerjfr.commands.internal.RecordingOptionsBuilderFactory;
import com.redhat.rhjmc.containerjfr.net.AuthManager;
import com.redhat.rhjmc.containerjfr.net.TargetConnectionManager;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

public class RecordingOptionsGetHandler extends AbstractAuthenticatedRequestHandler {

    static final String PATH = "/api/v1/targets/:targetId/recordingOptions";
    protected final TargetConnectionManager connectionManager;
    private final RecordingOptionsBuilderFactory recordingOptionsBuilderFactory;
    private final Gson gson;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    @Inject
    RecordingOptionsGetHandler(
            AuthManager auth,
            TargetConnectionManager connectionManager,
            RecordingOptionsBuilderFactory recordingOptionsBuilderFactory,
            Gson gson) {
        super(auth);
        this.connectionManager = connectionManager;
        this.recordingOptionsBuilderFactory = recordingOptionsBuilderFactory;
        this.gson = gson;
    }

    @Override
    public HttpMethod httpMethod() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return PATH;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    void handleAuthenticated(RoutingContext ctx) throws Exception {
        Map<String, String> optionMap =
                connectionManager.executeConnectedTask(
                        getConnectionDescriptorFromContext(ctx),
                        connection -> {
                            RecordingOptionsBuilder builder =
                                    recordingOptionsBuilderFactory.create(connection.getService());
                            IConstrainedMap<String> recordingOptions = builder.build();

                            Map<String, IOptionDescriptor<?>> TargetRecordingOptions =
                                    connection.getService().getAvailableRecordingOptions();

                            Map<String, String> map = new HashMap<String, String>();

                            if (recordingOptions.get("toDisk") != null) {
                                map.put("toDisk", recordingOptions.get("toDisk").toString());
                            } else {
                                map.put(
                                        "toDisk",
                                        TargetRecordingOptions.get("disk").getDefault().toString());
                            }

                            String maxAge;
                            if (recordingOptions.get("maxAge") != null) {
                                maxAge = recordingOptions.get("maxAge").toString();
                            } else {
                                maxAge =
                                        TargetRecordingOptions.get("maxAge")
                                                .getDefault()
                                                .toString();
                            }
                            Matcher ageMatcher = NUMBER_PATTERN.matcher(maxAge);
                            map.put("maxAge", ageMatcher.find() ? ageMatcher.group() : null);

                            String maxSize;
                            if (recordingOptions.get("maxSize") != null) {
                                maxSize = recordingOptions.get("maxSize").toString();
                            } else {
                                maxSize =
                                        TargetRecordingOptions.get("maxSize")
                                                .getDefault()
                                                .toString();
                            }
                            Matcher sizeMatcher = NUMBER_PATTERN.matcher(maxSize);
                            map.put("maxSize", sizeMatcher.find() ? sizeMatcher.group() : null);

                            return map;
                        });
        ctx.response().end(gson.toJson(optionMap));
    }
}
