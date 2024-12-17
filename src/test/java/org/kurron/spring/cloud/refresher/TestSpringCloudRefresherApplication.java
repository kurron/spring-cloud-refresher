package org.kurron.spring.cloud.refresher;

import org.springframework.boot.SpringApplication;

public class TestSpringCloudRefresherApplication {

    public static void main(String[] args) {
        SpringApplication.from(SpringCloudRefresherApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
