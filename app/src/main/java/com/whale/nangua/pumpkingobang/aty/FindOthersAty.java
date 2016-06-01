package com.whale.nangua.pumpkingobang.aty;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.whale.nangua.pumpkingobang.Config;
import com.whale.nangua.pumpkingobang.R;
import com.whale.nangua.pumpkingobang.adapter.DeviceshowAdapter;
import com.whale.nangua.pumpkingobang.bean.Device;
import com.whale.nangua.pumpkingobang.utils.ClsUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

/**
 * Created by nangua on 2016/5/31.
 */
public class FindOthersAty extends Activity {
    //初始化组件
    private Button btn_saomiao;
    private ListView saomiao_lv;
    //用户ListView显示的储存device名字与地址数组
    private ArrayList<Device> deviceNameAndDresss;
    private ArrayList<BluetoothDevice> devices;
    private DeviceshowAdapter deviceshowAdapter;

    //蓝牙组件
    private BluetoothAdapter bluetoothAdapter = null;
    //蓝牙扫描广播接收器
    private BluetoothReceiver bluetoothReceiver = null;
    //蓝牙ServerSocket
    BluetoothServerSocket bluetoothServerSocket = null;
    //客户端连接后服务端的Socket
    BluetoothSocket fuwuSocket = null;
    //作为客户端的Socket
    BluetoothSocket kehuduanSocket= null;
    //作为客户端连接服务端时需要的表示服务端的远程设备对象
    BluetoothDevice kehuduanDevice = null;

    private static boolean isQuering = false;

    private TextView xianshikuang;
    private EditText shurukuang;
    private Button fasongbtn;

    private Handler mhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(FindOthersAty.this,"收到了吗数据？",Toast.LENGTH_SHORT).show();
            xianshikuang.setText("要接收了");
            xianshikuang.setText(shuju);
        }
    };


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_layout);

        //初始化视图
        initView();
    }


    private void initView() {
        xianshikuang = (TextView) findViewById(R.id.xianshikuang);
        shurukuang = (EditText) findViewById(R.id.shurukuang);
        fasongbtn = (Button) findViewById(R.id.fasongbtn);
        fasongbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String temp = shurukuang.getText().toString();
                if (connectedThread!=null) {
                    connectedThread.write(temp.getBytes() );
                }
            }
        });

        //得到扫描周围蓝牙设备按钮
        btn_saomiao = (Button) findViewById(R.id.saomiao_btn);
        //扫描周围设备的ListView
        saomiao_lv = (ListView) findViewById(R.id.saomiao_lv);
        //设备信息ArrayList
        deviceNameAndDresss = new ArrayList<>();
        //设备ArrayList
        devices = new ArrayList<>();
        //显示蓝牙设备信息的adapter
        deviceshowAdapter = new DeviceshowAdapter(this,deviceNameAndDresss);
        saomiao_lv.setAdapter(deviceshowAdapter);
        //绑定扫描周围蓝牙设备按钮监听器
        btn_saomiao.setOnClickListener(new SaoMiaoButtonListener());


        //得到本机蓝牙设备的adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            //设置手机蓝牙可见性
            openBlueTooth();
        }
        final String benjiname = bluetoothAdapter.getName();//本机名称
        String benjidizhi = bluetoothAdapter.getAddress();//本机地址
        Log.d("qqqqqq",benjidizhi);


        //开启子线程等待连接
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //开启服务端
                    //等待客户端接入
                    while (true) {
                        bluetoothServerSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(benjiname, Config.UUID);
                        fuwuSocket = bluetoothServerSocket.accept();
                        if (fuwuSocket.isConnected()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    xianshikuang.setText("接收挑战请求，建立连接成功！");
                                }
                            });
//******
                                //得到输入输出流
                                InputStream in  =fuwuSocket.getInputStream();
                            DataInputStream datains = new DataInputStream(in);
                            String command = datains.readUTF();
                                    Log.d("whalea", "读到的数据！：" + command);
