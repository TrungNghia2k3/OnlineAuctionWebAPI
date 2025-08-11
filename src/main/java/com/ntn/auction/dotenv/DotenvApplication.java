package com.ntn.auction.dotenv;

import io.github.cdimascio.dotenv.Dotenv;

public class DotenvApplication {
    public static void init() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
    }
}
