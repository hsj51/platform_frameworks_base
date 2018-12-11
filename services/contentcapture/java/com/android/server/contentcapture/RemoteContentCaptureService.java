/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.server.contentcapture;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.service.contentcapture.ContentCaptureEventsRequest;
import android.service.contentcapture.IContentCaptureService;
import android.service.contentcapture.InteractionContext;
import android.service.contentcapture.SnapshotData;
import android.text.format.DateUtils;
import android.view.contentcapture.ContentCaptureEvent;

import com.android.server.infra.AbstractMultiplePendingRequestsRemoteService;

import java.util.List;

final class RemoteContentCaptureService
        extends AbstractMultiplePendingRequestsRemoteService<RemoteContentCaptureService,
        IContentCaptureService> {

    // TODO(b/117779333): changed it so it's permanentely bound
    private static final long TIMEOUT_IDLE_BIND_MILLIS = 2 * DateUtils.MINUTE_IN_MILLIS;
    private static final long TIMEOUT_REMOTE_REQUEST_MILLIS = 2 * DateUtils.SECOND_IN_MILLIS;

    RemoteContentCaptureService(Context context, String serviceInterface,
            ComponentName componentName, int userId,
            ContentCaptureServiceCallbacks callbacks, boolean bindInstantServiceAllowed,
            boolean verbose) {
        super(context, serviceInterface, componentName, userId, callbacks,
                bindInstantServiceAllowed, verbose, /* initialCapacity= */ 2);
    }

    @Override // from RemoteService
    protected IContentCaptureService getServiceInterface(@NonNull IBinder service) {
        return IContentCaptureService.Stub.asInterface(service);
    }

    // TODO(b/111276913): modify super class to allow permanent binding when value is 0 or negative
    @Override // from RemoteService
    protected long getTimeoutIdleBindMillis() {
        // TODO(b/111276913): read from Settings so it can be changed in the field
        return TIMEOUT_IDLE_BIND_MILLIS;
    }

    @Override // from RemoteService
    protected long getRemoteRequestMillis() {
        // TODO(b/111276913): read from Settings so it can be changed in the field
        return TIMEOUT_REMOTE_REQUEST_MILLIS;
    }

    /**
     * Called by {@link ContentCaptureSession} to generate a call to the
     * {@link RemoteContentCaptureService} to indicate the session was created (when {@code context}
     * is not {@code null} or destroyed (when {@code context} is {@code null}).
     */
    public void onSessionLifecycleRequest(@Nullable InteractionContext context,
            @NonNull String sessionId) {
        scheduleAsyncRequest((s) -> s.onSessionLifecycle(context, sessionId));
    }

    /**
     * Called by {@link ContentCaptureSession} to send a batch of events to the service.
     */
    public void onContentCaptureEventsRequest(@NonNull String sessionId,
            @NonNull List<ContentCaptureEvent> events) {
        scheduleAsyncRequest((s) -> s.onContentCaptureEventsRequest(sessionId,
                new ContentCaptureEventsRequest(events)));
    }

    /**
     * Called by {@link ContentCaptureSession} to send snapshot data to the service.
     */
    public void onActivitySnapshotRequest(@NonNull String sessionId,
            @NonNull SnapshotData snapshotData) {
        scheduleAsyncRequest((s) -> s.onActivitySnapshot(sessionId, snapshotData));
    }

    public interface ContentCaptureServiceCallbacks
            extends VultureCallback<RemoteContentCaptureService> {
        // NOTE: so far we don't need to notify the callback implementation (an inner class on
        // AutofillManagerServiceImpl) of the request results (success, timeouts, etc..), so this
        // callback interface is empty.
    }
}
