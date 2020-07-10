package in.lifcare.order;



import java.util.Collections;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import com.netflix.config.ConfigurationManager;

import in.lifcare.auth.gateway.utils.LifcareAgentInterceptor;
import in.lifcare.core.exception.RestTemplateErrorHandler;
/**
 * 
 * @author Amit Kumar
 * @date 10-April-2017
 * @since 0.1.0
 */
@Configuration	
@EnableAsync
@EnableCircuitBreaker
@SpringBootApplication
@EnableAutoConfiguration
@EnableJpaAuditing
@EnableJpaRepositories
@ComponentScan(basePackages = { "in.lifcare.core", "in.lifcare.order", "in.lifcare.producer","in.lifcare.auth.gateway", "in.lifcare.order.audit",  "in.lifcare.client", "in.lifcare.account.client"})
public class OrderApp {
	public static void main(String[] args) {
		SpringApplication.run(OrderApp.class, args);
		ConfigurationManager.getConfigInstance().setProperty("hystrix.command.HystrixCommandKey.execution.isolation.thread.timeoutInMilliseconds", 1000);
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {

		RestTemplate template = builder.build();
		RestTemplateErrorHandler errorHandler = new RestTemplateErrorHandler();
		template.setErrorHandler(errorHandler);
		template.setInterceptors(Collections.singletonList(new LifcareAgentInterceptor()));
		return template;
	}
}
