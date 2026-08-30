package com.pulseim.im;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ImGatewayApplication {
    public static void main(String[] args) { SpringApplication.run(ImGatewayApplication.class, args); }

    @Bean
    ObjectMapper protocolObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
