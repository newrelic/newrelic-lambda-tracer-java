/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing;

import com.newrelic.opentracing.state.DistributedTracingState;
import com.newrelic.opentracing.state.PrioritySamplingState;
import com.newrelic.opentracing.state.TransactionState;
import io.opentracing.util.ThreadLocalScopeManager;

class LambdaScopeManager extends ThreadLocalScopeManager {

    ThreadLocal<DataCollection> dataCollection = ThreadLocal.withInitial(DataCollection::new);
    ThreadLocal<DistributedTracingState> dtState = ThreadLocal.withInitial(DistributedTracingState::new);
    ThreadLocal<PrioritySamplingState> priorityState = ThreadLocal.withInitial(PrioritySamplingState::new);
    ThreadLocal<TransactionState> txnState = ThreadLocal.withInitial(TransactionState::new);

    /**
     * Called when the last span finishes.
     */
    void resetState() {
        dtState.set(new DistributedTracingState());
        priorityState.set(new PrioritySamplingState());
        txnState.set(new TransactionState());
    }

}
