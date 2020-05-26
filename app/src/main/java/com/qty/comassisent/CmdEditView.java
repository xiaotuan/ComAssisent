package com.qty.comassisent;

import android.content.Context;
import android.net.Uri;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.Gson;
import com.qty.comassisent.log.Log;
import com.qty.comassisent.serialport.SerialPortConfig;
import com.qty.comassisent.serialport.SerialPortFinder;
import com.qty.comassisent.util.FileUtils;
import com.qty.comassisent.util.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CmdEditView {

    private Context mContext;
    private View mView;
    private EditText mCmdEt;
    private Button mCmdSendBt;
    private Spinner mInnerCmdSpinner;
    private Button mSelectInnerCmdBt;
    private Button mInnerCmdSendBt;
    private Spinner mPortSpinner;
    private Spinner mBaudRateSpinner;
    private ToggleButton mSwitchTb;
    private RadioGroup mSendRg;
    private RadioButton mSendTxtRb;
    private RadioButton mSendHexRb;
    private EditText mSendBreakEt;
    private RadioGroup mReceiveRg;
    private RadioButton mReceiveTxtRb;
    private RadioButton mReceiveHexRb;
    private EditText mReceiveBreakEt;
    private EditText mSendDelayEt;
    private CheckBox mAutoSendCb;
    private ComAssisentSharedPreferences mSharedPreferences;
    private SerialPortConfig mConfig;
    private CmdEditViewCallback mCallback;
    private SerialPortFinder mSerialPortFinder;
    private ArrayList<CmdSequence> mCmds;

    private String mName;
    private boolean isOpened;
    private boolean isSendingInnerCmd;

    public CmdEditView(Context context, View view, String name) {
        mContext = context;
        mView = view;
        mName = name;
        initValues();
        updateConfig();
        initViews();
        updateViews();
        updateViewsEnabled();
    }

    public void setCallback(CmdEditViewCallback cb) {
        mCallback = cb;
    }

    public void setInnerCmdFileUri(Uri uri) {
        String path = FileUtils.getPath(mContext, uri);
        Log.d(this, "[" + mName + "]setInnerCmdFileUri=>path: " + path);
        if (path != null && !TextUtils.isEmpty(path.trim())) {
            mSharedPreferences.putString(ComAssisentSharedPreferences.PREF_INNER_CMD_FILE_PATH + mName, path.trim());
            updateCmdSquenceViews();
        }
    }

    public void setSendInnerCmdFinish() {
        isSendingInnerCmd = false;
        mSelectInnerCmdBt.setEnabled(true);
        mInnerCmdSendBt.setText(R.string.send_title);
    }

    private void initValues() {
        mSharedPreferences = new ComAssisentSharedPreferences(mContext);
        mConfig = new SerialPortConfig();
        mSerialPortFinder = new SerialPortFinder();
        mCmds = new ArrayList<>();
        isOpened = false;
        isSendingInnerCmd = false;
    }

    private void initViews() {
        mCmdEt = mView.findViewById(R.id.cmd);
        mCmdSendBt = mView.findViewById(R.id.send);
        mSelectInnerCmdBt = mView.findViewById(R.id.select_cmd_file);
        mInnerCmdSendBt = mView.findViewById(R.id.send_inner_cmd);
        mSwitchTb = mView.findViewById(R.id.switch_toggle);
        mSendRg = mView.findViewById(R.id.send_radio_group);
        mSendTxtRb = mView.findViewById(R.id.send_txt);
        mSendHexRb = mView.findViewById(R.id.send_hex);
        mSendBreakEt = mView.findViewById(R.id.send_line_break);
        mReceiveRg = mView.findViewById(R.id.receive_radio_group);
        mReceiveTxtRb = mView.findViewById(R.id.receive_txt);
        mReceiveHexRb = mView.findViewById(R.id.receive_hex);
        mReceiveBreakEt = mView.findViewById(R.id.receive_line_break);
        mSendDelayEt = mView.findViewById(R.id.send_delay);
        mAutoSendCb = mView.findViewById(R.id.auto_send);
        mInnerCmdSpinner = mView.findViewById(R.id.inner_cmd);
        mPortSpinner = mView.findViewById(R.id.port);
        mBaudRateSpinner = mView.findViewById(R.id.baud_rate);

        mCmdEt.addTextChangedListener(mCmdTextWatcher);
        mCmdSendBt.setOnClickListener(mCmdSendClick);
        mSelectInnerCmdBt.setOnClickListener(mSelectInnerCmdClick);
        mInnerCmdSendBt.setOnClickListener(mInnerCmdSendClick);
        mSwitchTb.setOnCheckedChangeListener(mSwitchCheckedChange);
        mSendRg.setOnCheckedChangeListener(mSendCheckedChange);
        mSendBreakEt.addTextChangedListener(mSendBreakTextWatcher);
        mReceiveRg.setOnCheckedChangeListener(mReceiveCheckedChange);
        mReceiveBreakEt.addTextChangedListener(mReceiveBreakTextWatcher);
        mSendDelayEt.addTextChangedListener(mSendDelayTextWatcher);
        mAutoSendCb.setOnCheckedChangeListener(mAutoSendCheckedChange);
        mPortSpinner.setOnItemSelectedListener(mPortItemSelected);
        mBaudRateSpinner.setOnItemSelectedListener(mBaudRateItemSelected);
    }

    private void updateConfig() {
        mConfig.name = mName;
        mConfig.path = getSerialPortPath();
        mConfig.baudRate = mSharedPreferences.getInt(ComAssisentSharedPreferences.PREF_BAUD_RATE + mName, 0);
        mConfig.commandath = getCommandPath();
        mConfig.isSendTxt = mSharedPreferences.getBoolean(ComAssisentSharedPreferences.PREF_SEND_TXT + mName, mContext.getResources().getBoolean(R.bool.default_send_show_text));
        mConfig.isReceiveShowTxt = mSharedPreferences.getBoolean(ComAssisentSharedPreferences.PREF_RECEIVE_SHOW_TXT + mName, mContext.getResources().getBoolean(R.bool.default_receive_show_text));
        mConfig.sendLineBreak = mSharedPreferences.getString(ComAssisentSharedPreferences.PREF_SEND_LINE_BREAK + mName, null);
        mConfig.receiveLineBreak = mSharedPreferences.getString(ComAssisentSharedPreferences.PREF_RECEIVE_LINE_BREAK + mName, null);
        mConfig.isAutoSend = mSharedPreferences.getBoolean(ComAssisentSharedPreferences.PREF_AUTO_SEND + mName, mContext.getResources().getBoolean(R.bool.default_auto_send));
        mConfig.autoSendDelay = mSharedPreferences.getInt(ComAssisentSharedPreferences.PREF_AUTO_SEND_DELAY + mName, mContext.getResources().getInteger(R.integer.default_auto_send_interval));
    }

    private void updateViews() {
        updateCmdSquenceViews();
        updateSerialPortPathView();
        updateBaudRateViews();
        updateSendSettingViews();
        updateReceiveSettingViews();
        updateAutoSendViews();
    }

    private void updateViewsEnabled() {
        mCmdEt.setEnabled(isOpened && !isSendingInnerCmd);
        mCmdSendBt.setEnabled(mCmdEt.getText().toString().length() > 0 && isOpened && !isSendingInnerCmd);
        mInnerCmdSpinner.setEnabled(!isSendingInnerCmd);
        mSelectInnerCmdBt.setEnabled(!isSendingInnerCmd);
        mInnerCmdSendBt.setEnabled(mCmds.size() > 0 && isOpened);
        mPortSpinner.setEnabled(!isOpened);
        mBaudRateSpinner.setEnabled(!isOpened);
        mSendRg.setEnabled(!isOpened);
        mSendTxtRb.setEnabled(!isOpened);
        mSendHexRb.setEnabled(!isOpened);
        mSendBreakEt.setEnabled(!isOpened);
        mReceiveRg.setEnabled(!isOpened);
        mReceiveTxtRb.setEnabled(!isOpened);
        mReceiveHexRb.setEnabled(!isOpened);
        mReceiveBreakEt.setEnabled(!isOpened);
        mSendDelayEt.setEnabled(!isOpened && mAutoSendCb.isChecked());
        mAutoSendCb.setEnabled(!isOpened);
    }

    private void updateCmdSquenceViews() {
        updateCmdSquences();
        updateInnerCmdSpinner();
        if (isSendingInnerCmd) {
            mInnerCmdSendBt.setText(R.string.stop_title);
        } else {
            mInnerCmdSendBt.setText(R.string.send_title);
        }
    }

    private void updateCmdSquences() {
        String path = mSharedPreferences.getString(ComAssisentSharedPreferences.PREF_INNER_CMD_FILE_PATH + mName, null);
        if (path != null) {
            mCmds = new ArrayList<>();
            FileReader fr = null;
            BufferedReader br = null;
            try {
                fr = new FileReader(new File(path));
                br = new BufferedReader(fr);
                StringBuffer sb = new StringBuffer();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                Log.d(this, "[" + mName + "]setInnerCmdFileUri=>content: " + sb.toString());
                JSONArray ja = new JSONArray(sb.toString());
                Gson gson = new Gson();
                for (int i = 0; i < ja.length(); i++) {
                    JSONObject jo = ja.getJSONObject(i);
                    CmdSequence sequence = new CmdSequence();
                    sequence.name = jo.getString("name");
                    sequence.cmds = new LinkedList<>();
                    JSONArray arr = jo.getJSONArray("commands");
                    for (int j = 0; j < arr.length(); j++) {
                        Cmd cmd = gson.fromJson(arr.getJSONObject(j).toString(), Cmd.class);
                        sequence.cmds.add(cmd);
                    }
                    mCmds.add(sequence);
                }
            } catch (Exception e) {
                Log.e(this, "[" + mName + "]setInnerCmdFileUri=>error: ", e);
            } finally {
                if (br != null) {
                    try { br.close(); } catch (Exception ignore) {}
                }
                if (fr != null) {
                    try { fr.close(); } catch (Exception ignore) {}
                }
            }
        }
    }

    private void updateInnerCmdSpinner() {
        List<String> cmdNames = getCmdSequenceNames();
        ArrayAdapter<String> innerCmdAdpater = new ArrayAdapter<String>(mContext,
                android.R.layout.simple_spinner_item, cmdNames);
        innerCmdAdpater.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mInnerCmdSpinner.setAdapter(innerCmdAdpater);
        mInnerCmdSpinner.setSelection(0);
    }

    private void updateSerialPortPathView() {
        String[] entryValues = mSerialPortFinder.getAllDevicesPath();
        List<String> allDevices = new ArrayList<String>();
        for (int i = 0; i < entryValues.length; i++) {
            allDevices.add(entryValues[i]);
        }
        ArrayAdapter<String> devicesAdpater = new ArrayAdapter<String>(mContext,
                android.R.layout.simple_spinner_item, allDevices);
        devicesAdpater.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPortSpinner.setAdapter(devicesAdpater);
        int index = 0;
        String lastPath = getSerialPortPath();
        if (lastPath != null) {
            for (int i = 0; i < allDevices.size(); i++) {
                if (lastPath.equals(allDevices.get(i))) {
                    index = i;
                    break;
                }
            }
        }
        mPortSpinner.setSelection(index);
    }

    private void updateBaudRateViews() {
        String[] baudRates = mContext.getResources().getStringArray(R.array.baudrates_value);
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < baudRates.length; i++) {
            list.add(baudRates[i]);
        }
        ArrayAdapter<String> baudRateAdpater = new ArrayAdapter<String>(mContext,
                android.R.layout.simple_spinner_item, list);
        baudRateAdpater.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBaudRateSpinner.setAdapter(baudRateAdpater);
        int index = 0;
        int rate = mSharedPreferences.getInt(ComAssisentSharedPreferences.PREF_BAUD_RATE + mName, mContext.getResources().getInteger(R.integer.default_baud_rate));
        for (int i = 0; i < list.size(); i++) {
            int item = Integer.parseInt(list.get(i));
            if (item == rate) {
                index = i;
                break;
            }
        }
        mBaudRateSpinner.setSelection(index);
    }

    private void updateSendSettingViews() {
        boolean isSendTxt = mSharedPreferences.getBoolean(ComAssisentSharedPreferences.PREF_SEND_TXT + mName, mContext.getResources().getBoolean(R.bool.default_send_show_text));
        mSendRg.check(isSendTxt ? R.id.send_txt : R.id.send_hex);
        String lineBreak = mSharedPreferences.getString(ComAssisentSharedPreferences.PREF_SEND_LINE_BREAK + mName, mContext.getResources().getString(R.string.default_send_line_break));
        mSendBreakEt.setText(lineBreak);
    }

    private void updateReceiveSettingViews() {
        boolean isShowTxt = mSharedPreferences.getBoolean(ComAssisentSharedPreferences.PREF_RECEIVE_SHOW_TXT + mName, mContext.getResources().getBoolean(R.bool.default_receive_show_text));
        mReceiveRg.check(isShowTxt ? R.id.receive_txt : R.id.receive_hex);
        String lineBreak = mSharedPreferences.getString(ComAssisentSharedPreferences.PREF_RECEIVE_LINE_BREAK + mName, mContext.getResources().getString(R.string.default_receive_line_break));
        mReceiveBreakEt.setText(lineBreak);
    }

    private void updateAutoSendViews() {
        int delayTime = mSharedPreferences.getInt(ComAssisentSharedPreferences.PREF_AUTO_SEND_DELAY + mName, mContext.getResources().getInteger(R.integer.default_auto_send_interval));
        mSendDelayEt.setText(delayTime + "");
        boolean autoSend = mSharedPreferences.getBoolean(ComAssisentSharedPreferences.PREF_AUTO_SEND + mName, mContext.getResources().getBoolean(R.bool.default_auto_send));
        mAutoSendCb.setChecked(autoSend);
    }

    private String getSerialPortPath() {
        String path = mSharedPreferences.getString(ComAssisentSharedPreferences.PREF_CMD_FILE_PATH + mName, null);
        if (path != null) {
            String[] paths = mSerialPortFinder.getAllDevicesPath();
            boolean has = false;
            for (int i = 0; i < paths.length; i++) {
                if (path.equals(paths[i])) {
                    has = true;
                }
            }
            if (!has) {
                path = null;
                mSharedPreferences.remove(ComAssisentSharedPreferences.PREF_CMD_FILE_PATH + mName);
            }
        }
        return path;
    }

    private String getCommandPath() {
        String path = mSharedPreferences.getString(ComAssisentSharedPreferences.PREF_INNER_CMD_FILE_PATH + mName, null);
        if (path != null && !TextUtils.isEmpty(path.trim())) {
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                path = null;
                mSharedPreferences.remove(ComAssisentSharedPreferences.PREF_INNER_CMD_FILE_PATH + mName);
            }
        }
        return path;
    }

    private ArrayList<String> getCmdSequenceNames() {
        ArrayList<String> list = new ArrayList<>();
        if (mCmds.size() == 0) {
            list.add(mContext.getString(R.string.select_cmd_file));
        } else {
            for (int i = 0; i < mCmds.size(); i++) {
                list.add(mCmds.get(i).name);
            }
        }
        return list;
    }

    private boolean isCorrectLineBreak(String lineBreak) {
        if (TextUtils.isEmpty(lineBreak)) return true;
        String[] breaks = lineBreak.trim().split(" ");
        boolean correct = true;
        for (int i = 0; i < breaks.length; i++) {
            if (breaks[i].length() != 2 || !Utils.isHexString(breaks[i])) {
                correct = false;
                break;
            }
        }
        return correct;
    }

    private TextWatcher mCmdTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            String cmd = mCmdEt.getText().toString();
            mCmdSendBt.setEnabled(mCmdEt.getText().toString().length() > 0 && isOpened && !isSendingInnerCmd);
        }
    };

    private View.OnClickListener mCmdSendClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCallback != null) {
                String cmd = mCmdEt.getText().toString();
                mCallback.sendCommand(mConfig, cmd);
            } else {
                Log.w(CmdEditView.this, "[" + mName + "]mCmdSendClick=>Callback is null.");
            }
        }
    };

    private View.OnClickListener mSelectInnerCmdClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
           if (mCallback != null) {
                mCallback.selectCmdFile(mConfig);
           }
        }
    };

    private View.OnClickListener mInnerCmdSendClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCallback != null) {
                if (!isSendingInnerCmd) {
                    CmdSequence cmd = mCmds.get(mInnerCmdSpinner.getSelectedItemPosition());
                    isSendingInnerCmd = mCallback.sendCommandSequence(mConfig, cmd);
                    mSelectInnerCmdBt.setEnabled(isSendingInnerCmd);
                    mInnerCmdSendBt.setText(R.string.stop_title);
                } else {
                    isSendingInnerCmd = false;
                    mCallback.stopSendInnerCmd(mConfig);
                    mInnerCmdSendBt.setText(R.string.send_title);
                }
                mCmdEt.setEnabled(isOpened && !isSendingInnerCmd);
                mCmdSendBt.setEnabled(mCmdEt.getText().toString().length() > 0 && isOpened && !isSendingInnerCmd);
            } else {
                Log.w(CmdEditView.this, "[" + mName + "]mInnerCmdSendClick=>Callback is null.");
            }
        }
    };

    private CompoundButton.OnCheckedChangeListener mSwitchCheckedChange = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mCallback != null) {
                if (isChecked) {
                    isOpened = mCallback.open(mConfig);
                    if (!isOpened) {
                        mSwitchTb.setChecked(false);
                    }
                } else {
                    mCallback.close(mConfig);
                    isOpened = false;
                }
            } else {
                Log.w(CmdEditView.this, "[" + mName + "]mSwitchCheckedChange=>Callback is null.");
                if (isChecked) {
                    mSwitchTb.setChecked(false);
                }
                isOpened = false;
            }
            updateViewsEnabled();
        }
    };

    private RadioGroup.OnCheckedChangeListener mSendCheckedChange = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            boolean isTxt = (checkedId == R.id.send_txt);
            mConfig.isSendTxt = isTxt;
            mSharedPreferences.putBoolean(ComAssisentSharedPreferences.PREF_SEND_TXT + mName, isTxt);
        }
    };

    private TextWatcher mSendBreakTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            String lineBreak = mSendBreakEt.getText().toString().trim();
            if (isCorrectLineBreak(lineBreak)) {
                mSharedPreferences.putString(ComAssisentSharedPreferences.PREF_SEND_LINE_BREAK + mName, lineBreak);
                mConfig.sendLineBreak = lineBreak;
            } else {
                Toast.makeText(mContext, R.string.line_break_limit, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private RadioGroup.OnCheckedChangeListener mReceiveCheckedChange = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            boolean isTxt = (checkedId == R.id.receive_txt);
            mConfig.isReceiveShowTxt = isTxt;
            mSharedPreferences.putBoolean(ComAssisentSharedPreferences.PREF_RECEIVE_SHOW_TXT + mName, isTxt);
        }
    };

    private CompoundButton.OnCheckedChangeListener mAutoSendCheckedChange = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.d(CmdEditView.this, "[" + mName + "]mAutoSendCheckedChange=>isChecked: " + isChecked);
            mSendDelayEt.setEnabled(isChecked);
            mSharedPreferences.putBoolean(ComAssisentSharedPreferences.PREF_AUTO_SEND + mName, isChecked);
            mConfig.isAutoSend = isChecked;
        }
    };

    private TextWatcher mReceiveBreakTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            String lineBreak = mReceiveBreakEt.getText().toString().trim();
            if (isCorrectLineBreak(lineBreak)) {
                mSharedPreferences.putString(ComAssisentSharedPreferences.PREF_RECEIVE_LINE_BREAK + mName, lineBreak);
                mConfig.receiveLineBreak = lineBreak;
            } else {
                Toast.makeText(mContext, R.string.line_break_limit, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private TextWatcher mSendDelayTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            String time = mSendDelayEt.getText().toString().trim();
            if (!TextUtils.isEmpty(time)) {
                try {
                    int delayTime = Integer.parseInt(time);
                    if (delayTime >= 0) {
                        mSharedPreferences.putInt(ComAssisentSharedPreferences.PREF_AUTO_SEND_DELAY + mName, delayTime);
                        mConfig.autoSendDelay = delayTime;
                    } else {
                        delayTime = mSharedPreferences.getInt(ComAssisentSharedPreferences.PREF_AUTO_SEND_DELAY + mName, mContext.getResources().getInteger(R.integer.default_auto_send_interval));
                        mSendDelayEt.setText(delayTime + "");
                    }
                } catch (Exception e) {
                    Toast.makeText(mContext, R.string.delay_time_limit, Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private AdapterView.OnItemSelectedListener mPortItemSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Log.d(CmdEditView.this, "[" + mName + "]onItemSelected=>position: " + position + ", value: " + mPortSpinner.getSelectedItem());
            mConfig.path = (String)mPortSpinner.getSelectedItem();
            mSharedPreferences.putString(ComAssisentSharedPreferences.PREF_CMD_FILE_PATH + mName, (String)mPortSpinner.getSelectedItem());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    };

    private AdapterView.OnItemSelectedListener mBaudRateItemSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String item = (String)mBaudRateSpinner.getSelectedItem();
            int rate = Integer.parseInt(item);
            mSharedPreferences.putInt(ComAssisentSharedPreferences.PREF_BAUD_RATE + mName, rate);
            mConfig.baudRate = rate;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    };

    public interface CmdEditViewCallback {
        boolean open(SerialPortConfig config);
        void close(SerialPortConfig config);
        void selectCmdFile(SerialPortConfig config);
        void sendCommand(SerialPortConfig config, String cmd);
        boolean sendCommandSequence(SerialPortConfig config, CmdSequence cmdSequence);
        void stopSendInnerCmd(SerialPortConfig config);
    }

}
