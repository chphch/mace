// Copyright 2018 The MACE Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.xiaomi.mace.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import com.xiaomi.mace.demo.camera.CameraEngage;
import com.xiaomi.mace.demo.camera.CameraTextureView;
import com.xiaomi.mace.demo.camera.ContextMenuDialog;

import com.xiaomi.mace.demo.camera.MessageEvent;
import com.xiaomi.mace.demo.result.InitData;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CameraActivity extends Activity implements View.OnClickListener, AppModel.CreateEngineCallback {

    CameraEngage mCameraEngage;
    ImageView mPictureResult;
    Button mSelectMode;
    Button mSelectPhoneType;
    Button mSelectOperatorStartIndex;
    Button mSelectOperatorEndIndex;
    CameraTextureView mCameraTextureView;
    private TextView mResultView;
    private InitData initData = new InitData();

    Button mTurnOnOffDNN;

    // For Profiling
    Button mStartProfile;
    TextView mProfileResult;
    private boolean isProfiling = false;
    private List<Long> profiledCostTimes;
    private final int profileTrialNum = 50;
    private long startTimeMilliseconds;

    private List operatorNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        mPictureResult = findViewById(R.id.iv_picture);
        mResultView = findViewById(R.id.tv_show_result);
        mCameraTextureView = findViewById(R.id.camera_texture);
        mCameraEngage = CameraFactory.genCameEngage(mCameraTextureView);

        mSelectMode = findViewById(R.id.tv_select_mode);
        mSelectMode.setOnClickListener(this);

        mSelectPhoneType = findViewById(R.id.tv_select_phone_type);
        mSelectPhoneType.setOnClickListener(this);

        mSelectOperatorStartIndex = findViewById(R.id.tv_select_operator_start);
        mSelectOperatorStartIndex.setOnClickListener(this);

        mSelectOperatorEndIndex = findViewById(R.id.tv_select_operator_end);
        mSelectOperatorEndIndex.setOnClickListener(this);

        mStartProfile = findViewById(R.id.tv_start_profile);
        mStartProfile.setOnClickListener(this);

        mProfileResult = findViewById(R.id.tv_profile_result);
        mProfileResult.setOnClickListener(this);

        mTurnOnOffDNN = findViewById(R.id.tv_turn_on_off_dnn);
        mTurnOnOffDNN.setOnClickListener(this);

        initJni();
        initView();

    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    private void initView() {
        mSelectMode.setText(initData.getModel());
        mSelectPhoneType.setText(initData.getDevice());
        mSelectOperatorStartIndex.setText("1");
        mSelectOperatorEndIndex.setText("" + operatorNames.size());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraEngage.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        mCameraEngage.onPause();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPicture(MessageEvent.PicEvent picEvent) {
        if (picEvent != null && picEvent.getBitmap() != null) {
            mPictureResult.setImageBitmap(picEvent.getBitmap());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSetCameraViewSize(MessageEvent.OutputSizeEvent event) {
        if (mCameraTextureView != null) {
            mCameraTextureView.setRatio(event.width, event.height);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onResultData(MessageEvent.MaceResultEvent resultData) {
        if (resultData != null && resultData.getData() != null) {
            String result = resultData.getData().name  + "\n"
                    + resultData.getData().probability + "\ncost time(ms): "
                    + resultData.getData().costTime;
            mResultView.setText(result);
            if (isProfiling) {
                doProfile(resultData.getData().costTime);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_select_mode:
                showSelectMode();
                break;
            case R.id.tv_select_phone_type:
                showPhoneType();
                break;
            case R.id.tv_select_operator_start:
                showOperatorStartIndex();
                break;
            case R.id.tv_select_operator_end:
                showEndOperatorEndIndex();
                break;
            case R.id.tv_start_profile:
                startProfile();
                break;
            case R.id.tv_turn_on_off_dnn:
                turn_on_off_dnn();
                break;
        }
    }

    private void turn_on_off_dnn() {
        AppModel.instance.switchStopClassify();
        mTurnOnOffDNN.setText(AppModel.instance.isStopClassify() ? "DNN Stopped" : "DNN Running...");
    }

    private void startProfile() {
        if (!isProfiling) {
            mStartProfile.setText("Profiling...");
            isProfiling = true;
            profiledCostTimes = new LinkedList<>();
            startTimeMilliseconds = System.currentTimeMillis();
        }
    }

    private void doProfile(long costTime) {
        if (isProfiling) {
            profiledCostTimes.add(costTime);
            setProfileResultView(profiledCostTimes.size(), -1, -1, -1);
            if (profiledCostTimes.size() == profileTrialNum) {
                endProfile();
            }
        }
    }

    private void endProfile() {
        long profileTimeMilliseconds = System.currentTimeMillis() - startTimeMilliseconds;
        setProfileResultView(-1, (int) Utils.mean(profiledCostTimes), (int) Utils.std(profiledCostTimes), profileTimeMilliseconds);
        profiledCostTimes = new LinkedList<>();
        mStartProfile.setText("Start Profile");
        isProfiling = false;
    }

    private void setProfileResultView(int index, int avgCostTime, int stdCostTime, long profiledTimeMilliseconds) {
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setGroupingUsed(true);
        mProfileResult.setText(
                "index: " + index + "/" + profileTrialNum
                + "\navg: " + avgCostTime + " ms"
                + "\nstd: " + stdCostTime
                + "\ntotal time (ms): " + numberFormat.format(profiledTimeMilliseconds));
    }

    private void showOperatorStartIndex() {
        ContextMenuDialog.show(this, operatorNames, new ContextMenuDialog.OnClickItemListener() {
            @Override
            public void onCLickItem(String content) {
                mSelectOperatorStartIndex.setText(content);
                // initData.operatorStartIndex starts from 0, and content starts from 1
                int operatorStartIndex = Integer.parseInt(content) - 1;
                initData.setOperatorStartIndex(operatorStartIndex);
                AppModel.instance.maceMobilenetCreateEngine(initData, CameraActivity.this);
            }
        });
    }

    private void showEndOperatorEndIndex() {
        ContextMenuDialog.show(this, operatorNames, new ContextMenuDialog.OnClickItemListener() {
            @Override
            public void onCLickItem(String content) {
                mSelectOperatorEndIndex.setText(content);
                // initData.operatorEndIndex starts from 0, and content starts from 1
                int operatorEndIndex = Integer.parseInt(content) - 1;
                initData.setOperatorEndIndex(operatorEndIndex);
                AppModel.instance.maceMobilenetCreateEngine(initData, CameraActivity.this);
            }
        });
    }

    private void initJni() {
        AppModel.instance.maceMobilenetCreateGPUContext(initData);
        AppModel.instance.maceMobilenetCreateEngine(initData, this);

        // TODO: Get operatorNames from the model dynamically.
        operatorNames = new LinkedList();
        for (int i = 1; i <= 31; i++) {
            operatorNames.add("" + i);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constant.CAMERA_PERMISSION_REQ) {
            boolean allGrant = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    mCameraEngage.onResume();
                    allGrant = false;
                    break;
                }
            }
            if (allGrant) {
                initData = new InitData();
            }
        }
    }

    private void showPhoneType() {
        List<String> menus = Arrays.asList(InitData.DEVICES);
        ContextMenuDialog.show(this, menus, new ContextMenuDialog.OnClickItemListener() {
            @Override
            public void onCLickItem(String content) {
                mSelectPhoneType.setText(content);
                initData.setDevice(content);
                AppModel.instance.maceMobilenetCreateEngine(initData, CameraActivity.this);
            }
        });
    }

    private void showSelectMode() {
        List<String> menus = Arrays.asList(InitData.MODELS);
        ContextMenuDialog.show(this, menus, new ContextMenuDialog.OnClickItemListener() {
            @Override
            public void onCLickItem(String content) {
                mSelectMode.setText(content);
                initData.setModel(content);
                handleOnlyCpuSupportByModel(content);
                AppModel.instance.maceMobilenetCreateEngine(initData, CameraActivity.this);
            }
        });
    }

    private void handleOnlyCpuSupportByModel(String model) {
        if (InitData.isOnlySupportCpuByModel(model)) {
            String device = InitData.getCpuDevice();
            mSelectPhoneType.setText(device);
            mSelectPhoneType.setEnabled(false);
            initData.setDevice(device);
        } else {
            mSelectPhoneType.setEnabled(true);
        }
    }

    @Override
    public void onCreateEngineFail(final boolean quit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setMessage("Failed to create inference engine with current setting:\n" + initData.getModel() + ", " + initData.getDevice());
        builder.setCancelable(false);
        builder.setPositiveButton(quit ? "Quit" : "Reset", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (quit) {
                    System.exit(0);
                } else {
                    dialog.dismiss();
                    resetCpu();
                }
            }
        });
        builder.show();
    }

    private void resetCpu() {
        String content = InitData.DEVICES[0];
        mSelectPhoneType.setText(content);
        initData.setDevice(content);
        AppModel.instance.maceMobilenetCreateEngine(initData, CameraActivity.this);
    }
}
