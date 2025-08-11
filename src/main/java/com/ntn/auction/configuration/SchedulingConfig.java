package com.ntn.auction.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2); // Số lượng thread trong pool
        scheduler.setThreadNamePrefix("auction-scheduler-"); // Tiền tố cho tên thread
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // Chính sách xử lý khi không còn thread nào sẵn sàng
        // CallerRunsPolicy sẽ chạy task trong thread gọi nó nếu không còn thread nào sẵn sàng, thay vì từ chối task mới.
        // Điều này giúp tránh mất task và đảm bảo rằng các task sẽ được thực thi,
        // nhưng có thể làm chậm quá trình nếu quá nhiều task được gửi cùng lúc.
        // Các chính sách khác có thể là:
        // - AbortPolicy: Ném exception khi không còn thread nào sẵn sàng.
        // - DiscardPolicy: Bỏ qua task mới mà không thông báo.
        // - DiscardOldestPolicy: Bỏ task cũ nhất trong hàng đợi
        //   và thêm task mới vào hàng đợi.
        //   Điều này có thể hữu ích nếu bạn muốn ưu tiên các task mới hơn.
        //   Tuy nhiên, nó có thể dẫn đến mất task nếu hàng đợi đầy.
        //   Nên cân nhắc kỹ trước khi sử dụng.
        // - Sử dụng CallerRunsPolicy để đảm bảo rằng các task sẽ được thực thi
        //   mà không bị từ chối, nhưng có thể làm chậm quá trình nếu quá nhiều task được gửi cùng lúc.
        scheduler.initialize();
        return scheduler;
    }
}