//******

                            //初始化线程来传输数据
                           // manageConnectedSocket(fuwuSocket);
                            //得到连接之后关闭ServerSocket
                           // bluetoothServerSocket.close();
                            //打断线程
                         //   Thread.interrupted();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("whalea", "没读到的原因！：" + e.getMessage());
                }
            }
        }).start();


        //获得本地蓝牙设备状态，这里如果是打开状态可以判断进入下一个界面
        bluetoothAdapter.getState();
        /**
         *  getState()获取本地蓝牙适配器当前状态（感觉可能调试的时候更需要）
            isDiscovering()判断当前是否正在查找设备，是返回true
            isEnabled()判断蓝牙是否打开，已打开返回true，否则，返回false
         */



        //设置广播获得未配对可检测的蓝牙设备
        //创建一个IntentFilter对象,将其action指定为BluetoothDevice.ACTION_FOUND
        //IntentFilter它是一个过滤器,只有符合过滤器的Intent才会被我们的BluetoothReceiver所接收
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //创建一个BluetoothReceiver对象
        bluetoothReceiver = new BluetoothReceiver();
        //设置广播的优先级为最大
        intentFilter.setPriority(Integer.MAX_VALUE);
        //注册广播接收器 注册完后每次发送广播后，BluetoothReceiver就可以接收到这个广播了
        registerReceiver(bluetoothReceiver, intentFilter);



        //设置ListView的点击监听
        saomiao_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                //自己主动去连接
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceNameAndDresss.get(position).getDeviceAddress());
                Boolean result = false;
                try {
                    //先进行配对
                    //如果没有配对
                    Log.d("whalea","开始配对");
                    if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                        Method createBondMethod = null;
                        createBondMethod = BluetoothDevice.class
                                .getMethod("createBond");

                        Log.d("whalea", "开始配对");
                        result = (Boolean) createBondMethod.invoke(device);
                    }



                    //如果已经配对好了
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED){
                        //获得客户端Socket
                        kehuduanSocket = device.createRfcommSocketToServiceRecord(Config.UUID);

                        final AlertDialog aDialog = new AlertDialog.Builder(FindOthersAty.this).
                                setTitle("发起对战").
                                setMessage("确认挑战玩家：" + deviceNameAndDresss.get(position).getDeviceName() + "吗？")
                                .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                //先停止扫描，以防止之后的连接被阻塞
                                                bluetoothAdapter.cancelDiscovery();
                                                try {
                                                    //开始连接，发送连接请求
                                                    kehuduanSocket.connect();
                                                    if (!bluetoothAdapter.isEnabled()) {
                                                        bluetoothAdapter.enable();
                                                    }


                                                    if (kehuduanSocket.isConnected()) {

                                                        try {
                                                            OutputStream out = kehuduanSocket.getOutputStream();
                                                            DataOutputStream dataout = new DataOutputStream(out);
                                                            //发送给服务器需要下载的文件和断点
                                                            dataout.writeUTF("阔爱的小鲸鱼");
                                                            Log.d("whalea", "阔爱的小鲸鱼");
                                                        } catch (IOException e) {
                                                            Log.d("whalea", "写不出的原因:" + e.getMessage());
                                                        }

                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Toast.makeText(FindOthersAty.this, "连接成功！！", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });


                                                    }


                                                } catch (final IOException e) {


                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {

                                                            Toast.makeText(FindOthersAty.this, "连接失败！！" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                            Log.d("whalea", e.getMessage());

                                                        }
                                                    });


                                                 /*   try {
                                                        kehuduanSocket.close();
                                                    } catch (IOException e1) {
                                                    }
                                                    return;*/
                                                }


                                                // manageConnectedSocket(kehuduanSocket);
                                                //之后关闭socket，清除内部资源
                                          /*      try {
                                                    kehuduanSocket.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }*/
                                            }
                                        }).start();
                                    }
                                })
                                .setPositiveButton("取消", null).show();
                }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        });
    }


    //扫描周围的蓝牙设备按钮监听器
    private class SaoMiaoButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            isQuering = true;
            Toast.makeText(FindOthersAty.this,"开始扫描",Toast.LENGTH_SHORT).show();
            //清空列表
            deviceNameAndDresss.clear();
            //获得已配对的蓝牙设备
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    deviceNameAndDresss.add(new Device(device.getName(),device.getAddress()));
                    devices.add(device);
                }
            }
            deviceshowAdapter.setDevices(deviceNameAndDresss);
            deviceshowAdapter.notifyDataSetChanged();
            //开始扫描周围的可见的蓝牙设备
            bluetoothAdapter.startDiscovery();
        }
    }

    /**
     * 打开蓝牙设备，设置可见性。
     */
    private void openBlueTooth() {
        //创建一个Intent对象,并且将其action的值设置为BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE也就是蓝牙设备设置为可见状态
        Intent kejianxingIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        //将一个键值对存放到Intent对象当中,主要用于指定可见状态的持续时间,大于300秒,就认为是300秒
        kejianxingIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 500);
        //执行请求
        startActivity(kejianxingIntent);
    }

    //接收广播

    /**
     * 接受广播，并显示尚未配对的可用的周围所有蓝牙设备
     */
    private class BluetoothReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //如果是正在扫描状态
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //只要BluetoothReceiver接收到来自于系统的广播,这个广播是什么呢,是我找到了一个远程蓝牙设备
                //Intent代表刚刚发现远程蓝牙设备适配器的对象,可以从收到的Intent对象取出一些信息
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 如果该设备已经被配对，则跳过
              //  if (bluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                    //设备数组获得新的设备信息并更新adapter
                    deviceNameAndDresss.add(new Device(bluetoothDevice.getName(), bluetoothDevice.getAddress()));
                    deviceshowAdapter.notifyDataSetChanged();
                    Toast.makeText(FindOthersAty.this,"aaaaaaaaaa",Toast.LENGTH_LONG).show();
                    //添加新的设备到设备Arraylist
                    devices.add(bluetoothDevice);
               // }
            }
        }
    }

    //数据传输线程
    ConnectedThread connectedThread;

    //初始化线程来传输或接收数据
    private void manageConnectedSocket(BluetoothSocket socket) {
        //在一个线程中执行数据传输
          connectedThread = new ConnectedThread(socket);

        connectedThread.start();


    }

    /**
     * 连接的线程
     */
    private class ConnectedThread extends Thread {
        //传入的socket
        private final BluetoothSocket mmSocket;
        //输入流
        private final InputStream mmInStream;
        //输出流
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //得到输入输出流，因为成员变量流是final的所以这里要用temp传递
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            String word = " wocaonima";
            this.write(word.getBytes());
            byte[] buffer = new byte[1024];  // 存储内容的byte数组
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // 从输入流中得到信息
                    bytes = mmInStream.read(buffer);
                    Log.d("whalea",bytes+"长度");
                    // Send the obtained bytes to the UI activity
                    Message message = new Message();
                    message.what= 1;
                    shuju = buffer.toString();
                    Log.d("whalea",buffer.toString() + "数据");
                    mhandler.sendMessage(message);

                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                mmOutStream.flush();
            } catch (IOException e) {
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private static String shuju;


}
