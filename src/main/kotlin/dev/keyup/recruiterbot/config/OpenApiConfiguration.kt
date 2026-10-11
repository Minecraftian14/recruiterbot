package dev.keyup.recruiterbot.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "Recruiter Bot API",
        version = "1.0.0",
        description = "Ranks seeded candidate/job matches. Scores use skill overlap (70%), experience fit (20%), and culture overlap (10%), less a soft availability penalty (0/5/15 points). Scores are clamped to 0–100 and rounded to two decimals using HALF_UP. Results sort by score, matched skill count, culture count, normalized display name, then ID. Missing source records return 404; existing records return 200 even when their match list is empty.",
    ),
)
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
class OpenApiConfiguration
