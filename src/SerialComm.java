import com.fazecast.jSerialComm.*;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_SEMI_BLOCKING;

public class SerialComm {
    static OkHttpClient client;
    static boolean validresponse = false;
    static void updateServer(){
        System.out.println("updating server...");
        client = new OkHttpClient();

        SerialPort port = SerialPort.getCommPorts()[0];
        port.openPort();
        port.setComPortTimeouts(TIMEOUT_READ_SEMI_BLOCKING,0,0);
        port.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent serialPortEvent) {
                if(serialPortEvent.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE){
                    return;
                }
                byte[] newData = new byte[port.bytesAvailable()];
                String id = new String(newData);
                System.out.println(id);
                if(id.isEmpty()){
                    System.out.println("error");
                }
                else{
                    JSONObject postBody = new JSONObject();
                    try{
                        postBody.put("action", "validate");
                        postBody.put("id", id);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    sendRequest(postBody);
                }
            }
        });
    }

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    static void sendRequest(JSONObject jsonObject) {
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

                    Headers responseHeaders = response.headers();
                    for(int i = 0; i<responseHeaders.size(); i++){
                        System.out.println(responseHeaders.name(i) + ": "+ responseHeaders.value(i));
                    }

                    if(responseBody.string().equals("OK")){
                        validresponse = true;
                    }
                    System.out.println(responseBody.string());
                }
            }
        });


    }

    static boolean isValidResponse(){
        return validresponse;
    }
}
