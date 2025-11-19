package sn.dev.product_service.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
        basePackages = "sn.dev.product_service.data.repo",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "sn.dev.product_service.data.repo.elastic\\..*"
        )
)
public class MongoConfig {
}