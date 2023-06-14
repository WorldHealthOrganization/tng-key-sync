package int_.who.tng.dataimport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
@EnableConfigurationProperties
@EnableFeignClients
public class TngDataImportApplication {

    public static void main(String[] args) {
        SpringApplication.run(TngDataImportApplication.class, args);
    }
}
