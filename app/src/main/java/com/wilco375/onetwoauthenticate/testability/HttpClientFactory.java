/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 * Modified Copyright 2018 Wilco van Beijnum.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wilco375.onetwoauthenticate.testability;

import android.content.Context;
import android.net.http.AndroidHttpClient;

import org.apache.http.client.HttpClient;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Factory that creates an {@link HttpClient}.
 *
 * @author klyubin@google.com (Alex Klyubin)
 */
final class HttpClientFactory {

    /**
     * Timeout (ms) for establishing a connection.
     */
    // @VisibleForTesting
    static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 20 * 1000;

    /**
     * Timeout (ms) for read operations on connections.
     */
    // @VisibleForTesting
    static final int DEFAULT_READ_TIMEOUT_MILLIS = 20 * 1000;

    /**
     * Timeout (ms) for obtaining a connection from the connection pool.
     */
    // @VisibleForTesting
    static final int DEFAULT_GET_CONNECTION_FROM_POOL_TIMEOUT_MILLIS = 20 * 1000;

    /**
     * Hidden constructor to prevent instantiation.
     */
    private HttpClientFactory() {
    }

    /**
     * Creates a new {@link HttpClient}.
     *
     * @param context context for reusing SSL sessions.
     */
    static HttpClient createHttpClient(Context context) {
        try {
            HttpClient client = AndroidHttpClient.newInstance(null, context);

            configureHttpClient(client);
            return client;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create HttpClient", e);
        }
    }

    private static void configureHttpClient(HttpClient httpClient) {
        HttpParams params = httpClient.getParams();
        HttpConnectionParams.setStaleCheckingEnabled(params, false);
        HttpConnectionParams.setConnectionTimeout(params, DEFAULT_CONNECT_TIMEOUT_MILLIS);
        HttpConnectionParams.setSoTimeout(params, DEFAULT_READ_TIMEOUT_MILLIS);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        ConnManagerParams.setTimeout(params, DEFAULT_GET_CONNECTION_FROM_POOL_TIMEOUT_MILLIS);

        // Don't handle redirects automatically
        HttpClientParams.setRedirecting(params, false);

        // Don't handle authentication automatically
        HttpClientParams.setAuthenticating(params, false);
    }

    /**
     * Creates a new instance of {@code HttpClient} for Eclair where we can't use
     * {@code android.net.http.AndroidHttpClient}.
     */
    private static HttpClient createHttpClientForEclair() {
        // IMPLEMENTATION NOTE: Since AndroidHttpClient is not available on Eclair, we create a
        // DefaultHttpClient with a configuration similar to that of AndroidHttpClient.
        return new DefaultHttpClient(new BasicHttpParams());
    }
}
