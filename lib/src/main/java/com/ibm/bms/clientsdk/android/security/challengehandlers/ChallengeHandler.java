/*
 *     Copyright 2015 IBM Corp.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.ibm.bms.clientsdk.android.security.challengehandlers;

import android.content.Context;

import com.ibm.bms.clientsdk.android.security.api.AuthenticationContext;
import com.ibm.bms.clientsdk.android.security.api.AuthenticationListener;
import com.ibm.bms.clientsdk.android.security.internal.AuthorizationRequestManager;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by vitalym on 7/16/15.
 */
public class ChallengeHandler implements AuthenticationContext {

    private String realm;
    private volatile AuthenticationListener listener;
    private volatile ArrayList<AuthorizationRequestManager> waitingRequests = new ArrayList<AuthorizationRequestManager>();
    private volatile AuthorizationRequestManager activeRequest;

    public void initialize(String realm, AuthenticationListener listener) {
        this.realm = realm;
        this.listener = listener;
    }

    @Override
    public synchronized void submitAuthenticationChallengeAnswer(JSONObject answer) {
        if (activeRequest == null) {
            return;
        }

        if (answer != null) {
            activeRequest.submitAnswer(answer, realm);
        } else {
            activeRequest.removeExpectedAnswer(realm);
        }

        setActiveRequest(null);
    }

    @Override
    public synchronized void submitAuthenticationChallengeSuccess() {
        if (activeRequest != null) {
            activeRequest.removeExpectedAnswer(realm);
            setActiveRequest(null);
        }

        releaseWaitingList();
    }

    @Override
    public synchronized void submitAuthenticationChallengeFailure(JSONObject info) {
        if (activeRequest != null) {
            activeRequest.requestFailed(info);
            setActiveRequest(null);
        }

        releaseWaitingList();
    }

    public synchronized void handleChallenge(AuthorizationRequestManager request, JSONObject challenge, Context context) {
        if (activeRequest == null) {
            setActiveRequest(request);
            listener.onAuthenticationChallengeReceived(this, challenge, context);
        } else {
            waitingRequests.add(request);
        }
    }

    public synchronized void handleSuccess(JSONObject success) {
        listener.onAuthenticationSuccess(success);
        releaseWaitingList();
        setActiveRequest(null);
    }

    public synchronized void handleFailure(JSONObject failure) {
        listener.onAuthenticationFailure(failure);
        clearWaitingList();
        setActiveRequest(null);
    }

    private synchronized void setActiveRequest(AuthorizationRequestManager request) {
        activeRequest = request;
    }

    private synchronized void releaseWaitingList() {
        for (AuthorizationRequestManager request : waitingRequests) {
            request.removeExpectedAnswer(realm);
        }

        clearWaitingList();
    }

    private synchronized void clearWaitingList() {
        waitingRequests.clear();
    }
}
