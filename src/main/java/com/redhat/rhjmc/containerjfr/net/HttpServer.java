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
package com.redhat.rhjmc.containerjfr.net;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;

import com.redhat.rhjmc.containerjfr.core.log.Logger;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;

public class HttpServer {

    private final NetworkConfiguration netConf;
    private final SslConfiguration sslConf;
    private final Logger logger;

    private final HandlerDelegate<HttpServerRequest> requestHandlerDelegate =
            new HandlerDelegate<>();
    private final HandlerDelegate<ServerWebSocket> websocketHandlerDelegate =
            new HandlerDelegate<>();

    private final io.vertx.core.http.HttpServer server;

    HttpServer(Vertx vertx, NetworkConfiguration netConf, SslConfiguration sslConf, Logger logger) {
        this.netConf = netConf;
        this.sslConf = sslConf;
        this.logger = logger;
        this.server =
                vertx.createHttpServer(
                        sslConf.applyToHttpServerOptions(
                                new HttpServerOptions()
                                        .setPort(
                                                isSsl()
                                                        ? netConf.getInternalWebServerPrimaryPort()
                                                        : netConf
                                                                .getInternalWebServerSecondaryPort())
                                        .addWebSocketSubProtocol("*")
                                        .setCompressionSupported(true)
                                        .setLogActivity(true)));

        if (!sslConf.enabled()) {
            this.logger.warn("No available SSL certificates. Fallback to plain HTTP.");
        }
    }

    public void start(Promise<Void> promise) throws SocketException, UnknownHostException {
        this.server
                .requestHandler(requestHandlerDelegate)
                .webSocketHandler(websocketHandlerDelegate)
                .listen(
                        res -> {
                            if (res.failed()) {
                                logger.error(new RuntimeException(res.cause()));
                                promise.fail(res.cause());
                                return;
                            }
                            try {
                                logger.info(
                                        String.format(
                                                "%s service running on %s://%s:%d",
                                                isSsl() ? "HTTPS" : "HTTP",
                                                isSsl() ? "https" : "http",
                                                netConf.getWebServerHost(),
                                                isSsl()
                                                        ? netConf.getExternalWebServerPrimaryPort()
                                                        : netConf
                                                                .getExternalWebServerSecondaryPort()));
                            } catch (Exception e) {
                                logger.error(e);
                                promise.fail(e);
                                return;
                            }
                            promise.complete();
                        });
    }

    public void stop() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        this.server.close(
                res -> {
                    if (res.failed()) {
                        future.completeExceptionally(res.cause());
                        return;
                    }

                    future.complete(null);
                });
        future.join(); // wait for vertx to be closed
    }

    public boolean isSsl() {
        return netConf.isSslProxied() || sslConf.enabled();
    }

    public void requestHandler(Handler<HttpServerRequest> handler) {
        requestHandlerDelegate.handler(handler);
    }

    public void websocketHandler(Handler<ServerWebSocket> handler) {
        websocketHandlerDelegate.handler(handler);
    }

    private static class HandlerDelegate<T> implements Handler<T> {

        private Handler<T> mHandler;

        @Override
        public final void handle(T event) {
            if (mHandler != null) {
                mHandler.handle(event);
            }
        }

        public HandlerDelegate<T> handler(Handler<T> handler) {
            mHandler = handler;

            return this;
        }
    }
}
