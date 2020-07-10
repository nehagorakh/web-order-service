package in.lifcare.order;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter {

	// Should be disabled on production server
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
			.allowedOrigins("*")
			.allowedMethods("PUT", "DELETE","GET","POST","PATCH")
			//.allowedHeaders("header1", "header2", "header3")
			//.exposedHeaders("header1", "header2")
			.allowCredentials(false).maxAge(3600);
	}
	
	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
	    registry.addRedirectViewController("/documentation/v2/api-docs", "/v2/api-docs");
	    registry.addRedirectViewController("/documentation/configuration/ui", "/configuration/ui");
	    registry.addRedirectViewController("/documentation/configuration/security", "/configuration/security");
	    registry.addRedirectViewController("/documentation/swagger-resources", "/swagger-resources");
	    registry.addRedirectViewController("/documentation", "/documentation/swagger-ui.html");
	    registry.addRedirectViewController("/documentation/", "/documentation/swagger-ui.html");
	}
	
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
	    registry.addResourceHandler("/documentation/**")
	      .addResourceLocations("classpath:/META-INF/resources/");
	 
	    registry.addResourceHandler("/webjars/**")
	      .addResourceLocations("classpath:/META-INF/resources/webjars/");
	}

}
