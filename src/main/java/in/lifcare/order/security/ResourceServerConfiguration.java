package in.lifcare.order.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.web.util.matcher.RequestMatcher;

import in.lifcare.auth.gateway.response.utils.APIResponseValidator;
import in.lifcare.auth.gateway.response.utils.ResponseFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableResourceServer
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {
	@Value("${spring.application.name:spring-boot-application}")
	private String resourceId;

	@Value("${apply-mask:true}")
	private boolean applyMask;
	
    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        // @formatter:off
        resources.resourceId(resourceId);
        //resources.tokenServices(customUserInfoTokenService);
        // @formatter:on
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
    	// @formatter:off
    	http.requestMatcher(new OAuthRequestedMatcher())
            .authorizeRequests()
            	.antMatchers(HttpMethod.OPTIONS).permitAll()
                .anyRequest().authenticated();
    	if (applyMask){
        	http.addFilterAfter(new ResponseFilter(appContext, apiResponseValidator), BasicAuthenticationFilter.class);
    	}
    	// @formatter:on
    }
    
   /* @Override
    public void configure1(HttpSecurity http) throws Exception {
        // @formatter:off
        	http.requestMatcher(new OAuthRequestedMatcher())
        		.antMatcher("/auth/otp").authorizeRequests().antMatchers(HttpMethod.POST, "/**").permitAll().and()
                .authorizeRequests()
                	.antMatchers(HttpMethod.OPTIONS).permitAll()
                    .anyRequest().authenticated();
        // @formatter:on
    }*/
    
    private static class OAuthRequestedMatcher implements RequestMatcher {
        public boolean matches(HttpServletRequest request) {
            String auth = request.getHeader("Authorization");
            // Determine if the client request contained an OAuth Authorization
            boolean haveOauth2Token = (auth != null) && auth.startsWith("Bearer");
            boolean haveAccessToken = request.getParameter("access_token")!=null;
			return haveOauth2Token || haveAccessToken;
        }
    }
    
	@Autowired
	private org.springframework.context.ApplicationContext appContext;

	@Autowired
	private APIResponseValidator apiResponseValidator;
}
