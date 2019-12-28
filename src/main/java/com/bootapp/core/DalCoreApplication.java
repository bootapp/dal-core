package com.bootapp.core;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@EnableAutoConfiguration
@SpringBootApplication
public class DalCoreApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder().sources(DalCoreApplication.class).web(WebApplicationType.NONE).build().run(args);
    }
}
