/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.dt;

import com.newrelic.opentracing.logging.Log;
import com.newrelic.opentracing.util.DistributedTraceUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.text.MessageFormat;
import java.util.Base64;

import static com.newrelic.opentracing.util.DistributedTraceUtil.ACCOUNT_ID;
import static com.newrelic.opentracing.util.DistributedTraceUtil.APPLICATION_ID;
import static com.newrelic.opentracing.util.DistributedTraceUtil.APP_PARENT_TYPE;
import static com.newrelic.opentracing.util.DistributedTraceUtil.DATA;
import static com.newrelic.opentracing.util.DistributedTraceUtil.GUID;
import static com.newrelic.opentracing.util.DistributedTraceUtil.PARENT_TYPE;
import static com.newrelic.opentracing.util.DistributedTraceUtil.PRIORITY;
import static com.newrelic.opentracing.util.DistributedTraceUtil.SAMPLED;
import static com.newrelic.opentracing.util.DistributedTraceUtil.TIMESTAMP;
import static com.newrelic.opentracing.util.DistributedTraceUtil.TRACE_ID;
import static com.newrelic.opentracing.util.DistributedTraceUtil.TRUSTED_ACCOUNT_KEY;
import static com.newrelic.opentracing.util.DistributedTraceUtil.TX;
import static com.newrelic.opentracing.util.DistributedTraceUtil.VERSION;
import static java.nio.charset.StandardCharsets.UTF_8;

public class DistributedTracePayloadImpl implements DistributedTracePayload {

    private final long timestamp;
    private final String parentType;
    private final String accountId;
    private final String trustKey;
    private final String applicationId;
    private final String guid;
    private final String traceId;
    private final Float priority;
    private final Boolean sampled;
    private final String txnId;

    static DistributedTracePayloadImpl createDistributedTracePayload(String traceId, String guid, String txnId, float priority) {
        DistributedTracing distributedTraceService = DistributedTracing.getInstance();
        String accountId = distributedTraceService.getAccountId();
        if (accountId == null) {
            Log.getInstance().debug("Not creating distributed trace payload due to null accountId.");
            return null;
        }

        String trustKey = distributedTraceService.getTrustKey();
        String applicationId = distributedTraceService.getApplicationId();
        long timestamp = System.currentTimeMillis();
        boolean sampled = DistributedTraceUtil.isSampledPriority(priority);

        return new DistributedTracePayloadImpl(timestamp, APP_PARENT_TYPE, accountId, trustKey, applicationId, guid, traceId, txnId, priority, sampled);
    }

