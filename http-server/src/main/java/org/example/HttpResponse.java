package org.example;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class HttpResponse {
    private final String statusLine;
    private final Map<String, String> headers;
    private final String body;

    private HttpResponse(String statusLine, Map<String, String> headers, String body) {
        this.statusLine = statusLine;
        this.headers = headers;
        this.body = body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(statusLine).append("\r\n");

        // Automatische Standard-Header
        headers.putIfAbsent("Date", getServerTime());
        headers.putIfAbsent("Server", "JavaHttpServer/1.0");
        headers.putIfAbsent("Connection", "close");
        headers.put("Content-Length", String.valueOf(body.getBytes().length));

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }

        sb.append("\r\n").append(body);
        return sb.toString();
    }

    private String getServerTime() {
        return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).format(new Date());
    }

    public static class Builder {
        private int statusCode;
        private String statusText;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private String body = "";

        public Builder status(int code, String text) {
            this.statusCode = code;
            this.statusText = text;
            return this;
        }

        public Builder header(String key, String value) {
            headers.put(key, value);
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public HttpResponse build() {
            String statusLine = "HTTP/1.1 " + statusCode + " " + statusText;
            return new HttpResponse(statusLine, headers, body);
        }
    }
}
