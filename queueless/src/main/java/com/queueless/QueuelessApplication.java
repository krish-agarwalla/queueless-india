package com.queueless;

import com.queueless.model.Organisation;
import com.queueless.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class QueuelessApplication {

	public static void main(String[] args) {
		SpringApplication.run(QueuelessApplication.class, args);
	}

	// This runs automatically exactly once right after the server starts
	@Bean
	public CommandLineRunner initData(
			OrganisationRepository orgRepo,
			UserRepository userRepo,
			org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {

		return args -> {
			if (userRepo.count() == 0) {
				// 1. Create the Super Admin (You)
				com.queueless.model.User superAdmin = new com.queueless.model.User();
				superAdmin.setName("Krish (Master Admin)");
				superAdmin.setEmail("admin@queueless.in");
				superAdmin.setPassword(passwordEncoder.encode("admin123")); // Securely hashed!
				superAdmin.setRole(com.queueless.model.enums.Role.SUPER_ADMIN);
				userRepo.save(superAdmin);
				System.out.println("👑 Master Admin created: admin@queueless.in / admin123");

				// 2. Create a Test Client (The Hospital)
				Organisation org = new Organisation();
				org.setName("City General Hospital");
				org.setType("Hospital");
				org.setPrefix("H");
				org = orgRepo.save(org);

				// 3. Create the Client Admin (Hospital Manager)
				com.queueless.model.User clientAdmin = new com.queueless.model.User();
				clientAdmin.setName("Hospital Manager");
				clientAdmin.setEmail("hospital@client.com");
				clientAdmin.setPassword(passwordEncoder.encode("client123"));
				clientAdmin.setRole(com.queueless.model.enums.Role.ORG_ADMIN);
				clientAdmin.setOrganisation(org); // Linked to the hospital!
				userRepo.save(clientAdmin);

				System.out.println("🏢 Test Client created: hospital@client.com / client123");
			}
		};
	}
}