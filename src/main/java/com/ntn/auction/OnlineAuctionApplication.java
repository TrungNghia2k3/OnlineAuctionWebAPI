package com.ntn.auction;

import com.ntn.auction.dotenv.DotenvApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OnlineAuctionApplication {
    public static void main(String[] args) {
        DotenvApplication.init(); // Load biến môi trường từ .env
        SpringApplication.run(OnlineAuctionApplication.class, args);
    }
}
