package org.qts;

import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties;

public class Main {
    public static void main(String[] args) {
        System.out.println(System.getProperty("java.library.path"));
        System.out.println(System.getenv().get("LD_LIBRARY_PATH"));
        System.loadLibrary("thostmduserapi_se");
        System.loadLibrary("thostmduserapi_wrap");
        System.out.println("Hello world!");
    }
}