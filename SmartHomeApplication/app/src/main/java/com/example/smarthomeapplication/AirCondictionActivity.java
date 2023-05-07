package com.example.smarthomeapplication;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



public class AirCondictionActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener, CompoundButton.OnCheckedChangeListener {

    private static final int REQUEST_CODE = 1;
    private TextView tv_air_temp;
    private TextView tv_air_model;

    private boolean Air_condition_status = false;
    private int Air_condition_Temp = 20;
    private int Air_condition_model =0;//1:制冷  2：制热   3除湿

    private ScheduledExecutorService scheduler;
    private MqttClient client;
    private Handler handler;

    private  String host = "tcp://82.157.54.211:1883";     // TCP协议
    private  String userName = "AIXIN698";
    private  String passWord = "AIXIN698";
    private  String mqtt_id = "1353023461_mqtt";
    private  String mqtt_sub_topic = "/mvzzzsmarthomedemo/pub";
    private  String mqtt_pub_topic = "/mzzzsmarthomedemo/sub";

    private int mCount = 0;



    private boolean isSubscribed = false;
    boolean isConnected = false;
    private CheckBox ck_power;
    private Boolean Data_Source_Mcu = false ;
    private RadioButton rb_cold;
    private RadioButton rb_hot;
    private RadioButton rb_dehum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_air_condiction);

        ImageButton btn_air_back = findViewById(R.id.btn_air_back);
        ImageButton Ibtn_add = findViewById(R.id.Ibtn_add);
        ImageButton Ibtn_sub = findViewById(R.id.Ibtn_sub);
        RadioGroup rg_model= findViewById(R.id.rg_model);
        rb_cold = findViewById( R.id.rb_cold);
        rb_hot = findViewById( R.id.rb_hot);
        rb_dehum = findViewById( R.id.rb_dehum);


        ck_power = findViewById(R.id.ck_power);

        tv_air_temp = findViewById(R.id.tv_air_temp);
        btn_air_back.setOnClickListener(this);
        Ibtn_add.setOnClickListener(this);
        Ibtn_sub.setOnClickListener(this);
        ck_power.setOnCheckedChangeListener(this);
        rg_model.setOnCheckedChangeListener(this);

        tv_air_model = findViewById(R.id.tv_air_model);
        tv_air_temp = findViewById(R.id.tv_air_temp);
        int mCount = Integer.parseInt(tv_air_temp.getText().toString());
        Air_condition_Temp = mCount;

        onActivityResult();

        //k_power.setEnabled(false);//开启按钮设置为禁用
        udatdaTemp();
        Mqtt_init();
        startReconnect();
        handler = new Handler(Looper.myLooper()) {
            @SuppressLint("SetTextI18n")
            public void handleMessage(Message msg) {

                super.handleMessage(msg);
                switch (msg.what){
                    case 1: //开机校验更新回传
                        break;
                    case 2:  // 反馈回传

                        break;
                    case 3:  //MQTT 收到消息回传   UTF8Buffer msg=new UTF8Buffer(object.toString());
                        break;
                    case 30:  //连接失败
                        Toast.makeText(AirCondictionActivity.this,"连接失败" ,Toast.LENGTH_SHORT).show();
                        break;
                    case 31:   //连接成功
                        if (!isConnected) {
                            isConnected = true;
                            Toast.makeText(AirCondictionActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                            try {
                                client.subscribe(mqtt_sub_topic, 1);
                                isSubscribed = true;  // 标记已经订阅
                            } catch (MqttException e) {
                                e.printStackTrace();
                            }
                        }
                        break;

                    default:
                        break;
                }
            }
        };

    }


    protected void onActivityResult() {

            Intent intent = getIntent();

            Air_condition_status = intent.getBooleanExtra("air_condition_power", false);
            Air_condition_Temp = intent.getIntExtra("air_condition_temp", 0);
            Air_condition_model = intent.getIntExtra("air_condition_model", 0);
            Data_Source_Mcu = intent.getBooleanExtra("data_source_mcn",false);
            // 在此处更新UI或执行其他操作

            if(Air_condition_status) {
                ck_power.setChecked(true);
            }else {
                ck_power.setChecked(false) ;
            }
            udatdaTemp();
            switch (Air_condition_model) {
                case 1 : rb_cold.setChecked(true);
                    break;
                case 2 :rb_hot.setChecked(true);
                    break;
                case 3 :rb_dehum.setChecked(true);
                    break;
            }

    }


    private void udatdaTemp() {
        tv_air_temp.setText(String.valueOf(Air_condition_Temp));
    }

    private void Mqtt_init()
    {
        try {
            //host为主机名，test为clientid即连接MQTT的客户端ID，一般以客户端唯一标识符表示，MemoryPersistence设置clientid的保存形式，默认为以内存保存
            client = new MqttClient(host, mqtt_id,
                    new MemoryPersistence());
            //MQTT的连接设置
            MqttConnectOptions options = new MqttConnectOptions();
            //设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            options.setCleanSession(false);
            //设置连接的用户名
            options.setUserName(userName);
            //设置连接的密码
            options.setPassword(passWord.toCharArray());
            // 设置超时时间 单位为秒
            options.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            options.setKeepAliveInterval(20);
            //设置回调
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    //连接丢失后，一般在这里面进行重连
                    System.out.println("connectionLost----------");
                    //startReconnect();
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    //publish后会执行到这里
                    System.out.println("deliveryComplete---------"
                            + token.isComplete());
                }
                @Override
                public void messageArrived(String topicName, MqttMessage message)
                        throws Exception {
                    //subscribe后得到的消息会执行到这里面
                    System.out.println("messageArrived----------");
                    Message msg = new Message();
                    msg.what = 3;   //收到消息标志位
//                    msg.obj = topicName + "---" +message.toString();
                    msg.obj = message.toString();
                    handler.sendMessage(msg);    // hander 回传
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // MQTT连接函数
    private void Mqtt_connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(!(client.isConnected()) )  //如果还未连接
                    {
                        MqttConnectOptions options = null;
                        client.connect(options);
                        Message msg = new Message();
                        msg.what = 31;
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = new Message();
                    msg.what = 30;
                    handler.sendMessage(msg);
                }
            }
        }).start();
    }

    // MQTT重新连接函数
    private void startReconnect() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!client.isConnected()) {
                    Mqtt_connect();
                }
            }
        }, 0*1000, 10 * 1000, TimeUnit.MILLISECONDS);
    }

    // 订阅函数    (下发任务/命令)
    private void publishmessageplus(String topic,String message2)
    {
        if (client == null || !client.isConnected()) {
            return;
        }
        MqttMessage message = new MqttMessage();
        message.setPayload(message2.getBytes());
        try {
            client.publish(topic,message);
        } catch (MqttException e) {

            e.printStackTrace();
        }
    }
    private void updateCount() {
        tv_air_temp.setText(String.valueOf(mCount));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_air_back:
                Intent intent = new Intent();
                intent.putExtra("Air_condition_Power",Air_condition_status);
                intent.putExtra("Air_condition_Temp",Air_condition_Temp);
                intent.putExtra("Air_condition_Model",Air_condition_model);
                setResult(RESULT_OK, intent);
                finish();

                break;
            case R.id.Ibtn_add:
                if(Air_condition_Temp > 31){
                    Air_condition_Temp = Air_condition_Temp + 0;
                }else {
                    Air_condition_Temp++;
                }
                udatdaTemp();
                break;
            case R.id.Ibtn_sub:
                if(Air_condition_Temp < 17){
                    Air_condition_Temp = Air_condition_Temp + 0;
                }else {
                    Air_condition_Temp--;
                }
                udatdaTemp();
                break;
        }

        if(Air_condition_status) {
            Air_Condition_Send();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        ck_power.setEnabled(true);//测试按钮设置可用
        switch (checkedId){
            case R.id.rb_cold:
                tv_air_model.setText("制冷模式");
                Air_condition_model = 1;
                break;
            case R.id.rb_hot:
                tv_air_model.setText("制热模式");
                Air_condition_model = 2;
                break;
            case R.id.rb_dehum:
                tv_air_model.setText("除湿模式");
                Air_condition_model = 3;
                break;
        }

        if(Air_condition_status) {
            Air_Condition_Send();
        }
    }



    private void Air_Condition_Send() {
        if (!Data_Source_Mcu) {
            String status = String.format("%s", Air_condition_status ? "ON" : "OFF");
            ;
            String payload = "{\"Device\":\"Air_conditon\",\"Status\":\"" + status + "\",\"Temperture\":\"" + Air_condition_Temp + "\",\"Model\":\"" + Air_condition_model + "\"}";
            publishmessageplus(mqtt_pub_topic, payload);
        }
        Data_Source_Mcu = false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            if (isChecked) {
                Air_condition_status = true;

            } else {
                Air_condition_status = false;
            }
            Air_Condition_Send();
    }

}