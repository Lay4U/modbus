package com.pluxity.modbus;

import com.pluxity.modbus.Model1.DummyData;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@RequiredArgsConstructor
public class ModbusApplication {

    private final DummyData dummyData;

    public static void main(String[] args) {
        SpringApplication.run(ModbusApplication.class, args);
    }

    @PostConstruct
    public void startServerAndClient(){
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try{
                InetAddress localHost = InetAddress.getLocalHost();
                dummyData.sendDataToServer();
            } catch (UnknownHostException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

}
