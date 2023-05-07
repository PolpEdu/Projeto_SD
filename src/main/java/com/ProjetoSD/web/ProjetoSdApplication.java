package com.ProjetoSD.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ProjetoSdApplication {

	/*@Bean
	public ServletRegistrationBean<ThymeleafServlet> MenuPage() {
		ServletRegistrationBean<ThymeleafServlet> bean = new ServletRegistrationBean<>(new ThymeleafServlet(), "/");
		bean.setLoadOnStartup(1);
		return bean;
	}*/


	/*@Bean
	public ServletRegistrationBean<MainPage> MainPage() {
		ServletRegistrationBean<MainPage> bean = new ServletRegistrationBean<>(new MainPage(), "/");
		bean.setLoadOnStartup(1);
		return bean;
	}*/

	public static void main(String[] args) {
		SpringApplication.run(ProjetoSdApplication.class, args);
	}

}
