package dev.keyup.recruiterbot.architecture

import dev.keyup.recruiterbot.RecruiterbotApplication
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

class ModuleArchitectureTest {
    @Test
    fun `modules have no dependency cycles`() {
        ApplicationModules.of(RecruiterbotApplication::class.java).verify()
    }
}
