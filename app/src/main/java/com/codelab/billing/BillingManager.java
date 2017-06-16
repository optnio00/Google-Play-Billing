/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
package com.codelab.billing;

import android.app.Activity;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * TODO: Implement BillingManager that will handle all the interactions with Play Store
 * (via Billing library), maintain connection to it through BillingClient and cache
 * temporary states/data if needed.
 */
public class BillingManager implements PurchasesUpdatedListener {
    private static final String TAG = "BillingManager";
    private final BillingClient mBillingClient;
    private final Activity mActivity;

    private boolean mIsBillingClientConnected;

    public BillingManager(Activity activity) {
        mActivity = activity;
        mBillingClient = new BillingClient.Builder(mActivity).setListener(this).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponse) {
                if (billingResponse == BillingClient.BillingResponse.OK) {
                    Log.i(TAG, "onBillingSetupFinished() response: " + billingResponse);
                } else {
                    Log.w(TAG, "onBillingSetupFinished() error code: " + billingResponse);
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "onBillingServiceDisconnected()");
            }
        });
    }

    private void startServiceConnection(final Runnable executeOnSuccess) {
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponse) {
                if (billingResponse == BillingClient.BillingResponse.OK) {
                    Log.i(TAG, "onBillingSetupFinished() response: " + billingResponse);
                    mIsBillingClientConnected = true;
                    if (executeOnSuccess != null) {
                        executeOnSuccess.run();
                    }
                } else {
                    Log.w(TAG, "onBillingSetupFinished() error code: " + billingResponse);
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "onBillingServiceDisconnected()");
                mIsBillingClientConnected = false;
            }
        });
    }

    public void querySkuDetailsAsync(@BillingClient.SkuType final String itemType,
                                     final List<String> skuList, final SkuDetailsResponseListener listener) {
        // Specify a runnable to start when connection to Billing client is established
        Runnable executeOnConnectedService = new Runnable() {
            @Override
            public void run() {
                mBillingClient.querySkuDetailsAsync(itemType, skuList,
                        new SkuDetailsResponseListener() {
                            @Override
                            public void onSkuDetailsResponse(SkuDetails.SkuDetailsResult result) {
                                // If billing service was disconnected, we try to reconnect 1 time
                                // (feel free to introduce your retry policy here).
                                listener.onSkuDetailsResponse(result);
                            }
                        });
            }
        };

        if (mIsBillingClientConnected) {
            executeOnConnectedService.run();
        } else {
            // If Billing client was disconnected, we retry 1 time and if success, execute the query
            startServiceConnection(executeOnConnectedService);
        }
    }

    public void startPurchaseFlow(final String skuId, final String billingType) {
        // Specify a runnable to start when connection to Billing client is established
        Runnable executeOnConnectedService = new Runnable() {
            @Override
            public void run() {
                BillingFlowParams billingFlowParams = new BillingFlowParams.Builder()
                        .setType(billingType)
                        .setSku(skuId)
                        .build();
                mBillingClient.launchBillingFlow(mActivity, billingFlowParams);
            }
        };

        if (mIsBillingClientConnected) {
            executeOnConnectedService.run();
        } else {
            // If Billing client was disconnected, we retry 1 time and if success, execute the query
            startServiceConnection(executeOnConnectedService);
        }
    }
    @Override
    public void onPurchasesUpdated(@BillingClient.BillingResponse int responseCode,
                                   List<Purchase> purchases) {
        Log.d(TAG, "onPurchasesUpdated() response: " + responseCode);
    }

    private static final HashMap<String, List<String>> SKUS;
    static
    {
        SKUS = new HashMap<>();
        SKUS.put(SkuType.INAPP, Arrays.asList("gas", "premium"));
        SKUS.put(SkuType.SUBS, Arrays.asList("gold_monthly", "gold_yearly"));
    }

    public List<String> getSkus(@SkuType String type) {
        return SKUS.get(type);
    }
    public void destroy() {
        mBillingClient.endConnection();
    }

}
