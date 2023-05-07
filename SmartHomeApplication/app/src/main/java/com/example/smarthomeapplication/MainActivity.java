package com.example.smarthomeapplication;


import android.annotation.SuppressLint;
import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.services.weather.WeatherSearch;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import android.util.Log;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.content.pm.PackageManager;




public class MainActivity extends AppCompatActivity implements AMapLocationListener, CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private static final int REQUEST_CODE = 0;
    private Device_Switch_Control  device_switch_control;
    private Sky_Con sky_con;

    private ScheduledExecutorService scheduler;
    private MqttClient client;
    private Handler handler;
    private TextView m_mqtt;

    private  String host = "tcp://82.157.54.211:1883";     // TCP协议
    private  String userName = "AIXIN698";
    private  String passWord = "AIXIN698";
    private  String mqtt_id = "1353023461_mqtt";
    private  String mqtt_sub_topic = "/mvzzzsmarthomedemo/pub";
    private  String mqtt_pub_topic = "/mzzzsmarthomedemo/sub";


    private boolean isSubscribed = false;
    boolean isConnected = false;

    private TextView tv_temp_value;
    private TextView tv_hum_value;
    private TextView tv_CO2_value;
    private TextView tv_illu_value;
    private TextView tv_tvoc_value;
    private TextView tv_pm2_5_value;//接受的数据
    private CheckBox ck_light;
    private CheckBox ck_fan;
    private CheckBox ck_humidifier;
    private boolean data_source_mcn = false;

//    String air_temp_value = Integer.toString(AirCondictionActivity.Air_conditon_Temp);
//    int air_model_value = AirCondictionActivity.Air_conditon_model;
//    boolean air_conditon_status = AirCondictionActivity.Air_conditon_status;

    private TextView tv_main_air_temp;
    private TextView tv_main_air_model;
    private ImageView main_status_airConditon;
    private int air_condition_power; //单片机发来的空调状态 1 ：ON 0 :OFF
    private int air_condition_temp;
    private int air_condition_model;
    private Boolean air_conditon_status;// App Activity中空调状态 ture false
    private int air_temp_value;
    private int air_model_value;