    private DistributedTracePayloadImpl(long timestamp, String parentType, String accountId, String trustKey, String applicationId, String guid,
            String traceId, String txnId, Float priority, Boolean sampled) {
        this.timestamp = timestamp;
        this.parentType = parentType;
        this.accountId = accountId;
        this.trustKey = trustKey;
        this.applicationId = applicationId;
        this.guid = guid;
        this.txnId = txnId;
        this.traceId = traceId;
        this.priority = priority;
        this.sampled = sampled;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String text() {
        DistributedTracing distributedTraceService = DistributedTracing.getInstance();

        JSONArray catVersion = new JSONArray();
        catVersion.add(distributedTraceService.getMajorSupportedCatVersion());
        catVersion.add(distributedTraceService.getMinorSupportedCatVersion());

        JSONObject payload = new JSONObject();
        payload.put(VERSION, catVersion); // version [major, minor]

        JSONObject data = new JSONObject();
        data.put(TIMESTAMP, timestamp);
        data.put(PARENT_TYPE, parentType);
        data.put(ACCOUNT_ID, accountId);

        if (!accountId.equals(trustKey)) {
            data.put(TRUSTED_ACCOUNT_KEY, trustKey);
        }

        data.put(APPLICATION_ID, applicationId);

        if (guid != null) {
            data.put(GUID, guid);
        }

        data.put(TRACE_ID, traceId);
        data.put(PRIORITY, priority);
        data.put(SAMPLED, sampled);

        if (txnId != null) {
            data.put(TX, txnId);
        }

        payload.put(DATA, data);
        return payload.toJSONString();
    }

    @Override
    public String httpSafe() {
        return Base64.getEncoder().encodeToString(text().getBytes(UTF_8));
    }

    public static DistributedTracePayloadImpl parseDistributedTracePayload(String payload) {
        if (payload == null) {
            Log.getInstance().debug("Incoming distributed trace payload is null.");
            return null;
        }

        if (!payload.trim().isEmpty()) {
            payload = payload.trim();
            char firstChar = payload.charAt(0);
            if (firstChar != '{' && firstChar != '[') {
                // This must be base64 encoded, decode it
                payload = new String(Base64.getDecoder().decode(payload), UTF_8);
            }
        }

        DistributedTracing distributedTraceService = DistributedTracing.getInstance();
        JSONParser parser = new JSONParser();
        try {
            JSONObject object = (JSONObject) parser.parse(payload);

            // ignore payload if major version is higher than our own
            JSONArray version = (JSONArray) object.get(VERSION);
            final Long majorVersion = (Long) version.get(0);
            int majorSupportedVersion = distributedTraceService.getMajorSupportedCatVersion();
            if (majorVersion > majorSupportedVersion) {
                Log.getInstance().debug(MessageFormat.format("Incoming distributed trace payload major version: {0} is newer than supported agent"
                        + " version: {1}. Ignoring payload.", majorVersion, majorSupportedVersion));
                return null;
            }

            JSONObject data = (JSONObject) object.get(DATA);

            // ignore payload if accountId isn't trusted
            String payloadAccountId = (String) data.get(ACCOUNT_ID);

            // ignore payload if isn't trusted
            String payloadTrustedAccountKey = (String) data.get(TRUSTED_ACCOUNT_KEY);
            String trustKey = distributedTraceService.getTrustKey();

            if (payloadAccountId == null) {
                Log.getInstance().debug(MessageFormat.format("Invalid payload {0}. Payload missing accountId.", data));
                return null;
            }

            String applicationId = (String) data.get(APPLICATION_ID);
            if (applicationId == null) {
                Log.getInstance().debug("Incoming distributed trace payload is missing application id.");
                return null;
            }

            // If payload doesn't have a tk, use accountId
            payloadTrustedAccountKey = payloadTrustedAccountKey == null ? payloadAccountId : payloadTrustedAccountKey;

            boolean isTrustedAccountKey = trustKey.equals(payloadTrustedAccountKey);
            if (!isTrustedAccountKey) {
                Log.getInstance().debug(MessageFormat.format("Incoming distributed trace payload trustKey: {0} does not match trusted account key: {1}." +
                        " Ignoring payload.", payloadTrustedAccountKey, trustKey));
                return null;
            }

            long timestamp = (Long) data.get(TIMESTAMP);
            if (timestamp <= 0) {
                Log.getInstance().debug(MessageFormat.format("Invalid payload {0}. Payload missing keys.", data));
                return null;
            }

            String parentType = (String) data.get(PARENT_TYPE);
            if (parentType == null) {
                Log.getInstance().debug("Incoming distributed trace payload is missing type.");
                return null;
            }

            String traceId = (String) data.get(TRACE_ID);
            if (traceId == null) {
                Log.getInstance().debug("Incoming distributed trace payload is missing traceId.");
                return null;
            }

            String guid = (String) data.get(GUID);
            String txnId = (String) data.get(TX);
            if (guid == null && txnId == null) {
                // caller has span events disabled and there's no transaction? they must be using txn-less api, but no spans?
                Log.getInstance().debug("Incoming distributed trace payload is missing traceId.");
                return null;
            }

            Number priorityNumber = (Number) data.get(PRIORITY);
            Float priority = priorityNumber != null ? priorityNumber.floatValue() : null;
            Boolean sampled = (Boolean) data.get(SAMPLED);

            DistributedTracePayloadImpl distributedTracePayload = new DistributedTracePayloadImpl(timestamp, parentType,
                    payloadAccountId, payloadTrustedAccountKey, applicationId, guid, traceId, txnId, priority, sampled);

            Log.getInstance().debug("Parsed inbound payload: " + distributedTracePayload);
            return distributedTracePayload;
        } catch (Exception e) {
            Log.getInstance().debug("Failed to parse distributed trace payload.");
            Log.getInstance().debug(e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        return "DistributedTracePayloadImpl{" +
                "timestamp=" + timestamp +
                ", parentType='" + parentType + '\'' +
                ", accountId='" + accountId + '\'' +
                ", trustKey='" + trustKey + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", guid='" + guid + '\'' +
                ", traceId='" + traceId + '\'' +
                ", txnId='" + txnId + '\'' +
                ", sampled=" + sampled +
                ", priority=" + priority +
                '}';
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getParentType() {
        return parentType;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getTrustKey() {
        return trustKey;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getGuid() {
        return guid;
    }

    public String getTraceId() {
        return traceId;
    }

    public Float getPriority() {
        return priority;
    }

    public String getTransactionId() {
        return txnId;
    }

    public boolean hasGuid() {
        return guid != null && !guid.isEmpty();
    }

    public boolean hasParentType() {
        return parentType != null && !parentType.isEmpty();
    }

    public boolean hasTraceId() {
        return traceId != null && !traceId.isEmpty();
    }

    public boolean hasApplicationId() {
        return applicationId != null && !applicationId.isEmpty();
    }

    public boolean hasAccountId() {
        return accountId != null && !accountId.isEmpty();
    }

    public boolean hasTrustKey() {
        return trustKey != null && !trustKey.isEmpty();
    }

    public boolean hasPriority() {
        return priority != null;
    }

    public boolean isSampled() {
        return sampled != null ? sampled : false;
    }

    public boolean hasTransactionId() {
        return txnId != null && !txnId.isEmpty();
    }
}
