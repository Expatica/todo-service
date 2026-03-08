package com.expatica.todoservice;

import com.expatica.todoservice.config.TestClockConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestClockConfiguration.class)
class TodoServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
