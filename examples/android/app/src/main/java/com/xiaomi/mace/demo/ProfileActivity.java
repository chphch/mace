package com.xiaomi.mace.demo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.xiaomi.mace.JniMaceUtils;
import com.xiaomi.mace.demo.camera.ContextMenuDialog;
import com.xiaomi.mace.demo.result.InitData;
import com.xiaomi.mace.demo.result.LabelCache;
import com.xiaomi.mace.demo.result.ProfileAppModel;
import com.xiaomi.mace.demo.result.ResultData;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ProfileActivity extends Activity implements View.OnClickListener {
    private static final String TAG = ProfileActivity.class.getSimpleName();
    Button mSelectMode;
    Button mSelectPhoneType;
    Button mTurnOnOffDNN;
    Button mSelectOperatorStartIndex;
    Button mSelectOperatorEndIndex;

    private List operatorNames;

    private InitData initData = new InitData();

    private boolean isDNNRunning;

    private Handler mBackgroundHandler = new Handler();
    private Runnable mHandleDNNRunnable = new Runnable() {
        @Override
        public void run() {
            if (isDNNRunning) {
                int finalSize = initData.getFinalSize();
                FloatBuffer floatBuffer = FloatBuffer.allocate(finalSize * finalSize * 3);

                long start = System.currentTimeMillis();
                float[] result = JniMaceUtils.maceMobilenetClassify(floatBuffer.array());
                final ResultData resultData = LabelCache.instance().getResultFirst(result);
                long costTime = System.currentTimeMillis() - start;
                Log.d(TAG, "mHandleDNNRunnable: name=" + resultData.name + ", time=" + costTime);
                mBackgroundHandler.postDelayed(mHandleDNNRunnable, 200);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_profile);

        mSelectMode = findViewById(R.id.tv_select_mode);
        mSelectMode.setOnClickListener(this);

        mSelectPhoneType = findViewById(R.id.tv_select_phone_type);
        mSelectPhoneType.setOnClickListener(this);

        mTurnOnOffDNN = findViewById(R.id.tv_turn_on_off_dnn);
        mTurnOnOffDNN.setOnClickListener(this);

        mSelectOperatorStartIndex = findViewById(R.id.tv_select_operator_start);
        mSelectOperatorStartIndex.setOnClickListener(this);

        mSelectOperatorEndIndex = findViewById(R.id.tv_select_operator_end);
        mSelectOperatorEndIndex.setOnClickListener(this);

        initJni();
        initView();
    }

    private void initView() {
        mSelectMode.setText(initData.getModel());
        mSelectPhoneType.setText(initData.getDevice());
        mTurnOnOffDNN.setText("DNN Stopped");
        mSelectOperatorStartIndex.setText("1");
        mSelectOperatorEndIndex.setText("" + operatorNames.size());
    }

    private void initJni() {
        ProfileAppModel.maceMobilenetCreateGPUContext(initData);
        ProfileAppModel.maceMobilenetCreateEngine(initData);

        // TODO: Get operatorNames from the model dynamically.
        operatorNames = new LinkedList();
        for (int i = 1; i <= 31; i++) {
            operatorNames.add("" + i);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_select_mode:
                showSelectMode();
                break;
            case R.id.tv_select_phone_type:
                showPhoneType();
                break;
            case R.id.tv_turn_on_off_dnn:
                turn_on_off_dnn();
                break;
            case R.id.tv_select_operator_start:
                showOperatorStartIndex();
                break;
            case R.id.tv_select_operator_end:
                showEndOperatorEndIndex();
                break;
        }
    }

    private void showPhoneType() {
        List<String> menus = Arrays.asList(InitData.DEVICES);
        ContextMenuDialog.show(this, menus, new ContextMenuDialog.OnClickItemListener() {
            @Override
            public void onCLickItem(String content) {
                mSelectPhoneType.setText(content);
                initData.setDevice(content);
                ProfileAppModel.maceMobilenetCreateEngine(initData);
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
//                handleOnlyCpuSupportByModel(content);
                ProfileAppModel.maceMobilenetCreateEngine(initData);
            }
        });
    }

    private void turn_on_off_dnn() {
        if (isDNNRunning) {
            turn_off_dnn();
        } else {
            turn_on_dnn();
        }
    }

    private void turn_on_dnn() {
        isDNNRunning = true;
        mBackgroundHandler.post(mHandleDNNRunnable);
        mTurnOnOffDNN.setText("DNN Running...");
    }

    private void turn_off_dnn() {
        isDNNRunning = false;
        mTurnOnOffDNN.setText("DNN Stopped");
    }

    private void showOperatorStartIndex() {
        ContextMenuDialog.show(this, operatorNames, new ContextMenuDialog.OnClickItemListener() {
            @Override
            public void onCLickItem(String content) {
                mSelectOperatorStartIndex.setText(content);
                // initData.operatorStartIndex starts from 0, and content starts from 1
                int operatorStartIndex = Integer.parseInt(content) - 1;
                initData.setOperatorStartIndex(operatorStartIndex);
                ProfileAppModel.maceMobilenetCreateEngine(initData);
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
                ProfileAppModel.maceMobilenetCreateEngine(initData);
            }
        });
    }
}