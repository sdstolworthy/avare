/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ds.avare.webinfc;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.ds.avare.StorageService;
import com.ds.avare.flight.WeightAndBalance;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;

import java.util.LinkedList;

/**
 * 
 * @author zkhan
 * This class feeds the WebView with data
 */
public class WebAppWnbInterface {
    private StorageService mService;
    private WeightAndBalance mWnb;
    private WebView mWebView;

    private static final int MSG_UPDATE_WNB = 1;
    private static final int MSG_CALCULATE = 2;
    private static final int MSG_CLEAR_WNB_SAVE = 7;
    private static final int MSG_ADD_WNB_SAVE = 8;

    /**
     * Instantiate the interface and set the context
     */
    public WebAppWnbInterface(WebView ww, GenericCallback cb) {
        mWebView = ww;
        mService = StorageService.getInstance();
        mService.getDBResource().setUserWnb(new WeightAndBalance(WeightAndBalance.WNB_C172R));
        mService.getDBResource().setUserWnb(new WeightAndBalance(WeightAndBalance.WNB_PA23_250));
        mService.getDBResource().setUserWnb(new WeightAndBalance(WeightAndBalance.WNB_PA28R_200B));

    }

    /**
     *
     */
    public void calculate() {
        mHandler.sendEmptyMessage(MSG_CALCULATE);
    }

    /**
     *
     */
    public void clearWnbSave() {
        mHandler.sendEmptyMessage(MSG_CLEAR_WNB_SAVE);
    }

    /**
     * Update the passed point on the WNB page
     * @param
     */
    public void updateWnb() {
        mHandler.sendEmptyMessage(MSG_UPDATE_WNB);
    }

    /**
     * New saved w&b when the save list changes.
     */
    public void newSaveWnb() {

        clearWnbSave();
        LinkedList<WeightAndBalance> wnbs = mService.getDBResource().getUserWnbs();

        for (WeightAndBalance wnb : wnbs) {
            Message m = mHandler.obtainMessage(MSG_ADD_WNB_SAVE, (Object)("'" + Helper.formatJsArgs(wnb.getName()) + "'"));
            mHandler.sendMessage(m);
        }
    }

    /**
     * 
     * @param data
     */
    @JavascriptInterface
    public void saveWnb(String data) {

        mWnb = new WeightAndBalance(data);
        mService.getDBResource().setUserWnb(mWnb);

        newSaveWnb();
    }

    /**
     * 
     * @param name
     */
    @JavascriptInterface
    public void loadWnb(String name) {
        mWnb = mService.getDBResource().getUserWnb(name);

        updateWnb();
    }

    /**
     * 
     * @param name
     */
    @JavascriptInterface
    public void saveDelete(String name) {
        mService.getDBResource().deleteUserWnb(name);

        newSaveWnb();

    }



    /**
     * This leak warning is not an issue if we do not post delayed messages, which is true here.
     * Must use handler for functions called from JS, but for uniformity, call all JS from this handler
     */
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if(MSG_UPDATE_WNB == msg.what) {
                /*
                 * Now update HTML with latest wnb stuff, do this every time we start the List screen as
                 * things might have changed.
                 */
                if(null != mWnb) {
                    String data = mWnb.getJSON().toString();

                    if (null != data) {
                        mWebView.loadUrl("javascript:wnb_set('" + data + "')");
                    }
                }
            }
            else if(MSG_ADD_WNB_SAVE == msg.what) {
            	String func = "javascript:save_add(" + (String)msg.obj + ")";
            	mWebView.loadUrl(func);
        	}
            else if(MSG_CALCULATE == msg.what) {
                String func = "javascript:wnb_calculate()";
                mWebView.loadUrl(func);
            }
            else if(MSG_CLEAR_WNB_SAVE == msg.what) {
                String func = "javascript:save_clear()";
                mWebView.loadUrl(func);
            }

        }
    };

}