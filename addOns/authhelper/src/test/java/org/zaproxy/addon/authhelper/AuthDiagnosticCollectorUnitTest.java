/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2023 The ZAP Development Team
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
package org.zaproxy.addon.authhelper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.parosproxy.paros.network.HttpBody;
import org.parosproxy.paros.network.HttpHeader;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.network.HttpRequestHeader;
import org.parosproxy.paros.network.HttpResponseHeader;
import org.zaproxy.zap.network.HttpRequestBody;
import org.zaproxy.zap.network.HttpResponseBody;
import org.zaproxy.zap.testutils.TestUtils;

class AuthDiagnosticCollectorUnitTest extends TestUtils {

    private AuthDiagnosticCollector adc;
    private StringBuilder sb;

    @BeforeEach
    void setUp() throws Exception {
        adc = new AuthDiagnosticCollector();
        sb = new StringBuilder();
        adc.setCollector(str -> sb.append(str));
    }

    @Test
    void shouldLogMsgIfEnabled() throws Exception {
        // Given
        adc.setEnabled(true);

        HttpMessage msg =
                new HttpMessage(
                        new HttpRequestHeader(),
                        new HttpRequestBody(),
                        new HttpResponseHeader(),
                        new HttpResponseBody());
        msg.getRequestHeader().setMethod("GET");
        msg.getRequestHeader().setURI(new URI("https://www.example.com", true));
        msg.getResponseHeader().setStatusCode(200);
        msg.getResponseHeader().setReasonPhrase("OK");

        // When
        adc.onHttpResponseReceive(msg, 1, null);

        // Then
        assertThat(sb.toString(), is(">>>>>\nGET https://example0/\n<<<\nHTTP/1.0 200 OK\n"));
    }

    @Test
    void shouldNotLogMsgIfDisabled() throws Exception {
        // Given / When
        adc.onHttpResponseReceive(new HttpMessage(), 1, null);

        // Then
        assertThat(sb.length(), is(equalTo(0)));
    }

    @Test
    void shouldLogStringIfEnabled() {
        // Given
        adc.setEnabled(true);

        // When
        adc.logString("test");

        // Then
        assertThat(sb.toString(), is("test"));
    }

    @Test
    void shouldNotLogStringIfDisabled() {
        // Given / When
        adc.logString("test");

        // Then
        assertThat(sb.length(), is(equalTo(0)));
    }

    @Test
    void shouldAppendCookies() throws Exception {
        // Given
        List<HttpCookie> cookies = new ArrayList<>();
        cookies.add(new HttpCookie("name", "someValue"));
        HttpCookie c = new HttpCookie("name2", "someValue2");
        c.setDiscard(true);
        c.setDomain("testDomain");
        c.setHttpOnly(true);
        c.setSecure(true);
        cookies.add(c);

        // When
        adc.appendCookies(cookies, "TestHeader", sb);

        // Then
        assertThat(
                sb.toString(),
                is(
                        equalTo(
                                "TestHeader: name=\"token0\"\nTestHeader: name2=\"token1\";$Domain=\"https://example0/\"\n")));
    }

    @Test
    void shouldAppendStructuredData() throws Exception {
        // Given
        StringBuilder sb = new StringBuilder();
        HttpHeader header = new HttpRequestHeader();
        HttpBody body = new HttpRequestBody();

        header.setHeader(HttpHeader.CONTENT_TYPE, "somethingJsonSomething");
        body.setBody(
                "[{\"user\":\"test@test.com\",\"password\":\"password123\"},{\"xxx\":\"yyy\"}]");

        // When
        adc.appendStructuredData(header, body, sb);

        // Then
        assertThat(
                sb.toString(),
                is(
                        equalTo(
                                "\n[{\"user\":\"token0\",\"password\":\"token1\"},{\"xxx\":\"token2\"}]\n")));
    }

    @Test
    void shouldNotAppendNonJsonData() throws Exception {
        // Given
        HttpHeader header = new HttpRequestHeader();
        HttpBody body = new HttpRequestBody();

        header.setHeader(HttpHeader.CONTENT_TYPE, "something");
        body.setBody(
                "[{\"user\":\"test@test.com\",\"password\":\"password123\"},{\"xxx\":\"yyy\"}]");

        // When
        adc.appendStructuredData(header, body, sb);

        // Then
        assertThat(sb.length(), is(equalTo(0)));
    }

    @Test
    void shouldAppendExactHeaders() throws Exception {
        // Given
        HttpHeader header = new HttpRequestHeader();

        header.addHeader("ThisHeader", "realValue1");
        header.addHeader("ThisHeader", "realValue2");
        header.addHeader("NotThisHeader", "realValue3");

        // When
        adc.appendExactHeaders(header, "ThisHeader", sb);

        // Then
        assertThat(sb.toString(), is(equalTo("ThisHeader: realValue1\nThisHeader: realValue2\n")));
    }

    @Test
    void shouldAppendSanitisedHeaders() throws Exception {
        // Given
        HttpHeader header = new HttpRequestHeader();

        header.addHeader("ThisHeader", "realValue1");
        header.addHeader("ThisHeader", "realValue2");
        header.addHeader("NotThisHeader", "realValue3");

        // When
        adc.appendSanitisedHeaders(header, "ThisHeader", sb);

        // Then
        assertThat(sb.toString(), is(equalTo("ThisHeader: token0\nThisHeader: token1\n")));
    }

    @Test
    void shouldReturnSanitisedSimpleJsonObject() throws Exception {
        // Given
        String jsonStr = "{\"user\":\"test@test.com\",\"password\":\"password123\"}";
        JSONObject json = JSONObject.fromObject(jsonStr);

        // When
        Object res = adc.sanitiseJson(json);

        // Then
        assertThat(res.toString(), is(equalTo("{\"user\":\"token0\",\"password\":\"token1\"}")));
    }

    @Test
    void shouldReturnSanitisedSimpleJsonArray() throws Exception {
        // Given
        String jsonStr = "[{\"user\":\"test@test.com\",\"password\":\"password123\"}]";
        JSONArray json = JSONArray.fromObject(jsonStr);

        // When
        Object res = adc.sanitiseJson(json);

        // Then
        assertThat(res.toString(), is(equalTo("[{\"user\":\"token0\",\"password\":\"token1\"}]")));
    }
}
