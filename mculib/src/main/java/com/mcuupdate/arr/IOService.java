package com.mcuupdate.arr;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;



public class IOService {

    public static boolean readValue(String ioName) throws IOException {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(ioName));
        String prop = reader.readLine();
        boolean value = ("1".equals(prop) ? true : false);
        reader.close();
        return value;
    }

    public static void writeValue(String ioName, boolean flag) throws IOException {
        BufferedWriter bufWriter = new BufferedWriter(new FileWriter(ioName));
        bufWriter.write(flag ? "1" : "0");
        bufWriter.flush();
        bufWriter.close();
    }


}
