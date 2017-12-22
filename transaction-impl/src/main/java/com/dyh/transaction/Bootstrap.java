package com.dyh.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.CountDownLatch;

@EnableScheduling
@SpringBootApplication
@ComponentScan(basePackages = {"com.dyh.transaction"})
@ImportResource({"classpath:dubbo/dubbo-service.xml"})
public class Bootstrap {

    private static CountDownLatch countDownLatch = new CountDownLatch(1);

    private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args) throws Exception {
        ApplicationContext ctx = new SpringApplicationBuilder().sources(Bootstrap.class).web(false).run(args);

        log.info("com.dyh.transaction 项目启动成功！");
        countDownLatch.await();
      }


}