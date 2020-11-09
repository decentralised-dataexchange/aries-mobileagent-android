package io.igrant.mobileagent.qrcode.decode;


import android.os.Handler;
import android.os.Looper;


import java.util.concurrent.CountDownLatch;

import io.igrant.mobileagent.qrcode.QrCodeActivity;

/**
 * This thread does all the heavy lifting of decoding the images.
 */
final class DecodeThread extends Thread {

    private final QrCodeActivity mActivity;
    private Handler mHandler;
    private final CountDownLatch mHandlerInitLatch;

    DecodeThread(QrCodeActivity activity) {
        this.mActivity = activity;
        mHandlerInitLatch = new CountDownLatch(1);
    }

    Handler getHandler() {
        try {
            mHandlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return mHandler;
    }

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new DecodeHandler(mActivity);
        mHandlerInitLatch.countDown();
        Looper.loop();
    }

}
