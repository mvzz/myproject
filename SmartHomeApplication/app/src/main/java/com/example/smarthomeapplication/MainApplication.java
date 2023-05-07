package com.example.smarthomeapplication;

import android.app.Application;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainApplication extends Application {


    private MqttAndroidClient mqttClient;
    private String brokerUri = "tcp://your-broker-url:1883";
    private String clientId = "your-client-id";
    private String username = "your-username";
    private String password = "your-password";

    @Override
    public void onCreate() {
        super.onCreate();
        // 建立连接
        mqttClient = new MqttAndroidClient(this, brokerUri, clientId);
        mqttClient.setCallback(new MqttCallback() {
            // 连接成功回调
            @Override
            public void connectionLost(Throwable cause) {
                Log.d("MQTT", "Connection lost.");
            }
            // 连接断开回调
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d("MQTT", "Message arrived.");
            }
            // 消息到达回调
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("MQTT", "Delivery complete.");
            }
        });
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        try {
            mqttClient.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // 公共方法，用于在 Activity 中订阅消息
    public void subscribeToTopic(String topic) {
        int qos = 1;
        try {
            mqttClient.subscribe(topic, qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // 公共方法，用于在 Activity 中发布消息
    public void publishMessage(String topic, String message) {
        int qos = 1;
        boolean retained = false;
        try {
            mqttClient.publish(topic, message.getBytes(), qos, retained);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}

