import com.fazecast.jSerialComm.*;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_SEMI_BLOCKING;

public class SerialComm {
    static OkHttpClient client;
    private static boolean validResponse = false;

    static void initialise() {
        client = new OkHttpClient();

        SerialPort port = SerialPort.getCommPorts()[0];
        System.out.println("Listening for data on serial port " + port.toString());

        port.openPort();
        port.setComPortTimeouts(TIMEOUT_READ_SEMI_BLOCKING,0,0);
        port.addDataListener(new SerialPortPacketListener() {
            @Override
            public int getPacketSize() {
                return 8;
            }

            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
            }

            @Override
            public void serialEvent(SerialPortEvent serialPortEvent) {
                byte[] newData = serialPortEvent.getReceivedData();
                System.out.println("Received data of size: " + newData.length);
                char[] message = new char[newData.length];
                for (int i = 0; i < newData.length; ++i)
                    message[i] = (char)newData[i];
                String id = new String(newData);
                System.out.println(id);
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("action", "validate");
                    jsonObject.put("id", id);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                sendRequest(jsonObject);
            }
        });
    }

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static void sendRequest(JSONObject jsonObject) {
        Request request = new Request.Builder()
                .url("http://192.168.0.11/request_handler.php")
                .post(RequestBody.create(JSON, jsonObject.toString()))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try(ResponseBody responseBody = response.body()){
                    if(!response.isSuccessful()) throw new IOException("unexpected code" + response);

                    String serverResponse = responseBody.string();
                    System.out.println(serverResponse);

                    if(serverResponse.equals("OK")) {
                        validResponse = true;
                    } else {
                        validResponse = false;
                    }
                }
            }
        });
    }

    static boolean isValidResponse(){
        boolean ret = validResponse;
        if(validResponse) {
            validResponse = false;
        }
        return ret;
    }
}
