package com.example.basicandroidmqttclient;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.basicandroidmqttclient.MESSAGE";
    public static final String brokerURI = "3.225.116.68";

    Activity thisActivity;
    ListView listViewSubMsg;
    List<String> messageList;
    ArrayAdapter<String> adapter;

    Spinner spinnerTopic;
    EditText editTextStart;
    EditText editTextEnd;
    CheckBox checkBoxOn;
    CheckBox checkBoxOff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        thisActivity = this;
        listViewSubMsg = findViewById(R.id.listViewSubMsg);

        messageList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messageList);
        listViewSubMsg.setAdapter(adapter);

        spinnerTopic = findViewById(R.id.spinnerTopic);
        editTextStart = findViewById(R.id.editTextStart);
        editTextEnd = findViewById(R.id.editTextEnd);
        checkBoxOn = findViewById(R.id.checkBoxOn);
        checkBoxOff = findViewById(R.id.checkBoxOff);

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this, R.array.topic_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTopic.setAdapter(spinnerAdapter);

        spinnerTopic.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedTopic = (String) parentView.getItemAtPosition(position);

                if ("air_monitor".equals(selectedTopic)) {
                    editTextStart.setVisibility(View.VISIBLE);
                    editTextEnd.setVisibility(View.VISIBLE);
                    checkBoxOn.setVisibility(View.GONE);
                    checkBoxOff.setVisibility(View.GONE);
                } else if ("light_monitor".equals(selectedTopic)) {
                    editTextStart.setVisibility(View.GONE);
                    editTextEnd.setVisibility(View.GONE);
                    checkBoxOn.setVisibility(View.VISIBLE);
                    checkBoxOff.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    public void publishMessage(View view) {
        String selectedTopic = spinnerTopic.getSelectedItem().toString();
        String message;

        if ("air_monitor".equals(selectedTopic)) {
            int start = Integer.parseInt(editTextStart.getText().toString());
            int end = Integer.parseInt(editTextEnd.getText().toString());
            message = String.format("{\"range\": {\"start\": %d, \"end\": %d}}", start, end);
        } else if ("light_monitor".equals(selectedTopic)) {
            boolean turnOn = checkBoxOn.isChecked();
            boolean turnOff = checkBoxOff.isChecked();
            message = String.format("{\"event\": {\"on\": %b, \"off\": %b}}", turnOn, turnOff);
        } else {
            return;
        }

        publishToTopic(selectedTopic, message);
    }

    private void publishToTopic(String topic, String message) {
        Mqtt5BlockingClient client = Mqtt5Client.builder()
                .identifier(UUID.randomUUID().toString())
                .serverHost(brokerURI)
                .buildBlocking();

        client.connect();
        client.publishWith()
                .topic(topic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(message.getBytes(StandardCharsets.UTF_8))
                .send();
        client.disconnect();
    }

    public void sendSubscription(View view) {
        EditText topicName = findViewById(R.id.editTextTopicNameSub);

        Mqtt5BlockingClient client = Mqtt5Client.builder()
                .identifier(UUID.randomUUID().toString())
                .serverHost(brokerURI)
                .buildBlocking();

        client.connect();

        client.toAsync().subscribeWith()
                .topicFilter(topicName.getText().toString())
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(msg -> {
                    String topic = msg.getTopic().toString();
                    String message = new String(msg.getPayloadAsBytes(), StandardCharsets.UTF_8);

                    String formattedMessage = topic + ": " + message;

                    thisActivity.runOnUiThread(() -> {
                        messageList.add(0, formattedMessage);
                        adapter.notifyDataSetChanged();
                    });
                })
                .send();
    }

    public void clearList(View view) {
        messageList.clear();
        adapter.notifyDataSetChanged();
    }
}
