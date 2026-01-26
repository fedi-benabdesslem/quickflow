package com.ai.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test for Spring context loading.
 * 
 * Note: This test requires MongoDB to be running.
 * It is disabled when MongoDB is not available to allow unit tests to run independently.
 * 
 * To run this test, ensure MongoDB is running at localhost:27017 
 * or update application-test.properties with the correct MongoDB URI.
 */
@SpringBootTest
@DisabledIf("isMongoDbUnavailable")
class AiApplicationTests {

	@Test
	void contextLoads() {
	}

	static boolean isMongoDbUnavailable() {
		try {
			java.net.Socket socket = new java.net.Socket();
			socket.connect(new java.net.InetSocketAddress("localhost", 27017), 1000);
			socket.close();
			return false; // MongoDB is available
		} catch (Exception e) {
			return true; // MongoDB is unavailable, skip test
		}
	}
}

