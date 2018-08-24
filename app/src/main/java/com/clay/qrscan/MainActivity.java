package com.clay.qrscan;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.zxing.android.CaptureActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //别忘了在清单文件注册服务，以及IMEI权限读取的加入
    public static String MqttUserString = "yang";//用户名
    public static String MqttPwdString = "11223344";//密码
    public static String MqttIPString = "47.93.19.134";//IP地址
    public static int MqttPort = 1883;//端口号
//    public static String SubscribeString = "/sub";//订阅的主题
//    public static String PublishString = "/pub";//发布的主题

    private static final int REQUEST_CODE_SCAN = 0x0000;
    private static final String DECODED_CONTENT_KEY = "codedContent";

    AdapterDevices mAdapterDevices;
    ArrayList<ArrayList<Object>> mArrayList = new ArrayList<ArrayList<Object>>();
    ArrayList<String> arrayListDeviceID = new ArrayList<String>();//记录ClientID
    ArrayList<String> arrayListDeviceName = new ArrayList<String>();//记录设备名字
    ArrayList<String> arrayListDeviceSWStatus = new ArrayList<String>();//记录设备开关的状态
    ListView listView;

    private DataBaseDeviceMessage mDataBaseDeviceMessage;//数据库
    String NowDevicesClientID = "";//当前选择的设备的ClientID
    String NowDevicesName = "";//当前选择的设备的名字
    String NowDevicesSwitchStatus = "0";//选择的设备开关的状态
    int NowDevicesIndex = 0;//当前选择的设备的Index

    Vibrator vibrator;//按钮按下震动

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        vibrator=(Vibrator)getSystemService(Service.VIBRATOR_SERVICE);//震动

        mDataBaseDeviceMessage = new DataBaseDeviceMessage(MainActivity.this, "DevicesMsgDatabase.db", 1);
        Cursor cursor = mDataBaseDeviceMessage.query(null, null, null, null, null, null, null, null);
        while (cursor.moveToNext())
        {
            String mString1 = cursor.getString(cursor.getColumnIndex(DataBaseDeviceMessage.DeviceClientID));
            String mString2 = cursor.getString(cursor.getColumnIndex(DataBaseDeviceMessage.DeviceName));
            String mString3 = cursor.getString(cursor.getColumnIndex(DataBaseDeviceMessage.DeviceSwitchStatus));
            String mString4 = cursor.getString(cursor.getColumnIndex(DataBaseDeviceMessage.OtherData1));
            String mString5 = cursor.getString(cursor.getColumnIndex(DataBaseDeviceMessage.OtherData2));

            arrayListDeviceID.add(mString1);
            arrayListDeviceName.add(mString2);
            arrayListDeviceSWStatus.add(mString3);
            ArrayList<Object> arrayList = new ArrayList<Object>();
            arrayList.add(0,mString2);
            arrayList.add(1,mString3);
            mArrayList.add(arrayList);
        }
        cursor.close();

        listView = (ListView) findViewById(R.id.listViewActiMain1);
        listView.setBackgroundColor(Color.WHITE);
        listView.setOnItemLongClickListener(ItemLongClickListener);//长按
        listView.setOnItemClickListener(OnItemClickListener);

        mAdapterDevices = new AdapterDevices(MainActivity.this, mArrayList);
        mAdapterDevices.setonItemSwitchClickListener(new AdapterDevices.onItemSwitchClickListener() {
            @Override
            public void onClick(View imageView, int index, String DeviceName, String isClick) {

            }
        });
        mAdapterDevices.setonItemSwitchClickListener(mItemSwitchClickListener);
        listView.setAdapter(mAdapterDevices);
        mAdapterDevices.notifyDataSetChanged();


    }

    /**
     * 选择点击了设备
     */
    private OnItemClickListener OnItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                long arg3) {
            // TODO Auto-generated method stub
//            Toast.makeText(getApplicationContext(), "您点击了"+arg2, 500).show();
            NowDevicesClientID = arrayListDeviceID.get(arg2);
            NowDevicesName = arrayListDeviceName.get(arg2);
            NowDevicesSwitchStatus = arrayListDeviceSWStatus.get(arg2);
            NowDevicesIndex = arg2;
        }
    };

    /**
     * 长按
     */
    private OnItemLongClickListener ItemLongClickListener = new OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
