package dev.keyup.recruiterbot.persistence

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager

class SqlDetectiveIntegrationTest {
    @Test
    fun `fixture and all five challenge queries execute against SQLite`() {
        Class.forName("org.sqlite.JDBC")
        val sql = Files.readString(Path.of("sql_detective.sql"))
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            connection.createStatement().use { statement ->
                val results = mutableListOf<Int>()
                var hasZeroFinalCount = false
                val duplicateEmails = mutableListOf<String>()
                val conversionRates = mutableListOf<Double>()
                sql.split(';').map(String::trim).filter(String::isNotEmpty).forEach { command ->
                    val hasRows = statement.execute(command)
                    if (command.contains("-- Question") && hasRows) {
                        statement.resultSet.use { rows ->
                            var count = 0
                            while (rows.next()) {
                                count++
                                when {
                                    command.contains("Question 2") -> hasZeroFinalCount = hasZeroFinalCount || rows.getInt("final_applicant_count") == 0
                                    command.contains("Question 3") -> duplicateEmails += rows.getString("email")
                                    command.contains("Question 4") -> conversionRates += rows.getDouble("conversion_rate_percent")
                                }
                            }
                            results += count
                        }
                    }
                }
                assertEquals(listOf(4, 7, 3, 2, 3), results)
                org.junit.jupiter.api.Assertions.assertTrue(hasZeroFinalCount)
                assertEquals(listOf("ananya.rao@mail.com", "ben.turner@mail.com", "elena.popescu@mail.com"), duplicateEmails)
                assertEquals(listOf(75.0, 50.0), conversionRates)
                assertEquals(5, scalar(statement, "SELECT COUNT(*) FROM recruiters"))
                assertEquals(7, scalar(statement, "SELECT COUNT(*) FROM job_postings"))
                assertEquals(11, scalar(statement, "SELECT COUNT(*) FROM applicants"))
                assertEquals(24, scalar(statement, "SELECT COUNT(*) FROM interviews"))
            }
        }
    }

    private fun scalar(statement: java.sql.Statement, sql: String): Int =
        statement.executeQuery(sql).use { rows -> rows.next(); rows.getInt(1) }
}
