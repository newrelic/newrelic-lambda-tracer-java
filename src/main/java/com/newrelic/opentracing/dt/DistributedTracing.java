/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.dt;

import com.newrelic.opentracing.LambdaSpan;
import com.newrelic.opentracing.TransportType;
import com.newrelic.opentracing.state.DistributedTracingState;
import com.newrelic.opentracing.util.DistributedTraceUtil;
import com.newrelic.opentracing.util.TimeUtil;

import java.util.HashMap;
import java.util.Map;

public class DistributedTracing {

    private static final int MAJOR_CAT_VERSION = 0;
    private static final int MINOR_CAT_VERSION = 1;

    /*
     * In order to support distributed tracing, certain variables which are normally supplied via the connect response
     * must now be supplied via local configuration.
     */
    private static final String NEW_RELIC_ACCOUNT_ID = "NEW_RELIC_ACCOUNT_ID";
    private static final String NEW_RELIC_TRUSTED_ACCOUNT_KEY = "NEW_RELIC_TRUSTED_ACCOUNT_KEY";
    private static final String NEW_RELIC_PRIMARY_APPLICATION_ID = "NEW_RELIC_PRIMARY_APPLICATION_ID";

    private static final String NEW_RELIC_ACCOUNT_ID_DEFAULT = null;
    private static final String NEW_RELIC_PRIMARY_APPLICATION_ID_DEFAULT = "Unknown";

    private static Configuration configuration;

    private DistributedTracing() {
        if (configuration == null) {
            configuration = configureFromEnvironment();
        }
    }

    private static Configuration configureFromEnvironment() {
        final String accountIdEnvVar = System.getenv(NEW_RELIC_ACCOUNT_ID);
        final String trustKeyEnvVar = System.getenv(NEW_RELIC_TRUSTED_ACCOUNT_KEY);
        final String primaryAppIdEnvVar = System.getenv(NEW_RELIC_PRIMARY_APPLICATION_ID);

        String accountId =
                accountIdEnvVar == null ? NEW_RELIC_ACCOUNT_ID_DEFAULT : accountIdEnvVar;
        // trustKey defaults to accountId if not set explicitly
        String trustKey = trustKeyEnvVar == null ? accountId : trustKeyEnvVar;
        String primaryAppId =
                primaryAppIdEnvVar == null ? NEW_RELIC_PRIMARY_APPLICATION_ID_DEFAULT
                        : primaryAppIdEnvVar;
        return new Configuration(trustKey, accountId, primaryAppId);
    }

    private static DistributedTracing INSTANCE;

    public static DistributedTracing getInstance() {
        //since this class is completely stateless, the possible race on construction of the singleton instance
        // is benign.
        if (INSTANCE == null) {
            INSTANCE = new DistributedTracing();
        }
        return INSTANCE;
    }

    int getMajorSupportedCatVersion() {
        return MAJOR_CAT_VERSION;
    }

    int getMinorSupportedCatVersion() {
        return MINOR_CAT_VERSION;
    }

    String getAccountId() {
        return configuration.accountId;
    }

    String getApplicationId() {
        return configuration.primaryAppId;
    }

    public Map<String, Object> getDistributedTracingAttributes(DistributedTracingState dtState, String guid, float priority) {
        Map<String, Object> attributes = new HashMap<>();

        final DistributedTracePayloadImpl inboundPayload = dtState.getInboundPayload();
        if (inboundPayload != null) {
            if (inboundPayload.hasParentType()) {
                attributes.put("parent.type", inboundPayload.getParentType());
            }
            if (inboundPayload.hasApplicationId()) {
                attributes.put("parent.app", inboundPayload.getApplicationId());
            }
            if (inboundPayload.hasAccountId()) {
                attributes.put("parent.account", inboundPayload.getAccountId());
            }

            // Record unknown for now. There's no good way of identifying transport type in OpenTracing
            attributes.put("parent.transportType", TransportType.Unknown.name());

            final long transportDurationInMillis = dtState.getTransportTimeMillis();
            if (transportDurationInMillis >= 0) {
                float transportDurationSec = transportDurationInMillis / TimeUtil.MILLISECONDS_PER_SECOND;
                attributes.put("parent.transportDuration", transportDurationSec);
            }
        }

        attributes.put("guid", guid);
        attributes.put("traceId", dtState.getTraceId());
        attributes.put("priority", priority);
        attributes.put("sampled", DistributedTraceUtil.isSampledPriority(priority));

        return attributes;
    }

    String getTrustKey() {
        return configuration.trustKey;
    }

    public DistributedTracePayloadImpl createDistributedTracePayload(LambdaSpan span) {
        return DistributedTracePayloadImpl
                .createDistributedTracePayload(span.traceId(), span.guid(), span.getTransactionId(),
                        span.priority());
    }

    /**
     * Use this method to set a custom configuration for distributed tracing headers. In general, this
     * should not be the preferred method of configuring distributed tracing.
     *
     * @param customConfiguration Configuration to be used
     */
    public static void setConfiguration(Configuration customConfiguration) {
        configuration = customConfiguration;
    }

    public static class Configuration {

        private final String trustKey;
        private final String accountId;
        private final String primaryAppId;

        public Configuration(String trustKey, String accountId, String primaryAppId) {
            this.trustKey = trustKey;
            this.accountId = accountId;
            this.primaryAppId = primaryAppId;
        }
    }

}
