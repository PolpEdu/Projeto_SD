package com.ProjetoSD.web;

import com.ProjetoSD.interfaces.RMIServerInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

@Configuration
public class RMIServerConfig {

    @Value("${rmi.host}")
    private String rmiHost;

    @Value("${rmi.port}")
    private int rmiPort;

    @Value("${rmi.registry.name}")
    private String rmiRegistryName;

    @Bean
    public RMIServerInterface rmiServerInterface() throws Exception {
        Registry registry = LocateRegistry.getRegistry(rmiHost, rmiPort);
        return (RMIServerInterface) registry.lookup(rmiRegistryName);
    }


}
