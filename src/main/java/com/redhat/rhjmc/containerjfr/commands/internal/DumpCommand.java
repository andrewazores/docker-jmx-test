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
package com.redhat.rhjmc.containerjfr.commands.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.openjdk.jmc.common.unit.IConstrainedMap;

import com.redhat.rhjmc.containerjfr.commands.SerializableCommand;
import com.redhat.rhjmc.containerjfr.core.net.JFRConnectionToolkit;
import com.redhat.rhjmc.containerjfr.core.tui.ClientWriter;

@Singleton
class DumpCommand extends AbstractRecordingCommand implements SerializableCommand {

    @Inject
    DumpCommand(
            ClientWriter cw,
            JFRConnectionToolkit jfrConnectionToolkit,
            EventOptionsBuilder.Factory eventOptionsBuilderFactory,
            RecordingOptionsBuilderFactory recordingOptionsBuilderFactory) {
        super(cw, jfrConnectionToolkit, eventOptionsBuilderFactory, recordingOptionsBuilderFactory);
    }

    @Override
    public String getName() {
        return "dump";
    }

    /**
     * Three args expected. First argument is recording name, second argument is recording length in
     * seconds. Third argument is comma-separated event options list, ex.
     * jdk.SocketWrite:enabled=true,com.foo:ratio=95.2
     */
    @Override
    public void execute(String[] args) throws Exception {
        String hostId = args[0];
        String name = args[1];
        int seconds = Integer.parseInt(args[2]);
        String events = args[3];

        executeConnectedTask(
                hostId,
                connection -> {
                    if (getDescriptorByName(hostId, name).isPresent()) {
                        cw.println(
                                String.format("Recording with name \"%s\" already exists", name));
                        return null;
                    }

                    IConstrainedMap<String> recordingOptions =
                            recordingOptionsBuilderFactory
                                    .create(connection.getService())
                                    .name(name)
                                    .duration(1000 * seconds)
                                    .build();
                    connection
                            .getService()
                            .start(recordingOptions, enableEvents(connection, events));
                    return null;
                });
    }

    @Override
    public Output<?> serializableExecute(String[] args) {
        try {
            String hostId = args[0];
            String name = args[1];
            int seconds = Integer.parseInt(args[2]);
            String events = args[3];

            return executeConnectedTask(
                    hostId,
                    connection -> {
                        if (getDescriptorByName(hostId, name).isPresent()) {
                            return new FailureOutput(
                                    String.format(
                                            "Recording with name \"%s\" already exists", name));
                        }

                        IConstrainedMap<String> recordingOptions =
                                recordingOptionsBuilderFactory
                                        .create(connection.getService())
                                        .name(name)
                                        .duration(1000 * seconds)
                                        .build();
                        connection
                                .getService()
                                .start(recordingOptions, enableEvents(connection, events));
                        return new SuccessOutput();
                    });
        } catch (Exception e) {
            return new ExceptionOutput(e);
        }
    }

    @Override
    public boolean validate(String[] args) {
        if (args.length != 4) {
            cw.println(
                    "Expected four arguments: target (host:port, ip:port, or JMX service URL), recording name, recording length, and event types");
            return false;
        }

        String hostId = args[0];
        String name = args[1];
        String seconds = args[2];
        String events = args[3];

        boolean isValidHostId = validateHostId(hostId);
        boolean isValidName = validateRecordingName(name);
        boolean isValidDuration = seconds.matches("\\d+");
        boolean isValidEvents = validateEvents(events);

        if (!isValidHostId) {
            cw.println(String.format("%s is an invalid connection specifier", args[0]));
        }

        if (!isValidName) {
            cw.println(String.format("%s is an invalid recording name", name));
        }

        if (!isValidDuration) {
            cw.println(String.format("%s is an invalid recording length", seconds));
        }

        if (!isValidEvents) {
            cw.println(String.format("%s is an invalid events specifier", events));
        }

        return isValidHostId && isValidName && isValidDuration && isValidEvents;
    }
}
