package com.queuectl.cli;

import com.queuectl.service.ConfigService;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "config", description = "Manage system configuration")
public class ConfigCommand {

    @Command(name = "set", description = "Set a configuration value")
    public void set(
            @Parameters(index = "0", description = "Configuration key (e.g., max-retries, backoff-base)") String key,
            @Parameters(index = "1", description = "Configuration value") String value) {
        
        ConfigService configService = new ConfigService();
        configService.setConfig(key, value);
        System.out.printf("Config set: %s = %s%n", key, value);
    }
}