//            Toast.makeText(getApplicationContext(), "您长按了"+arg2, 500).show();
            NowDevicesClientID = arrayListDeviceID.get(arg2);
            NowDevicesName = arrayListDeviceName.get(arg2);
            NowDevicesSwitchStatus = arrayListDeviceSWStatus.get(arg2);
            NowDevicesIndex = arg2;

            Vibrate();//震动

            EditDeleteDialog();
            return false;
        }
    };

    /**
     * 开关点击
     */
    private AdapterDevices.onItemSwitchClickListener mItemSwitchClickListener = new AdapterDevices.onItemSwitchClickListener() {
        @Override
        public void onClick(View imageView, int index, String DeviceName, String isClick) {

            NowDevicesClientID = arrayListDeviceID.get(index);
            NowDevicesName = arrayListDeviceName.get(index);
            NowDevicesIndex = index;

            String sendString = "";

            ArrayList<Object> arrayList = new ArrayList<Object>();
            if (isClick.equals("1")) {//气死我了，注意这里是字符串类型
                arrayList.add(0,DeviceName);
                arrayList.add(1,"0");//这里也是字符串类型
                NowDevicesSwitchStatus = "0";
                sendString = "switch;relay=0";
            }
            else {
                arrayList.add(0,DeviceName);
                arrayList.add(1,"1");
                NowDevicesSwitchStatus = "1";
                sendString = "switch;relay=1";
            }
            mArrayList.set(index, arrayList);
            mAdapterDevices.notifyDataSetChanged();

            arrayListDeviceSWStatus.set(NowDevicesIndex, NowDevicesSwitchStatus);
            mDataBaseDeviceMessage.update(NowDevicesClientID, null, NowDevicesSwitchStatus, null, null);

            Intent intent = new Intent();
            intent.setAction("ActivitySendMqttService");
            intent.putExtra("OtherActivitySend","SendData"+";;"+"device/"+NowDevicesClientID+";;"+sendString);
            sendBroadcast(intent);

        }
    };

    /*编辑和删除设备对话框*/
    private void EditDeleteDialog()
    {
        AlertDialog.Builder EditDeleteDialog = new AlertDialog.Builder(MainActivity.this);
        View mView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_edit_delete, null);
        EditDeleteDialog.setView(mView);
        EditDeleteDialog.setTitle("设备选项");

        final EditText dialogEditText = (EditText) mView.findViewById(R.id.editTextDialogED1);
        dialogEditText.setText(NowDevicesName);
        dialogEditText.setSelection(NowDevicesName.length());//将光标移至文字末尾

        EditDeleteDialog.setPositiveButton("确认",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                String DeviceName = dialogEditText.getText().toString();

                ArrayList<Object> arrayList = new ArrayList<Object>();
                arrayList.add(0,DeviceName);//名字
                arrayList.add(1,NowDevicesSwitchStatus);//开关状态
                mArrayList.set(NowDevicesIndex, arrayList);
                mAdapterDevices.notifyDataSetChanged();//更新显示

                mDataBaseDeviceMessage.update(NowDevicesClientID,DeviceName,null, null,null);

                arrayListDeviceName.set(NowDevicesIndex,DeviceName);
            }
        });

        EditDeleteDialog.setNeutralButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDataBaseDeviceMessage.delete(NowDevicesClientID);
                arrayListDeviceID.remove(NowDevicesIndex);
                arrayListDeviceName.remove(NowDevicesIndex);
                arrayListDeviceSWStatus.remove(NowDevicesIndex);

                mArrayList.remove(NowDevicesIndex);
                mAdapterDevices.notifyDataSetChanged();

                Intent intent = new Intent();//取消订阅设备的主题
                intent.setAction("ActivitySendMqttService");
                intent.putExtra("OtherActivitySend","UnSubscribUneTopic"+";;"+NowDevicesClientID);
                sendBroadcast(intent);
            }
        });

        EditDeleteDialog.setNegativeButton("取消",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        EditDeleteDialog.show();
    }

    /*该类的广播接收程序*/
    private BroadcastReceiver MainActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try
            {
                String msgString = intent.getStringExtra("MqttServiceSendToast");
                if (msgString != null)
                {
                    Toast.makeText(MainActivity.this,msgString,Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
            }
        }
    };

    /**
     * 返回数据,添加设备
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {
            if (data != null) {
                String DevicesID = data.getStringExtra(DECODED_CONTENT_KEY);

                int ddd = arrayListDeviceID.indexOf(DevicesID);

                Intent intent = new Intent();//订阅设备的主题
                intent.setAction("ActivitySendMqttService");
                intent.putExtra("OtherActivitySend","SubscribeTopic"+";;"+DevicesID);
                sendBroadcast(intent);

                if (arrayListDeviceID.indexOf(DevicesID) == -1) {//查询设备存不存在
                    ArrayList<Object> arrayList = new ArrayList<Object>();
                    arrayList.add(0,DevicesID);
                    arrayList.add(1,"0");
                    mArrayList.add(arrayList);
                    mAdapterDevices.notifyDataSetChanged();

                    arrayListDeviceID.add(DevicesID);//设备ID
                    arrayListDeviceName.add(DevicesID);//初始化设备名字
                    arrayListDeviceSWStatus.add("0");
                    mDataBaseDeviceMessage.insert(DevicesID, DevicesID, "0", "null", "null");
                }
                else {
                    DeviceIsHavaDialog(DevicesID);
                }
            }
        }
    }


    /*显示设备已经存在的对话框对话框*/
    private void DeviceIsHavaDialog(final String DeviceID)
    {
        AlertDialog.Builder DeviceIsHavaDialog = new AlertDialog.Builder(MainActivity.this);
        DeviceIsHavaDialog.setTitle("提示");
        DeviceIsHavaDialog.setMessage("该设备已经存在,是否覆盖");


        DeviceIsHavaDialog.setPositiveButton("是",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                int Index = arrayListDeviceID.indexOf(DeviceID);//得到要覆盖的设备的位置

                ArrayList<Object> arrayList = new ArrayList<Object>();
                arrayList.add(0,DeviceID);//名字
                arrayList.add(1,arrayListDeviceSWStatus.get(Index));//开关状态
                mArrayList.set(Index, arrayList);
                mAdapterDevices.notifyDataSetChanged();//更新显示
                mDataBaseDeviceMessage.update(DeviceID,DeviceID,null, null,null);
                arrayListDeviceName.set(Index,DeviceID);
            }
        });


        DeviceIsHavaDialog.setNegativeButton("否",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        DeviceIsHavaDialog.show();
    }

    /*震动*/
    private void Vibrate()
    {
        if (vibrator!=null) {
            vibrator.vibrate(new long[]{0,30}, -1);//震动
        }
    }

    /** 当活动即将可见时调用 */
    @Override
    protected void onStart() {
        Intent startIntent = new Intent(getApplicationContext(), ServiceMqtt.class);
        startService(startIntent); //启动后台服务

        IntentFilter filter = new IntentFilter();//监听的广播
        filter.addAction("Broadcast.MqttServiceSend");
        registerReceiver(MainActivityReceiver, filter);
//        Log.e("err","onStart");
        super.onStart();
    }

    /** 当活动不再可见时调用 */
    @Override
    protected void onStop() {

        super.onStop();
    }

    /** 当活动注销时调用 */
    @Override
    protected void onDestroy() {
        unregisterReceiver(MainActivityReceiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.CAMERA}, 1);
            }
        }
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SCAN);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
