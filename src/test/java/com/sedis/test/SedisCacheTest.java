package com.sedis.test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SedisCacheTest {

    public static void main(String[] args) {
        String configPath = "classpath:spring-context.xml";
        ApplicationContext context = new ClassPathXmlApplicationContext(configPath);
        WaybillService service = (WaybillService) context.getBean("waybillService");

        String code = "14";

        long s3 = System.currentTimeMillis();
        System.out.println("findListById, " + service.findListById(code));
        long e3 = System.currentTimeMillis();
        System.out.println("f3 is " + (e3 - s3));
        long s4 = System.currentTimeMillis();
        System.out.println("findListById, " + service.findListById(code));
        long e4 = System.currentTimeMillis();
        System.out.println("f4 is " + (e4 - s4));

        System.out.println("==================================");

        long s5 = System.currentTimeMillis();
        System.out.println("findMapById, " + service.findMapById(code));
        long e5 = System.currentTimeMillis();
        System.out.println("f5 is " + (e5 - s5));
        long s6 = System.currentTimeMillis();
        System.out.println("findMapById, " + service.findMapById(code));
        long e6 = System.currentTimeMillis();
        System.out.println("f6 is " + (e6 - s6));

        System.out.println("==================================");

        long s1 = System.currentTimeMillis();
        System.out.println("findArrayById, " + service.findArrayById(code));
        long e1 = System.currentTimeMillis();
        System.out.println("f1 is " + (e1 - s1));
        long s2 = System.currentTimeMillis();
        System.out.println("findArrayById, " + service.findArrayById(code));
        long e2 = System.currentTimeMillis();
        System.out.println("f2 is " + (e2 - s2));

        System.out.println("==================================");

        long s7 = System.currentTimeMillis();
        System.out.println("findStringById, " + service.findStringById(code));
        long e7 = System.currentTimeMillis();
        System.out.println("f7 is " + (e7 - s7));
        long s8 = System.currentTimeMillis();
        System.out.println("findStringById, " + service.findStringById(code));
        long e8 = System.currentTimeMillis();
        System.out.println("f8 is " + (e8 - s8));

        System.out.println("==================================");

        long s9 = System.currentTimeMillis();
        System.out.println("findIntegerById, " + service.findIntegerById(code));
        long e9 = System.currentTimeMillis();
        System.out.println("f9 is " + (e9 - s9));
        long s10 = System.currentTimeMillis();
        System.out.println("findIntegerById, " + service.findIntegerById(code));
        long e10 = System.currentTimeMillis();
        System.out.println("f10 is " + (e10 - s10));

        System.out.println("==================================");

    }
}






