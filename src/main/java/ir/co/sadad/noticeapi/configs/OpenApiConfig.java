package ir.co.sadad.noticeapi.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER).name("Authorization")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt", Arrays.asList("read", "write")))
                .info(new Info().title("Notice Application API").description(
                        "This is a documentation of notice-api with OpenAPI 3."));
    }

//    public OpenApiCustomiser getOpenApiCustomiser() {
//
//        return openAPI -> openAPI.getPaths().values().stream().flatMap(pathItem ->
//                pathItem.readOperations().stream())
//                .forEach(operation -> {
//                    operation.addParametersItem(new Parameter().name("Authorization").in("header").
//                            schema(new StringSchema().example("token")).required(true));
//                    operation.addParametersItem(new Parameter().name("ssn").in("header").
//                            schema(new StringSchema().example("test")).required(true));
//
//                });
//    }
}