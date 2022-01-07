package com.github.sh;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class LoadbalancerServerApplication {


	public static void main(String[] args) {
		SpringApplication.run(LoadbalancerServerApplication.class, args);
	}

	@GetMapping("/str")
	public String greet() {
		List<String> stringList = Arrays.asList("A", "B", "C");
		Random rand = new Random();
		int randomNum = rand.nextInt(stringList.size());
		return stringList.get(randomNum);
	}

}
