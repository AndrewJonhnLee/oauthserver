package com.example.oauth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;

@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

	private static final String RESOURCE_ID = "resource_id";
	
	@Override
	public void configure(ResourceServerSecurityConfigurer resources) {
		resources.resourceId(RESOURCE_ID).stateless(false);
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {
        http.
                anonymous().disable()
                .authorizeRequests()
                .antMatchers("/users/**")
//				.authenticated()
				.access("hasRole('ADMIN')")
                .and().csrf().disable().exceptionHandling().accessDeniedHandler(new OAuth2AccessDeniedHandler());
	}

//
//	    http
//				// Just for laughs, apply OAuth protection to only 2 resources
//				.requestMatchers().antMatchers("/","/admin/beans").and()
//           .authorizeRequests()
//           .anyRequest().access("#oauth2.hasScope('read')"); //[4]
//	// @formatter:on



}