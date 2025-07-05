package com.piats.dms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The main entry point for the Document Management System (DMS) application.
 * <p>
 * This class initializes and runs the Spring Boot application.
 * </p>
 */
@SpringBootApplication
public class DmsApplication {

	/**
	 * The main method that serves as the entry point for the Java application.
	 *
	 * @param args Command-line arguments passed to the application.
	 */
	public static void main(String[] args) {
		SpringApplication.run(DmsApplication.class, args);
	}

}
