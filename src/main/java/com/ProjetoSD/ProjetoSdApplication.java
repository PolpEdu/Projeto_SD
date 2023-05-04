package com.ProjetoSD;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import routes.Example;

@SpringBootApplication
public class ProjetoSdApplication {

	@Bean
	public ServletRegistrationBean<Example> exampleServletBean() {
		ServletRegistrationBean<Example> bean = new ServletRegistrationBean<>(new Example(), "/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	public static void main(String[] args) {
		SpringApplication.run(ProjetoSdApplication.class, args);
	}

}
