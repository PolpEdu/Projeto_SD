package com.projetoSD.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProjetoSdApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjetoSdApplication.class, args);
	}


	@GetMapping("/hello")
	public String sayHello(@RequestParam(value = "myName", defaultValue = "World") String name) {
		return String.format("Hello %s!", name);
	}
}
