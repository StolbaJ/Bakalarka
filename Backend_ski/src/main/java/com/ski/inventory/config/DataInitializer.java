package com.ski.inventory.config;

import com.ski.inventory.repository.UserRepository;
import com.ski.inventory.service.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * Inicializuje výchozí účty admin/technician.
 * Dev: jednoduchá hesla (admin123, tech123), FE může zobrazit nápovědu.
 * Prod: silné 12znakové heslo, zaloguje se jen jednou při startu, na FE se nápověda nezobrazuje.
 */
@Configuration
public class DataInitializer {

    private static final int PROD_PASSWORD_LENGTH = 12;
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(
            UserRepository userRepository,
            PasswordService passwordService,
            Environment environment) {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        return args -> {
            logger.info("DataInitializer: checking default users (profile: {})", isProd ? "prod" : "dev");

            userRepository.findByUsername("admin").ifPresent(user -> {
                if (user.getPasswordHash().contains("placeholder")) {
                    if (isProd) {
                        String strongPassword = passwordService.generateRandomPassword(PROD_PASSWORD_LENGTH);
                        user.setPasswordHash(passwordService.hashPassword(strongPassword));
                        userRepository.save(user);
                        logger.warn("PROD admin default password (zapište si, po prvním přihlášení heslo změňte): {}", strongPassword);
                    } else {
                        user.setPasswordHash(passwordService.hashPassword("admin123"));
                        userRepository.save(user);
                        logger.info("Dev admin: admin / admin123");
                    }
                }
            });

            userRepository.findByUsername("technician").ifPresent(user -> {
                if (user.getPasswordHash().contains("placeholder")) {
                    if (isProd) {
                        String strongPassword = passwordService.generateRandomPassword(PROD_PASSWORD_LENGTH);
                        user.setPasswordHash(passwordService.hashPassword(strongPassword));
                        userRepository.save(user);
                        logger.warn("PROD technician default password (zapište si): {}", strongPassword);
                    } else {
                        user.setPasswordHash(passwordService.hashPassword("tech123"));
                        userRepository.save(user);
                        logger.info("Dev technician: technician / tech123");
                    }
                }
            });

            logger.info("DataInitializer: done");
        };
    }
}
