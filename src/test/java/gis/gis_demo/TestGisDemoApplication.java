package gis.gis_demo;

import org.springframework.boot.SpringApplication;

public class TestGisDemoApplication {

	public static void main(String[] args) {
		SpringApplication.from(GisDemoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
