import com.fazecast.jSerialComm.*;
import com.github.sarxos.webcam.Webcam;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_SEMI_BLOCKING;

public class SerialComm {
    static OkHttpClient client = new OkHttpClient();
    private static boolean validResponse = false;
    private static SerialPort port;
    private static final String SERVER = "http://192.168.0.11/request_handler.php";
    private static final String IMG_DIR = "images/";
    private static Webcam webcam = Webcam.getDefault();

    static void initialise() {
        port = SerialPort.getCommPorts()[0];
        System.out.println("Listening for data on serial port " + port.getSystemPortName());

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
                .url(SERVER)
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
                    if(!response.isSuccessful()) throw new IOException("Unexpected code" + response);

                    String serverResponse = responseBody.string();
                    System.out.println("Server response: " + serverResponse + "\n");

                    validResponse = serverResponse.equals("OK");

                    if(serverResponse.equals("ERROR")) {
                        uploadFile();
                    }
                }
            }
        });
    }

    public static void uploadFile() throws IOException {
        File file = new File(IMG_DIR + takePicture());
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(MediaType.parse("image/png"), file))
                .addFormDataPart("other_field", "other_field_value")
                .build();
        Request request = new Request.Builder().url(SERVER).post(formBody).build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

        System.out.println(response.body().string());
    }

    private static String takePicture() throws IOException {
        webcam.setViewSize(new Dimension(640, 480));
        webcam.open();

        // get image
        BufferedImage image = webcam.getImage();

        // save image to PNG file
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        String name = timeStamp + ".png";
        ImageIO.write(image, "PNG", new File(IMG_DIR + name));

        webcam.close();
        return name;
    }


    static boolean isValidResponse(){
        boolean ret = validResponse;
        if(validResponse) {
            validResponse = false;
        }
        return ret;
    }

    static void shutDown() {
        port.closePort();
    }
}
