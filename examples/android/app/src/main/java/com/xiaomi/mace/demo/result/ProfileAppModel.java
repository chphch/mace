package com.xiaomi.mace.demo.result;

import android.util.Log;

import com.xiaomi.mace.JniMaceUtils;
import com.xiaomi.mace.demo.ProfileActivity;

public class ProfileAppModel {
    private static final String TAG = ProfileActivity.class.getSimpleName();

    public static void maceMobilenetCreateGPUContext(InitData initData) {
        int result = JniMaceUtils.maceMobilenetCreateGPUContext(initData.getStoragePath());
        Log.i(TAG, "initJni; maceMobilenetCreateGPUContext result = " + result);
    }

    public static void maceMobilenetCreateEngine(InitData initData) {
        int result = JniMaceUtils.maceMobilenetCreateEngine(
                initData.getOmpNumThreads(), initData.getCpuAffinityPolicy(),
                initData.getGpuPerfHint(), initData.getGpuPriorityHint(),
                initData.getModel(), initData.getDevice(),
                initData.getOperatorStartIndex(), initData.getOperatorEndIndex());
        Log.i(TAG, "showSelectMode: maceMobilenetCreateEngine result = " + result);
    }
}