//位置信息用
    private static final int MY_PERMISSIONS_REQUEST_CALL_LOCATION = 1;
    public AMapLocationClient mlocationClient;
    public AMapLocationClientOption mLocationOption = null;
    public TextView location;
    private WeatherSearch mWeatherSearch;
    private TextView tv_weather;
    private static final String TAG = "MainActivity";
    private TextView tv_net_location;
    private TextView tv_net_air;
    private TextView tv_net_description;
    private TextView tv_net_pm25;
    private TextView tv_net_weather;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //合规检查
        AMapLocationClient.updatePrivacyShow(this,true,true);
        AMapLocationClient.updatePrivacyAgree(this,true);
        //检查版本是否大于M

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_CALL_LOCATION);
            } else {
                //"权限已申请";
                showLocation();
            }
        }

        device_switch_control = new Device_Switch_Control();
        sky_con = new Sky_Con();

        CardView cardv_aircondiction = findViewById(R.id.cardv_aircondiction);
        m_mqtt = findViewById(R.id.m_mqtt);
        tv_hum_value = findViewById(R.id.tv_hum_value);
        tv_temp_value = findViewById(R.id.tv_temp_value);
        tv_CO2_value = findViewById(R.id.tv_CO2_value);
        tv_illu_value = findViewById(R.id.tv_illu_value);
        tv_tvoc_value = findViewById(R.id.tv_TVOC_value);
        tv_pm2_5_value = findViewById(R.id.tv_PM2_5_value);
        tv_main_air_temp = findViewById(R.id.tv_main_air_temp);
        tv_main_air_model = findViewById(R.id.tv_main_air_model);

        tv_net_location = findViewById(R.id.tv_net_location);
        tv_net_air = findViewById(R.id.tv_net_air);
        tv_net_description = findViewById(R.id.tv_net_description);
        tv_net_pm25 = findViewById(R.id.tv_net_pm25);
        tv_net_weather = findViewById(R.id.tv_net_weather);

        main_status_airConditon = findViewById(R.id.main_status_AirConditon);

        ck_light = findViewById(R.id.ck_light);
        ck_fan = findViewById(R.id.ck_fan);
        ck_humidifier = findViewById(R.id.ck_Humidifier);

        ck_humidifier.setOnCheckedChangeListener(this);
        ck_light.setOnCheckedChangeListener(this);
        ck_fan.setOnCheckedChangeListener(this);

        cardv_aircondiction.setOnClickListener(this);



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
                        System.out.println(msg.obj.toString());   // 显示MQTT数据
                        parseJsonobj(msg.obj.toString());
                        if (isSubscribed) {   // 只有已经订阅才弹出Toast
                            Toast.makeText(MainActivity.this, "收到消息", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 30:  //连接失败
                        m_mqtt.setText("连接失败");
                        Toast.makeText(MainActivity.this,"连接失败" ,Toast.LENGTH_SHORT).show();
                        break;
                    case 31:   //连接成功
                        if (!isConnected) {
                            isConnected = true;
                            m_mqtt.setText("连接成功");
                            Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
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
        /* -------------------------------------------------------------------------------------- */

    }
//天气获取
    private void searchCity() {
        //使用Get异步请求
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                //拼接访问地址
                .url("https://api.caiyunapp.com/v2.6/44WvwzbXJKyQlFfS/104.698,31.5378/realtime")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){//回调的方法执行在子线程。
                    String shydata_body = response.body().string();
                    int shydata_code = response.code();
                    Log.d("mvzapplicationtest","获取数据成功了");
                    Log.d("mvzapplicationtest","response.code()=="+shydata_code);
                    Log.d("mvzapplicationtest","response.body().string()=="+shydata_body);


                    try {
                        JSONObject jsonObject = new JSONObject(shydata_body);
                        String timezone = jsonObject.getString("timezone");
                        Log.d("mvzapplicationtest","timezone=="+timezone);

                        JSONObject resultObject = jsonObject.getJSONObject("result");
                        JSONObject realtimeObject = resultObject.getJSONObject("realtime");
                        String temperature = realtimeObject.getString("temperature");
                        Log.d("mvzapplicationtest","temperature=="+temperature);
                        String skycondata = realtimeObject.getString("skycon");
                        Log.d("mvzapplicationtest","skycondata=="+skycondata);
                        String skyconCH = sky_con.SkyCon_Switch(skycondata);
                        tv_net_weather.setText(skyconCH);

                        JSONObject airObject = realtimeObject.getJSONObject("air_quality");
                        String pm25data = airObject.getString("pm25");
                        Log.d("mvzapplicationtest","pm25data=="+pm25data);
                        tv_net_pm25.setText(pm25data);

                        JSONObject descriptionObject = airObject.getJSONObject("description");
                        String chn_description = descriptionObject.getString("chn");
                        Log.d("mvzapplicationtest","chn_description=="+chn_description);
                        tv_net_description.setText("空气质量-"+chn_description);

                    }catch (JSONException e) {
                        e.printStackTrace();

                    }

                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_CALL_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //"权限已申请"
                showLocation();
            } else {
                Toast.makeText(getBaseContext(),"权限已拒绝,不能定位",Toast.LENGTH_SHORT).show();

            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    // TODO:
    private void showLocation() {
        try {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            mlocationClient.setLocationListener(this);
            //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationOption.setInterval(5000);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            //启动定位
            mlocationClient.startLocation();
        } catch (Exception e) {

        }
    }
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        try {
            if (amapLocation != null) {
                StringBuffer dz = new StringBuffer();
                if (amapLocation.getErrorCode() == 0) {
                    //定位成功回调信息，设置相关消息
                    Toast.makeText(getBaseContext(),"收到定位",Toast.LENGTH_SHORT).show();
                    //获取当前定位结果来源，如网络定位结果，详见定位类型表

                    String Latitude = String.valueOf(amapLocation.getLatitude());
                    String Longitude = String.valueOf(amapLocation.getLongitude());
                    String City = amapLocation.getCity();
                    String District = amapLocation.getDistrict();



                    dz.append("定位类型:  "+ amapLocation.getLocationType() + "\n");
                    dz.append("获取纬度:  "+ Latitude + "\n");
                    dz.append("获取经度:  "+ Longitude + "\n");
                    dz.append("获取精度信息:  "+ amapLocation.getAccuracy() + "\n");

                    //如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                    dz.append("地址:  "+ amapLocation.getAddress() + "\n");
                    dz.append("国家信息:  "+ amapLocation.getCountry() + "\n");
                    dz.append("省信息:  "+ amapLocation.getProvince() + "\n");
                    dz.append("城市信息:  "+ City + "\n");
                    dz.append("城区信息:  "+ District + "\n");
                    dz.append("街道信息:  "+ amapLocation.getStreet() + "\n");
                    dz.append("街道门牌号信息:  "+ amapLocation.getStreetNum() + "\n");
                    dz.append("城市编码:  "+ amapLocation.getCityCode() + "\n");
                    dz.append("地区编码:  "+ amapLocation.getAdCode() + "\n");
                    dz.append("获取当前定位点的AOI信息:  "+ amapLocation.getAoiName() + "\n");
                    dz.append("获取当前室内定位的建筑物Id:  "+ amapLocation.getBuildingId() + "\n");
                    dz.append("获取当前室内定位的楼层:  "+ amapLocation.getFloor() + "\n");
                    dz.append("获取GPS的当前状态:  "+ amapLocation.getGpsAccuracyStatus() + "\n");
                    //获取定位时间
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = new Date(amapLocation.getTime());
                    dz.append("获取定位时间:  "+  df.format(date)+ "\n");


                    tv_net_location.setText( District + "-" + City + "市" );

                    searchCity();

                    // 停止定位
                    mlocationClient.stopLocation();
                } else {
                    //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                    Log.e("AmapError", "location Error, ErrCode:"
                            + amapLocation.getErrorCode() + ", errInfo:"
                            + amapLocation.getErrorInfo());
                }
            }
            else{
                Toast.makeText(getBaseContext(),"定位失败",Toast.LENGTH_SHORT).show();

            }
        } catch (Exception e) {
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 停止定位
        if (null != mlocationClient) {
            mlocationClient.stopLocation();
        }
    }

    /**
     * 销毁定位
     */
    private void destroyLocation() {
        if (null != mlocationClient) {
            /**
             * 如果AMapLocationClient是在当前Activity实例化的，
             * 在Activity的onDestroy中一定要执行AMapLocationClient的onDestroy
             */
            mlocationClient.onDestroy();
            mlocationClient = null;
        }
    }





    @Override
    protected void onDestroy() {
        destroyLocation();
        super.onDestroy();
    }





    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            air_conditon_status = data.getBooleanExtra("Air_condition_Power", false);
            air_temp_value = data.getIntExtra("Air_condition_Temp", 0);
            air_model_value = data.getIntExtra("Air_condition_Model", 0);

            // 在此处更新UI或执行其他操作

            if(air_conditon_status) {
            main_status_airConditon.setImageResource(R.drawable.switchon);
            }else {
            main_status_airConditon.setImageResource(R.drawable.switchoff);
                }
             tv_main_air_temp.setText(""+air_temp_value+"");
            switch (air_model_value) {
            case 1 : tv_main_air_model.setText("制冷模式");
                break;
            case 2 :tv_main_air_model.setText("制热模式");
                break;
            case 3 :tv_main_air_model.setText("除湿模式");
                break;
            }
        }
    }


    // MQTT初始化
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
    /* ========================================================================================== */
    // Json数据解析
    private void parseJsonobj(String jsonobj){
        // 解析json
        try {
            JSONObject jsonObject = new JSONObject(jsonobj);
            String Humidity = jsonObject.getString("Hum");
            String Tempreature = jsonObject.getString("Temp");
            String Illuminance = jsonObject.getString("Illu");
            String CO2data = jsonObject.getString("CO2");
            String TVOCData = jsonObject.getString("TVOC");
            String PM2_5data = jsonObject.getString("PM2_5");


            air_condition_power = jsonObject.getInt("Air_Power");
            air_condition_temp = jsonObject.getInt("Air_Temp");
            air_condition_model = jsonObject.getInt("Air_Model");
            int Fan_Status = jsonObject.getInt("Fan_S");
            int Humidifier_Status = jsonObject.getInt("Hum_S");
            int Light_Status = jsonObject.getInt("L_S");


            tv_temp_value.setText(""+Tempreature+"");
            tv_hum_value.setText(""+Humidity+"");
            tv_illu_value.setText(""+Illuminance+"");
            tv_CO2_value.setText(""+CO2data+"");
            tv_tvoc_value.setText(""+TVOCData+"");
            tv_pm2_5_value.setText(""+PM2_5data+"");




            if(Light_Status == 1) {
                ck_light.setChecked(true);
            } else {
                ck_light.setChecked(false);
            }

            if(Humidifier_Status == 1) ck_humidifier.setChecked(true);
            else ck_humidifier.setChecked(false);

            if(Fan_Status == 1) ck_fan.setChecked(true);
            else ck_fan.setChecked(false);

            if(air_condition_power == 1) {
                main_status_airConditon.setImageResource(R.drawable.switchon);
                air_conditon_status = true;
            }else {
                main_status_airConditon.setImageResource(R.drawable.switchoff);
                air_conditon_status = false;
            }

            tv_main_air_temp.setText(""+ air_condition_temp +"");
            switch (air_condition_model) {
                case 1 :tv_main_air_model.setText("制冷模式");
                    break;
                case 2 :tv_main_air_model.setText("制热模式");
                    break;
                case 3 :tv_main_air_model.setText("除湿模式");
                    break;

            }


        } catch (JSONException e) {
            e.printStackTrace();

        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.ck_light:
                    publishmessageplus(mqtt_pub_topic, device_switch_control.Light_Switch(isChecked));
                    break;
                case R.id.ck_fan:
                    publishmessageplus(mqtt_pub_topic, device_switch_control.Fan_Switch(isChecked));
                    break;
                case R.id.ck_Humidifier:
                    publishmessageplus(mqtt_pub_topic, device_switch_control.Humidifier_Switch(isChecked));
                    break;
                default:
                    break;

            }
    }




    @Override
    public void onClick(View v) {
//        Intent intent = new Intent();
//        intent.setClass(MainActivity.this,AirCondictionActivity.class);
//
//        startActivity(intent);
        Intent intent = new Intent(MainActivity.this, AirCondictionActivity.class);
        data_source_mcn = true;
        intent.putExtra("air_condition_power",air_conditon_status);
        intent.putExtra("air_condition_temp",air_condition_temp);
        intent.putExtra("air_condition_model",air_condition_model);
        intent.putExtra("data_source",data_source_mcn);
        setResult(RESULT_OK, intent);
        startActivityForResult(intent, REQUEST_CODE);

    }
}