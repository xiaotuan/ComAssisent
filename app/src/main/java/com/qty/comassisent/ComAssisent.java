package com.qty.comassisent;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.qty.comassisent.log.Log;
import com.qty.comassisent.serialport.SerialPortConfig;
import com.qty.comassisent.serialport.SerialPortHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class ComAssisent extends Activity {

    private static final int PERMISION_REQUEST_CODE = 106;
    private static final int COM_A_REQUEST_CODE = 101;
    private static final int COM_B_REQUEST_CODE = 102;
    private static final int COM_C_REQUEST_CODE = 103;
    private static final int COM_D_REQUEST_CODE = 104;

    private View mABContainer;
    private View mCDContainer;
    private View mAContainer;
    private View mBContainer;
    private View mCContainer;
    private View mDContainer;
    private EditText mLogEt;
    private Button mWindowsBt;
    private Button mFormatBt;
    private CheckBox mAutoClearCb;
    private EditText mMaxLineEt;
    private CmdEditView mACmdEditView;
    private CmdEditView mBCmdEditView;
    private CmdEditView mCCmdEditView;
    private CmdEditView mDCmdEditView;
    private SerialPortHelper mASerialPortHelper;
    private SerialPortHelper mBSerialPortHelper;
    private SerialPortHelper mCSerialPortHelper;
    private SerialPortHelper mDSerialPortHelper;
    private SendCmdSequenceThread mASendCmdThread;
    private SendCmdSequenceThread mBSendCmdThread;
    private SendCmdSequenceThread mCSendCmdThread;
    private SendCmdSequenceThread mDSendCmdThread;
    private ComAssisentSharedPreferences mSharedPreferences;
    private DispQueueThread mDispThread;

    private int mLines;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_com_assisent);
        initValues();
        initViews();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(ComAssisentConstants.PERMISIONS, PERMISION_REQUEST_CODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateViews();
        updateViewsEnabled();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(this, "onRequestPermissionsResult=>code: " + requestCode
                + ", request length: " + permissions.length + ", grant length: " + grantResults.length);
        if (requestCode == PERMISION_REQUEST_CODE) {
            if (permissions.length != grantResults.length) {
                new ComAssisentDialog.Builder(this)
                        .setTitle("警告")
                        .setMessage("权限被拒绝，部分功能可能无法正常工作")
                        .setPositiveButton("确定", null)
                        .show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.w(this, "onActivityResult=>requestCode: " + requestCode + ", resultCode: " + resultCode);
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                switch (requestCode) {
                    case COM_A_REQUEST_CODE:
                        mACmdEditView.setInnerCmdFileUri(uri);
                        break;

                    case COM_B_REQUEST_CODE:
                        mBCmdEditView.setInnerCmdFileUri(uri);
                        break;

                    case COM_C_REQUEST_CODE:
                        mCCmdEditView.setInnerCmdFileUri(uri);
                        break;

                    case COM_D_REQUEST_CODE:
                        mDCmdEditView.setInnerCmdFileUri(uri);
                        break;
                }
            } else {
                Log.w(this, "onActivityResult=>uri is null.");
            }
        }
    }

    private void initValues() {
        mSharedPreferences = new ComAssisentSharedPreferences(this);
        mLines = 0;
        mDispThread = new DispQueueThread();
        mDispThread.start();
    }

    private void initViews() {
        mABContainer = findViewById(R.id.ab_com_container);
        mCDContainer = findViewById(R.id.cd_com_container);
        mAContainer = findViewById(R.id.com_a);
        mBContainer = findViewById(R.id.com_b);
        mCContainer = findViewById(R.id.com_c);
        mDContainer = findViewById(R.id.com_d);
        mLogEt = findViewById(R.id.log);
        mWindowsBt = findViewById(R.id.windows);
        Button mSaveLogBt = findViewById(R.id.save_log);
        Button mClearBt = findViewById(R.id.clear);
        Button mFontBt = findViewById(R.id.font);
        mFormatBt = findViewById(R.id.log_format);
        mAutoClearCb = findViewById(R.id.auto_clear);
        mMaxLineEt = findViewById(R.id.max_line);
        mACmdEditView = new CmdEditView(this, mAContainer, "A");
        mBCmdEditView = new CmdEditView(this, mBContainer, "B");
        mCCmdEditView = new CmdEditView(this, mCContainer, "C");
        mDCmdEditView = new CmdEditView(this, mDContainer, "D");

        mWindowsBt.setOnClickListener(mWindowsClick);
        mSaveLogBt.setOnClickListener(mSaveLogClick);
        mClearBt.setOnClickListener(mClearClick);
        mFontBt.setOnClickListener(mFontClick);
        mAutoClearCb.setOnCheckedChangeListener(mAutoClearCheckedChange);
        mMaxLineEt.addTextChangedListener(mMaxLineTextWatcher);
        mACmdEditView.setCallback(mCmdEditViewCallback);
        mBCmdEditView.setCallback(mCmdEditViewCallback);
        mCCmdEditView.setCallback(mCmdEditViewCallback);
        mDCmdEditView.setCallback(mCmdEditViewCallback);
        mFormatBt.setOnClickListener(mFormatClick);
    }

    private void updateViews() {
        updateLogEditView();
        updateSettingViews();
    }

    private void updateViewsEnabled() {
        mWindowsBt.setEnabled(hasSerialPortOpened());
        mFormatBt.setEnabled(hasSerialPortOpened());
        mAutoClearCb.setEnabled(hasSerialPortOpened());
        mMaxLineEt.setEnabled(hasSerialPortOpened());
    }

    private void updateLogEditView() {
        int fontSize =  mSharedPreferences.getInt(ComAssisentSharedPreferences.PREF_LOG_VIEW_TEXT_SIZE, getResources().getInteger(R.integer.default_log_text_size));
        mLogEt.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
    }

    private void updateSettingViews() {
        updateWindows();
        updateAutoClearView();
    }

    private void updateWindows() {
        int count = mSharedPreferences.getInt(ComAssisentSharedPreferences.PREF_WINDOW_COUNT, getResources().getInteger(R.integer.default_window_count));
        mABContainer.setVisibility(View.VISIBLE);
        mAContainer.setVisibility(View.VISIBLE);
        mBContainer.setVisibility(count == 4 ? View.VISIBLE : View.GONE);
        mCDContainer.setVisibility(count >= 2 ? View.VISIBLE : View.GONE);
        mCContainer.setVisibility(count >= 2 ? View.VISIBLE : View.GONE);
        mDContainer.setVisibility(count >= 3 ? View.VISIBLE : View.GONE);
        mWindowsBt.setEnabled(hasSerialPortOpened());
    }

    @SuppressLint("SetTextI18n")
    private void updateAutoClearView() {
        int maxLine = mSharedPreferences.getInt(ComAssisentSharedPreferences.PREF_MAX_LINE, getResources().getInteger(R.integer.default_max_line));
        boolean isAutoClear = mSharedPreferences.getBoolean(ComAssisentSharedPreferences.PREF_AUTO_CLEAR, getResources().getBoolean(R.bool.default_auto_clear));
        mAutoClearCb.setChecked(isAutoClear);
        mAutoClearCb.setEnabled(hasSerialPortOpened());
        mMaxLineEt.setEnabled(isAutoClear && hasSerialPortOpened());
        mMaxLineEt.setText(Integer.toString(maxLine));
    }

    private boolean hasSerialPortOpened() {
        return mASerialPortHelper == null && mBSerialPortHelper == null && mCSerialPortHelper == null && mDSerialPortHelper == null;
    }

    private String getSaveFilePath() {
        String documentPath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        String dataDirPath = documentPath + File.separator + ComAssisentConstants.DATA_FILE_DIR;
        File dir = new File(dataDirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        String dataFilePath = dataDirPath + File.separator + ComAssisentConstants.DATA_FILE_NAME + new Date().getTime() + ".txt";
        File file = new File(dataFilePath);
        if (!file.exists() || !dir.isFile()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                Log.e(this, "getSaveFilePath=>error: ", e);
                return null;
            }
        }
        return dataFilePath;
    }

    private void saveSerialPortDataToFile(String filePath) {
        File logFile = new File(filePath);
        try (FileOutputStream fos = new FileOutputStream(logFile)) {
            fos.write(mLogEt.getText().toString().getBytes());
            fos.flush();
            Toast.makeText(this, getString(R.string.save_file_tip, filePath), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(this, "saveLogToFile=>error: ", e);
            Toast.makeText(this, R.string.save_file_fail, Toast.LENGTH_SHORT).show();
        }
    }

    public void hideBottomUIMenu() {
        View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            decorView.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener(){
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                View decorView = getWindow().getDecorView();
                int uiState=decorView.getSystemUiVisibility();
                if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
                    if(uiState!=View.GONE) decorView.setSystemUiVisibility(View.GONE);
                } else if (Build.VERSION.SDK_INT >= 19) {
                    if(uiState!=(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN))
                        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
                }
            }
        });
    }

    private View.OnClickListener mWindowsClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int count = mSharedPreferences.getInt(ComAssisentSharedPreferences.PREF_WINDOW_COUNT, getResources().getInteger(R.integer.default_window_count));
            final CharSequence[] items = new CharSequence[] {"1", "2", "3", "4"};
            new ComAssisentDialog.Builder(ComAssisent.this)
                    .setTitle(R.string.select_window_count)
                    .setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(ComAssisent.this, "onClick=>window count: " + (which + 1));
                            int currentCount = which + 1;
                            if (currentCount != count) {
                                mSharedPreferences.putInt(ComAssisentSharedPreferences.PREF_WINDOW_COUNT, currentCount);
                                ComAssisent.this.updateWindows();
                            }
                        }
                    }).show();
        }
    };

    private View.OnClickListener mSaveLogClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ComAssisent.this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(ComAssisent.this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if(TextUtils.isEmpty(mLogEt.getText().toString().trim())) {
                Toast.makeText(ComAssisent.this, R.string.no_data_save, Toast.LENGTH_SHORT).show();
                return;
            }
            String path = getSaveFilePath();
            if (path != null) {
                saveSerialPortDataToFile(path);
            } else {
                Toast.makeText(ComAssisent.this, R.string.can_not_save_file, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private View.OnClickListener mClearClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mLogEt.setText("");
        }
    };

    private View.OnClickListener mFontClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int minSize = getResources().getInteger(R.integer.min_log_text_size);
            final int maxSize = getResources().getInteger(R.integer.max_log_text_size);
            int fontSize =  mSharedPreferences.getInt(ComAssisentSharedPreferences.PREF_LOG_VIEW_TEXT_SIZE, getResources().getInteger(R.integer.default_log_text_size));
            int progress = (int)((fontSize - minSize) * 100.0 / (maxSize - minSize));
            SeekBar seekBar = new SeekBar(ComAssisent.this);
            seekBar.setMax(100);
            seekBar.setProgress(progress);
            seekBar.setPadding(48, 10, 48, 0);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int size = minSize + (int)((maxSize - minSize) * (progress / 100.0));
                    Log.d(ComAssisent.this, "mFontClick=>progress: " + progress + ", fontSize: " + size);
                    mSharedPreferences.putInt(ComAssisentSharedPreferences.PREF_LOG_VIEW_TEXT_SIZE, size);
                    updateLogEditView();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            new ComAssisentDialog.Builder(ComAssisent.this)
                    .setView(seekBar)
                    .show();
        }
    };

    private View.OnClickListener mFormatClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int index = mSharedPreferences.getInt(ComAssisentSharedPreferences.PREF_LOG_FOMART_INDEX, getResources().getInteger(R.integer.default_log_fomart_index));
            final String[] items = getResources().getStringArray(R.array.log_format);
            new ComAssisentDialog.Builder(ComAssisent.this)
                    .setTitle(R.string.select_log_format)
                    .setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(ComAssisent.this, "onClick=>window count: " + (which + 1));
                            int currentIndex = which;
                            if (currentIndex != index) {
                                mSharedPreferences.putInt(ComAssisentSharedPreferences.PREF_LOG_FOMART_INDEX, currentIndex);
                            }
                        }
                    }).show();
        }
    };

    private CheckBox.OnCheckedChangeListener mAutoClearCheckedChange = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.d(ComAssisent.this, "mAutoClearCheckedChange=>isChecked: " + isChecked);
            mSharedPreferences.putBoolean(ComAssisentSharedPreferences.PREF_AUTO_CLEAR, isChecked);
            updateAutoClearView();
        }
    };

    private TextWatcher mMaxLineTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            int maxLine = mSharedPreferences.getInt(ComAssisentSharedPreferences.PREF_MAX_LINE, getResources().getInteger(R.integer.default_max_line));
            String lineStr = mMaxLineEt.getText().toString().trim();
            if (!TextUtils.isEmpty(lineStr)) {
                int lines = Integer.parseInt(lineStr);
                if (lines >= 0 && lines != maxLine) {
                    maxLine = lines;
                    mSharedPreferences.putInt(ComAssisentSharedPreferences.PREF_MAX_LINE, maxLine);
                }
            }
        }
    };

    private CmdEditView.CmdEditViewCallback mCmdEditViewCallback = new CmdEditView.CmdEditViewCallback() {

        @Override
        public boolean open(SerialPortConfig config) {
            switch (config.name) {
                case "A":
                    mASerialPortHelper = new SerialPortHelper(ComAssisent.this, config);
                    mASerialPortHelper.setCallback(mSerialPortCallback);
                    updateViewsEnabled();
                    return mASerialPortHelper.open();

                case "B":
                    mBSerialPortHelper = new SerialPortHelper(ComAssisent.this, config);
                    mBSerialPortHelper.setCallback(mSerialPortCallback);
                    updateViewsEnabled();
                    return mBSerialPortHelper.open();

                case "C":
                    mCSerialPortHelper = new SerialPortHelper(ComAssisent.this, config);
                    mCSerialPortHelper.setCallback(mSerialPortCallback);
                    updateViewsEnabled();
                    return mCSerialPortHelper.open();

                case "D":
                    mDSerialPortHelper = new SerialPortHelper(ComAssisent.this, config);
                    mDSerialPortHelper.setCallback(mSerialPortCallback);
                    updateViewsEnabled();
                    return mDSerialPortHelper.open();

                default:
                    Log.w(ComAssisent.this, "selectCmdFile=>Unknow serial port.");
                    return false;
            }
        }

        @Override
        public void close(SerialPortConfig config) {
            switch (config.name) {
                case "A":
                    if (mASerialPortHelper != null) {
                        mASerialPortHelper.close();
                        mASerialPortHelper = null;
                    }
                break;

                case "B":
                    if (mBSerialPortHelper != null) {
                        mBSerialPortHelper.close();
                        mBSerialPortHelper = null;
                    }
                break;

                case "C":
                    if (mCSerialPortHelper != null) {
                        mCSerialPortHelper.close();
                        mCSerialPortHelper = null;
                    }
                break;

                case "D":
                    if (mDSerialPortHelper != null) {
                        mDSerialPortHelper.close();
                        mDSerialPortHelper = null;
                    }
                break;

                default:
                    Log.w(ComAssisent.this, "selectCmdFile=>Unknow serial port.");
            }
            updateViewsEnabled();
        }

        @Override
        public void selectCmdFile(SerialPortConfig config) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            int requestCode = -1;
            switch (config.name) {
                case "A":
                    requestCode = COM_A_REQUEST_CODE;
                    break;

                case "B":
                    requestCode = COM_B_REQUEST_CODE;
                    break;

                case "C":
                    requestCode = COM_C_REQUEST_CODE;
                    break;

                case "D":
                    requestCode = COM_D_REQUEST_CODE;
                    break;

                default:
                    Log.w(ComAssisent.this, "selectCmdFile=>Unknow serial port.");
                    return;
            }
            startActivityForResult(intent, requestCode);
        }

        @Override
        public void sendCommand(SerialPortConfig config, String cmd) {
            switch (config.name) {
                case "A":
                    if (mASerialPortHelper != null) {
                        mASerialPortHelper.sendCommand(cmd);
                        Data log = new Data();
                        log.isSend = true;
                        log.config = config;
                        log.date = new Date();
                        log.data = cmd;
                        mDispThread.addLogToQueue(log);
                    }
                    break;

                case "B":
                    if (mBSerialPortHelper != null) {
                        mBSerialPortHelper.sendCommand(cmd);
                        Data log = new Data();
                        log.isSend = true;
                        log.config = config;
                        log.date = new Date();
                        log.data = cmd;
                        mDispThread.addLogToQueue(log);
                    }
                    break;

                case "C":
                    if (mCSerialPortHelper != null) {
                        mCSerialPortHelper.sendCommand(cmd);
                        Data log = new Data();
                        log.isSend = true;
                        log.config = config;
                        log.date = new Date();
                        log.data = cmd;
                        mDispThread.addLogToQueue(log);
                    }
                    break;

                case "D":
                    if (mDSerialPortHelper != null) {
                        mDSerialPortHelper.sendCommand(cmd);
                        Data log = new Data();
                        log.isSend = true;
                        log.config = config;
                        log.date = new Date();
                        log.data = cmd;
                        mDispThread.addLogToQueue(log);
                    }
                    break;

                default:
                    Log.w(ComAssisent.this, "selectCmdFile=>Unknow serial port.");
            }
        }

        @Override
        public boolean sendCommandSequence(SerialPortConfig config, CmdSequence cmdSequence) {
            switch (config.name) {
                case "A":
                    mASendCmdThread = new SendCmdSequenceThread(mACmdEditView, mASerialPortHelper, cmdSequence);
                    mASendCmdThread.start();
                    break;

                case "B":
                    mBSendCmdThread = new SendCmdSequenceThread(mBCmdEditView, mBSerialPortHelper, cmdSequence);
                    mBSendCmdThread.start();
                    break;

                case "C":
                    mCSendCmdThread = new SendCmdSequenceThread(mCCmdEditView, mCSerialPortHelper, cmdSequence);
                    mCSendCmdThread.start();
                    break;

                case "D":
                    mDSendCmdThread = new SendCmdSequenceThread(mDCmdEditView, mDSerialPortHelper, cmdSequence);
                    mDSendCmdThread.start();
                    break;
            }
            return true;
        }

        @Override
        public void stopSendInnerCmd(SerialPortConfig config) {
            switch (config.name) {
                case "A":
                    mASendCmdThread.stopThread();
                    mASendCmdThread = null;
                    break;

                case "B":
                    mBSendCmdThread.stopThread();
                    mBSendCmdThread = null;
                    break;

                case "C":
                    mCSendCmdThread.stopThread();
                    mCSendCmdThread = null;
                    break;

                case "D":
                    mDSendCmdThread.stopThread();
                    mDSendCmdThread = null;
                    break;
            }
        }
    };

    private SerialPortHelper.SerialPortCallback mSerialPortCallback = new SerialPortHelper.SerialPortCallback() {
        @Override
        public void onError(SerialPortConfig config) {
            Log.e(ComAssisent.this, "mSerialPortCallback=>COM-" + config.name + " error.");
        }

        @Override
        public void onReceiveData(SerialPortConfig config, String data) {
            Log.d(ComAssisent.this, "mSerialPortCallback=>COM-" + config.name + " receive data: " + data);
            Data log = new Data();
            log.config = config;
            log.date = new Date();
            log.data = data;
            log.isSend = false;
            mDispThread.addLogToQueue(log);
        }
    };

    /**
     * 刷新显示线程
     */
    private class DispQueueThread extends Thread {

        private Queue<Data> mQueueList = new LinkedList<Data>();

        private boolean isStop;


        @Override
        public void run() {
            while (!isInterrupted()) {
                final Data log;
                final int maxLine = mSharedPreferences.getInt(ComAssisentSharedPreferences.PREF_MAX_LINE, ComAssisent.this.getResources().getInteger(R.integer.default_max_line));
                final boolean isAutoClear = mSharedPreferences.getBoolean(ComAssisentSharedPreferences.PREF_AUTO_CLEAR, ComAssisent.this.getResources().getBoolean(R.bool.default_auto_clear));
                final int fomartIndex = mSharedPreferences.getInt(ComAssisentSharedPreferences.PREF_LOG_FOMART_INDEX, ComAssisent.this.getResources().getInteger(R.integer.default_log_fomart_index));
                final int color = ComAssisent.this.getResources().getColor(R.color.log_header);
                while ((log = mQueueList.poll()) != null && !isStop) {
                    String header = "";
                    switch (fomartIndex) {
                        case 0:
                            header = "";
                            break;

                        case 1:
                            header = (log.isSend ? getString(R.string.send_data_title) : getString(R.string.receive_data_title));
                            break;

                        case 2:
                            header = log.getDateStr() +  (log.isSend ? getString(R.string.send_data_title) : getString(R.string.receive_data_title));
                            break;

                        case 3:
                            header = "[" + getString(R.string.serial_port_prefix) + log.config.name + "] ";
                            break;

                        case 4:
                            header = "[" + getString(R.string.serial_port_prefix) + log.config.name + "] " + (log.isSend ? getString(R.string.send_data_title) : getString(R.string.receive_data_title));
                            break;

                        case 5:
                            header = "[" + getString(R.string.serial_port_prefix) + log.config.name + "] " + log.getDateStr() +  (log.isSend ? getString(R.string.send_data_title) : getString(R.string.receive_data_title));
                            break;
                    }
                    String content = header + log.data + "\n";
                    final SpannableString span = new SpannableString(content);
                    span.setSpan(new ForegroundColorSpan(color), 0, header.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isAutoClear) {
                                if (mLines >= maxLine) {
                                    mLogEt.setText("");
                                    mLines = 0;
                                }
                            }
                            mLogEt.append(span);
                            mLines++;
                            mLogEt.setSelection(mLogEt.getText().length(), mLogEt.getText().length());
                        }
                    });
                    if (mQueueList.size() == 0) {
                        try {
                            Thread.sleep(100);//显示性能高的话，可以把此数值调小。
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                }
            }
        }

        public synchronized void addLogToQueue(Data log) {
            mQueueList.add(log);
        }

        public synchronized void stopThread() {
            isStop = true;
            notify();
        }
    }

    private class SendCmdSequenceThread extends Thread {

        private CmdEditView mCmdEditView;
        private SerialPortHelper mHelper;
        private CmdSequence mCmds;
        private boolean isStoped;

        public SendCmdSequenceThread(CmdEditView cev, SerialPortHelper sph, CmdSequence cmds) {
            mCmdEditView = cev;
            mHelper = sph;
            mCmds = cmds;
        }

        @Override
        public void run() {
            Cmd cmd = null;
            while ((cmd = mCmds.cmds.pop()) != null && !isStoped) {
                try {
                    sleep(cmd.delay);
                } catch (Exception ignore) {
                }
                mHelper.sendCommand(cmd.cmd);
            }
            if (!isStoped) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCCmdEditView.setSendInnerCmdFinish();
                    }
                });
            }
        }

        public void stopThread() {
            isStoped = true;
             try { interrupt(); } catch (Exception ignore) {}
        }
    }

    private class Data {
        public Date date;
        public SerialPortConfig config;
        public String data;
        public boolean isSend;

        public String getDateStr() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
            return sdf.format(date);
        }
    }
}
