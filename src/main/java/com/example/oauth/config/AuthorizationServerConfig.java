package com.example.oauth.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import javax.sql.DataSource;
import java.util.Arrays;

@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {
//"client_credentials"
	static final String CLIEN_ID = "devglan-client";
	static final String CLIENT_SECRET = "devglan-secret";
	static final String GRANT_TYPE_PASSWORD = "password";
	static final String AUTHORIZATION_CODE = "authorization_code";
    static final String REFRESH_TOKEN = "refresh_token";
    static final String IMPLICIT = "implicit";
	static final String SCOPE_READ = "read";
	static final String SCOPE_WRITE = "write";
    static final String TRUST = "trust";
	static final int ACCESS_TOKEN_VALIDITY_SECONDS = 1*60*60;
    static final int FREFRESH_TOKEN_VALIDITY_SECONDS = 6*60*60;
	
	@Autowired
	private TokenStore tokenStore;
	@Qualifier("dataSource")
	@Autowired
	DataSource dataSource;


	@Autowired
	private UserApprovalHandler userApprovalHandler;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private RedisConnectionFactory connectionFactory;

//	@Autowired
//	private TokenStore tokenStore;


	/**
	 * 配置 oauth_client_details【client_id和client_secret等】信息的认证【检查ClientDetails的合法性】服务
	 * 设置 认证信息的来源：数据库 (可选项：数据库和内存,使用内存一般用来作测试)
	 * 自动注入：ClientDetailsService的实现类 JdbcClientDetailsService (检查 ClientDetails 对象)
	 */
//	@Override
//	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
//		clients.jdbc(dataSource);
//	}




//
//	@Bean
//	public RedisTokenStore tokenStore() {
//		return new RedisTokenStore(connectionFactory);
//	}


	@Override
	public void configure(ClientDetailsServiceConfigurer configurer) throws Exception {
//		ConsumerTokenServices
		configurer
				.inMemory()
				.withClient(CLIEN_ID)
//				.autoApprove(true)
				.secret(CLIENT_SECRET)
				.authorizedGrantTypes(GRANT_TYPE_PASSWORD, AUTHORIZATION_CODE, REFRESH_TOKEN, IMPLICIT )
				.scopes(SCOPE_READ, SCOPE_WRITE, TRUST)
//				.authorities()  Authorities that are granted to the client (regular Spring Security authorities).
				.accessTokenValiditySeconds(ACCESS_TOKEN_VALIDITY_SECONDS).
				refreshTokenValiditySeconds(FREFRESH_TOKEN_VALIDITY_SECONDS);
	}

	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
//		endpoints.tokenStore(tokenStore).userApprovalHandler(userApprovalHandler)
////				.userDetailsService()
//				.authenticationManager(authenticationManager);
		TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
		tokenEnhancerChain.setTokenEnhancers(
				Arrays.asList(tokenEnhancer(), jwtAccessTokenConverter()));

		endpoints.tokenStore(tokenStore)
				.tokenEnhancer(tokenEnhancerChain)
				.authenticationManager(authenticationManager);


	}


	@Bean
	public TokenStore tokenStore() {
		return new JwtTokenStore(jwtAccessTokenConverter());
	}


	@Bean
	//使用私钥编码 JWT 中的  OAuth2 令牌
	public JwtAccessTokenConverter jwtAccessTokenConverter() {
		final JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
		KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(new ClassPathResource("myssl.jks"), "mypassstore".toCharArray());
		converter.setKeyPair(keyStoreKeyFactory.getKeyPair("myssl","mypasskey".toCharArray()));
		return converter;
	}

	/**
	 *  配置：安全检查流程
	 *  默认过滤器：BasicAuthenticationFilter
	 *  1、oauth_client_details表中clientSecret字段加密【ClientDetails属性secret】
	 *  2、CheckEndpoint类的接口 oauth/check_token 无需经过过滤器过滤，默认值：denyAll()
	 */

	@Override
	public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
		//允许表单认证
//		oauthServer.allowFormAuthenticationForClients().tokenKeyAccess("permitAll()")
//		.checkTokenAccess("isAuthenticated()");

		oauthServer.allowFormAuthenticationForClients().tokenKeyAccess("isAnonymous() || hasAuthority('ROLE_TRUSTED_CLIENT')").checkTokenAccess(
				"hasAuthority('ROLE_TRUSTED_CLIENT')");
//		tokenKeyAccess 获取jwt public key
//		oauthServer.tokenKeyAccess("permitAll()").checkTokenAccess(
//				"isAuthenticated()");
//		oauthServer.passwordEncoder(new BCryptPasswordEncoder());//设置oauth_client_details中的密码编码器
//		oauthServer.checkTokenAccess("permitAll()");//对于CheckEndpoint控制器[框架自带的校验]的/oauth/check端点允许所有客户端发送器请求而不会被Spring-security拦截
	}



	@Bean
	public TokenEnhancer tokenEnhancer() {
		return new CustomTokenEnhancer();
	}



//	@Override
//	public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
//		oauthServer.tokenKeyAccess("isAnonymous() || hasAuthority('ROLE_TRUSTED_CLIENT')").checkTokenAccess(
//				"hasAuthority('ROLE_TRUSTED_CLIENT')");
//	}




//
//	Grant Type包括：
//
//	authorization_code：传统的授权码模式
//	implicit：隐式授权模式
//	password：资源所有者（即用户）密码模式
//	client_credentials：客户端凭据（客户端ID以及Key）模式
//	refresh_token：获取access token时附带的用于刷新新的token模式


}