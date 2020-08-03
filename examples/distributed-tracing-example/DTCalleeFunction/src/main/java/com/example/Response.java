/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example;

import java.util.HashMap;
import java.util.Map;

public class Response {
    private int statusCode;
    private String body;

    Response(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    Map<String, Object> getMap() {
        return new HashMap<String, Object>() {{
            put("statusCode", statusCode);
            put("body", body);
        }};
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
