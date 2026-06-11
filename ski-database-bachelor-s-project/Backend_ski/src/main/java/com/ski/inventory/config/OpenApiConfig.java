package com.ski.inventory.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;
import org.springdoc.core.customizers.OperationCustomizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String COOKIE_AUTH = "cookieAuth";

    private static final String PERMISSIONS_DESCRIPTION = """
            **Oprávnění dle cest (výchozí):**
            - `/api/auth/**` – bez přihlášení
            - `/api/public/**` – bez přihlášení
            - `/api/admin/**` – pouze **ADMIN**
            - `/api/technician/**` – **ADMIN** nebo **TECHNICIAN** (u některých operací jen ADMIN – viz popis endpointu)
            - `/api/customer/**` – **ADMIN** nebo **CUSTOMER**
            """;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ski Inventory API")
                        .description("REST API pro správu inventáře lyží – objednávky, lyže, zákazníci, uživatelé.\n\n"
                                + "**Autentizace:** `POST /api/auth/login` a `POST /api/auth/login/customer` nastaví JWT do HttpOnly cookie `access_token`. "
                                + "Token se nevrací v JSON odpovědi. API klienti musí u dalších požadavků posílat cookie `access_token`; alternativně lze stejný JWT poslat v hlavičce `Authorization: Bearer <token>`.\n\n"
                                + PERMISSIONS_DESCRIPTION)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Ski Inventory")))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(COOKIE_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name("access_token")
                                        .description("JWT uložený serverem po loginu jako HttpOnly cookie. Login response vrací jen informace o session, ne token v JSON těle."))
                        .addSecuritySchemes(BEARER_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Alternativní autentizace pro API klienty: pošli JWT v hlavičce Authorization: Bearer <token>. Login endpoint token nevrací v JSON těle; standardně ho nastavuje do cookie access_token.")));
    }

    /** Doplní u každého endpointu v Swaggeru text o požadovaných rolích z @PreAuthorize. */
    @Bean
    public OperationCustomizer preAuthorizeOperationCustomizer() {
        return (operation, handlerMethod) -> {
            String roles = extractRolesFromPreAuthorize(handlerMethod);
            if (roles != null && !roles.isEmpty()) {
                String suffix = "\n\n**Oprávnění:** " + roles;
                operation.setDescription(operation.getDescription() == null ? suffix.trim() : operation.getDescription() + suffix);
            }
            return operation;
        };
    }

    private static String extractRolesFromPreAuthorize(HandlerMethod handlerMethod) {
        PreAuthorize preAuthorize = handlerMethod.getMethodAnnotation(PreAuthorize.class);
        if (preAuthorize == null) {
            preAuthorize = handlerMethod.getBeanType().getAnnotation(PreAuthorize.class);
        }
        if (preAuthorize == null || preAuthorize.value() == null || preAuthorize.value().isBlank()) {
            return null;
        }
        return parseRoles(preAuthorize.value());
    }

    /** Parsuje výraz typu hasRole('ADMIN') nebo hasAnyRole('ADMIN', 'TECHNICIAN') na "ADMIN" resp. "ADMIN, TECHNICIAN". */
    private static String parseRoles(String expression) {
        // hasRole('X') nebo hasAnyRole('X', 'Y')
        Pattern p = Pattern.compile("'(\\w+)'");
        Matcher m = p.matcher(expression);
        String roles = m.results()
                .map(mr -> mr.group(1))
                .collect(Collectors.joining(", "));
        return roles.isEmpty() ? null : roles;
    }
}
