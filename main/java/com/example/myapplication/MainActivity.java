package com.example.myapplication;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

public class MainActivity extends AppCompatActivity {

    // initialize socket and input output
    public Socket               socket;
    public BufferedReader input   = null;
    public PrintWriter out     = null;
    public BufferedWriter nameOutter = null;
    public String user1;
    private PublicKey publicKey;

    public String currentMessage = "No Messages Yet";
    private PrivateKey privateKey;
    private PublicKey serverPubKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void connectButton(View view) {

        //Client Initiation
        TextView ip = (TextView) findViewById(R.id.ip);
        String ipAddress = ip.getText().toString();
        EditText port = (EditText) findViewById(R.id.port);
        int portAddress = Integer.parseInt(port.getText().toString());
        EditText user = (EditText) findViewById(R.id.username);
        String username = user.getText().toString();
        //Need to Catch the error if entered incorrectly

        user1 = username;
        Thread starter = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    socket = new Socket(ipAddress, portAddress);
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    nameOutter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    out = new PrintWriter(socket.getOutputStream());
                }catch(Exception e){e.printStackTrace();}
                    // Initialise RSA
                    try{
                        KeyPairGenerator RSAKeyGen = KeyPairGenerator.getInstance("RSA");
                        RSAKeyGen.initialize(2048);
                        KeyPair pair = RSAKeyGen.generateKeyPair();
                        publicKey = pair.getPublic();
                        privateKey = pair.getPrivate();
                    } catch (GeneralSecurityException e) {
                        System.out.println(e.getLocalizedMessage() + "\n");
                        System.out.println("Error initialising encryption. Exiting.\n");
                        System.exit(1);
                    }

                    //Sending Username to Server
                try {
                    nameOutter.write(user1);
                    nameOutter.newLine();
                    nameOutter.flush();
                }catch(Exception i){i.printStackTrace();}
                    //SEnding PublicKey to Server
                    try{
                        ByteBuffer bb = ByteBuffer.allocate(4);
                        bb.putInt(publicKey.getEncoded().length);
                        socket.getOutputStream().write(bb.array());
                        socket.getOutputStream().write(publicKey.getEncoded());
                        socket.getOutputStream().flush();
                    }catch(IOException e){
                        System.out.println("IO Problem");
                    }


                    //Read-In the Server Public Key
                    try{
                        byte[] l = new byte[4];
                        socket.getInputStream().read(l,0,4);
                        ByteBuffer bb = ByteBuffer.wrap(l);
                        int len = bb.getInt();
                        byte[] pubKeyByte = new byte[len];
                        socket.getInputStream().read(pubKeyByte);

                        System.out.println("Read in Server Key Data");
                        X509EncodedKeySpec kys = new X509EncodedKeySpec(pubKeyByte);
                        KeyFactory kfs = KeyFactory.getInstance("RSA");
                        serverPubKey = kfs.generatePublic(kys);
                        System.out.println("Made server key");


                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    listener();

                    System.out.println("Worked");


            }
        });
        starter.start();

        setContentView(R.layout.caht_roomv2);
        Button button = findViewById(R.id.button_gchat_send);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("In Onclick");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{

                            System.out.println("In try");
                            TextInputLayout st = findViewById(R.id.username2);
                            EditText s = st.getEditText();
                            String message=s.getText().toString();
                            System.out.println(message);
                            Base64.Encoder base64 = Base64.getEncoder();
                            String encryptedString = base64.encodeToString(encrypter(user1 + ": "+message));
                            PrintWriter out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(socket.getOutputStream())),
                                    true);
                           out.println(encryptedString);
                        }catch(Exception e){e.printStackTrace();}
                    }
                }).start();
            }
        });
           /* */
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void listener()
    {

        try{
            String temp = input.readLine();
//Need to append chat to display
            TextView recycler = findViewById(R.id.editText);
            recycler.append("\n"+decrypter(temp)+"\n");
            while(socket.isConnected()){
                //ChatDisplay.setText("In the loop in listener.");
                recycler.append(decrypter(temp)+"\n");
                String temp2 = input.readLine();
                if(temp!=temp2){
                    //ChatDisplay.append(decrypter(temp2)+"\n");
                    temp = temp2;
                }
            }
        }catch(Exception e){}

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public String decrypter(String message) throws Exception
    {
        Base64.Decoder decoder = Base64.getDecoder();
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE,privateKey);
        try{
            return new String(cipher.doFinal(decoder.decode(message)));
        }catch(Exception e){return "Message Failed to Decrypt";}

    }

    public byte[] encrypter(String message)
    {
        try{
            Cipher encrypter = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            encrypter.init(Cipher.ENCRYPT_MODE,serverPubKey);
            byte[] secretMessageBytes = message.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedMessageBytes = encrypter.doFinal(secretMessageBytes);
            System.out.println("enc");
            return encryptedMessageBytes;
        }catch(Exception e){e.printStackTrace();}
        return new byte[0];
    }

    public void closer(Socket s, BufferedReader br, BufferedWriter out)
    {
        try
        {
            s.close();
            br.close();
            out.close();
        }catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}