package com.pluxity.modbus.Model1;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.Register;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.NormalDistribution;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;


@Slf4j
public class DummyData {
    public static final int LIMIT_RETRY = 3;
    private String timestamp;
    private String deviceId;
    private SensorData sensors;

    public DummyData() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        timestamp = simpleDateFormat.format(new Date());

        deviceId = UUID.randomUUID().toString();

        sensors = new SensorData();
    }

    private class SensorData {
        private double temperature;
        private double humidity;
        private double pressure;
        private double CO2;
        private double O2;

        public SensorData() {
            temperature = new NormalDistribution(20, 5).sample();
            humidity = new NormalDistribution(50, 10).sample();
            pressure = new NormalDistribution(1000, 100).sample();
            CO2 = new NormalDistribution(1000, 100).sample();
            O2 = new NormalDistribution(1000, 100).sample();
        }
    }

    public void sendDataToServer() throws Exception {
        InetAddress serverIpAddress = InetAddress.getByName("127.0.0.1");
        int retryCount = 0;
        while (retryCount < LIMIT_RETRY) {
            byte[] modbusPacket = createModbusPacket(serverIpAddress);
            byte[] encryptedPacket = encrpyt(modbusPacket);

            //실제로 서버에 보내야 하는데 여기서는 테스트를 위해 로컬로 보냄
            try (Socket socket = new Socket(serverIpAddress, 8080)) {
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(Objects.requireNonNull(encryptedPacket));
                break;
            } catch (IOException e) {
                retryCount++;
                log.error("Error while sending data to server. Retry count: " + retryCount);
                if (retryCount == LIMIT_RETRY) {
                    throw e;
                }
            }
        }
    }

    //암호화를 위한 KEY 생성
    private static final byte[] KEY;
    static {
        KeyGenerator keyGen;
        try {
            keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();
            KEY = secretKey.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new Error("Error while generating key", e);
        }
    }

    private byte[] encrpyt(byte[] modbusPacket) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(KEY, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(modbusPacket);
    }

    private byte[] createModbusPacket(InetAddress serverIpAddress) {
        // modbus connection Setting
        TCPMasterConnection connection = null;
        try {
            connection = new TCPMasterConnection(serverIpAddress);
            connection.setPort(8080);
            connection.connect();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error while creating connection", e);
            return null;
        }

        // modbus request 준비
        ReadMultipleRegistersRequest request = new ReadMultipleRegistersRequest(0, 8);
        // modbus transaction 준비
        ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
        transaction.setRequest(request);

        //transaction 실행
        try {
            transaction.execute();
        } catch (ModbusException e) {
            e.printStackTrace();
            log.error("Error while executing transaction", e);
            return null;
        }

        //response 받고,
        ReadMultipleRegistersResponse response = (ReadMultipleRegistersResponse) transaction.getResponse();

        //register packet 저장
        Register[] registers = response.getRegisters();
        byte[] packet = new byte[registers.length * 2];

        for (int i = 0; i < registers.length; i++) {
            System.arraycopy(registers[i].toBytes(), 0, packet, i * 2, 2);
        }

        connection.close();
        /// 해당 패킹 전달하면 끝
        return packet;
    }


}
