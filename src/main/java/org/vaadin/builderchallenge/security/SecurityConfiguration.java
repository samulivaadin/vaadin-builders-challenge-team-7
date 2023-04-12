package org.vaadin.builderchallenge.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.vaadin.builderchallenge.views.login.LoginView;

import java.util.HashMap;
import java.util.Map;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends VaadinWebSecurity {

	@Bean
	public PasswordEncoder passwordEncoder() {
		Map<String, PasswordEncoder> passwordEncoders = new HashMap<>();
		passwordEncoders.put("bcrypt", new BCryptPasswordEncoder());
		passwordEncoders.put("noop", NoOpPasswordEncoder.getInstance());
		return new DelegatingPasswordEncoder("bcrypt", passwordEncoders);
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		http.authorizeHttpRequests().requestMatchers(new AntPathRequestMatcher("/images/*.png")).permitAll();

		// Icons from the line-awesome addon
		http.authorizeHttpRequests().requestMatchers(new AntPathRequestMatcher("/line-awesome/**/*.svg")).permitAll();
		super.configure(http);
		setLoginView(http, LoginView.class);
	}

}
