package com.ProjetoSD;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import routes.MainPage;
import routes.MenuPage;

@SpringBootApplication
public class ProjetoSdApplication {

	@Bean
	public ServletRegistrationBean<MenuPage> MenuPage() {
		ServletRegistrationBean<MainPage> bean = new ServletRegistrationBean<>(new MenuPage(), "/hello");
		bean.setLoadOnStartup(1);
		return bean;
	}


	@Bean
	public ServletRegistrationBean<MainPage> MainPage() {
		ServletRegistrationBean<MainPage> bean = new ServletRegistrationBean<>(new MainPage(), "/");
		bean.setLoadOnStartup(1);
		return bean;
	}

	public static void main(String[] args) {
		SpringApplication.run(ProjetoSdApplication.class, args);
	}

}
