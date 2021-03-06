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
package io.cryostat.commands.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.cryostat.commands.Command;
import io.cryostat.core.RecordingOptionsCustomizer;
import io.cryostat.core.tui.ClientWriter;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordingOptionsCustomizerCommandTest {

    RecordingOptionsCustomizerCommand command;
    @Mock ClientWriter cw;
    @Mock RecordingOptionsCustomizer customizer;

    @BeforeEach
    void setup() {
        command = new RecordingOptionsCustomizerCommand(cw, customizer);
    }

    @Test
    void shouldBeNamedRecordingOptions() {
        MatcherAssert.assertThat(command.getName(), Matchers.equalTo("recording-option"));
    }

    @Test
    void shouldBeAvailable() {
        Assertions.assertTrue(command.isAvailable());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2, 3})
    void shouldNotValidateIncorrectArgc(int argc) {
        Exception e =
                assertThrows(
                        FailedValidationException.class, () -> command.validate(new String[argc]));
        String errorMessage = "Expected one argument: recording option name";
        verify(cw).println(errorMessage);
        MatcherAssert.assertThat(e.getMessage(), Matchers.equalTo(errorMessage));
    }

    @Test
    void shouldNotValidateNullArg() {
        Exception e =
                Assertions.assertThrows(
                        FailedValidationException.class,
                        () -> command.validate(new String[] {null}));
        String errorMessage = "One or more arguments were null";
        Mockito.verify(cw).println(errorMessage);
        MatcherAssert.assertThat(e.getMessage(), Matchers.equalTo(errorMessage));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "foo",
                "+foo",
                "foo=",
                "-foo=bar",
            })
    void shouldNotValidateMalformedArg(String arg) {
        Exception e =
                assertThrows(
                        FailedValidationException.class,
                        () -> command.validate(new String[] {arg}));
        String errorMessage = " is an invalid option string";
        verify(cw).println(arg + errorMessage);
        MatcherAssert.assertThat(e.getMessage(), Matchers.equalTo(arg + errorMessage));
    }

    @Test
    void shouldNotValidateUnrecognizedOption() {
        Exception e =
                assertThrows(
                        FailedValidationException.class,
                        () -> command.validate(new String[] {"someUnknownOption=value"}));
        String errorMessage = "someUnknownOption is an unrecognized or unsupported option";
        verify(cw).println(errorMessage);
        MatcherAssert.assertThat(e.getMessage(), Matchers.equalTo(errorMessage));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "toDisk=true",
                "maxAge=10",
                "maxSize=512",
            })
    void shouldKnownValidateKeyValueArg(String arg) {
        assertDoesNotThrow(() -> command.validate(new String[] {arg}));
        verifyNoInteractions(cw);
    }

    @Test
    void shouldExpectUnsetArg() {
        assertDoesNotThrow(() -> command.validate(new String[] {"-toDisk"}));
        verifyNoInteractions(cw);
    }

    @Test
    void shouldSetMaxAge() throws Exception {
        verifyNoInteractions(customizer);
        command.execute(new String[] {"maxAge=123"});
        verify(customizer).set(RecordingOptionsCustomizer.OptionKey.MAX_AGE, "123");
        verifyNoMoreInteractions(customizer);
        verifyNoInteractions(cw);
    }

    @Test
    void shouldSetMaxSize() throws Exception {
        verifyNoInteractions(customizer);
        command.execute(new String[] {"maxSize=123"});
        verify(customizer).set(RecordingOptionsCustomizer.OptionKey.MAX_SIZE, "123");
        verifyNoMoreInteractions(customizer);
        verifyNoInteractions(cw);
    }

    @Test
    void shouldSetToDisk() throws Exception {
        verifyNoInteractions(customizer);
        command.execute(new String[] {"toDisk=true"});
        verify(customizer).set(RecordingOptionsCustomizer.OptionKey.TO_DISK, "true");
        verifyNoMoreInteractions(customizer);
        verifyNoInteractions(cw);
    }

    @Test
    void shouldUnsetMaxAge() throws Exception {
        verifyNoInteractions(customizer);
        command.execute(new String[] {"-maxAge"});
        verify(customizer).unset(RecordingOptionsCustomizer.OptionKey.MAX_AGE);
        verifyNoMoreInteractions(customizer);
        verifyNoInteractions(cw);
    }

    @Test
    void shouldUnsetMaxSize() throws Exception {
        verifyNoInteractions(customizer);
        command.execute(new String[] {"-maxSize"});
        verify(customizer).unset(RecordingOptionsCustomizer.OptionKey.MAX_SIZE);
        verifyNoMoreInteractions(customizer);
        verifyNoInteractions(cw);
    }

    @Test
    void shouldUnsetToDisk() throws Exception {
        verifyNoInteractions(customizer);
        command.execute(new String[] {"-toDisk"});
        verify(customizer).unset(RecordingOptionsCustomizer.OptionKey.TO_DISK);
        verifyNoMoreInteractions(customizer);
        verifyNoInteractions(cw);
    }

    @Test
    void shouldReturnSuccessOutput() throws Exception {
        verifyNoInteractions(customizer);
        Command.Output<?> out = command.execute(new String[] {"toDisk=true"});
        MatcherAssert.assertThat(out, Matchers.instanceOf(Command.SuccessOutput.class));
        verify(customizer).set(RecordingOptionsCustomizer.OptionKey.TO_DISK, "true");
        verifyNoMoreInteractions(customizer);
        verifyNoInteractions(cw);
    }

    @Test
    void shouldReturnExceptionOutput() throws Exception {
        verifyNoInteractions(customizer);
        doThrow(NullPointerException.class).when(customizer).set(Mockito.any(), Mockito.any());
        Command.Output<?> out = command.execute(new String[] {"toDisk=true"});
        MatcherAssert.assertThat(out, Matchers.instanceOf(Command.ExceptionOutput.class));
        MatcherAssert.assertThat(out.getPayload(), Matchers.equalTo("NullPointerException: "));
        verify(customizer).set(RecordingOptionsCustomizer.OptionKey.TO_DISK, "true");
        verifyNoMoreInteractions(customizer);
    }
}
