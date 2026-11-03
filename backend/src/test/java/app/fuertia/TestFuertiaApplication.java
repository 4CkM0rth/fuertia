package app.fuertia;

import org.springframework.boot.SpringApplication;

public class TestFuertiaApplication {

	public static void main(String[] args) {
		SpringApplication.from(FuertiaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
