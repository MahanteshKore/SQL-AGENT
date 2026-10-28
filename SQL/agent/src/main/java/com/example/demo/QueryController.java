package com.example.demo;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final ChatModel chatModel;
    private final JdbcTemplate jdbcTemplate;

    public QueryController(
            ChatModel chatModel,
            JdbcTemplate jdbcTemplate) {

        this.chatModel = chatModel;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/query")
    public Map<String, Object> query(
            @RequestBody Map<String, String> request) {

        String question = request.get("question");

        if (question == null || question.isBlank()) {
            return Map.of(
                    "error",
                    "Please enter a question."
            );
        }

        String sql;

        try {

            // ==========================================
            // TRY GEMINI
            // ==========================================

            String prompt = """
                    You are an expert PostgreSQL SQL generator.

                    Database table:

                    students(
                        id BIGINT,
                        roll_no INTEGER,
                        name VARCHAR,
                        branch VARCHAR,
                        marks INTEGER,
                        attendance DOUBLE PRECISION
                    )

                    Convert the user's question into ONE PostgreSQL SELECT query.

                    Rules:
                    - Return ONLY SQL.
                    - Only SELECT statements are allowed.
                    - Use only the students table.
                    - Available columns:
                      id, roll_no, name, branch, marks, attendance.
                    - Do not use INSERT.
                    - Do not use UPDATE.
                    - Do not use DELETE.
                    - Do not use DROP.
                    - Do not use ALTER.
                    - Do not use TRUNCATE.
                    - Do not use CREATE.
                    - Do not use markdown.
                    - Do not explain anything.

                    User question:
                    """ + question;

            sql = chatModel.chat(prompt).trim();

            sql = sql
                    .replace("```sql", "")
                    .replace("```SQL", "")
                    .replace("```", "")
                    .trim();

            System.out.println("Gemini Generated SQL: " + sql);

        } catch (Exception e) {

            // ==========================================
            // GEMINI FAILED → LOCAL FALLBACK
            // ==========================================

            System.out.println(
                    "Gemini unavailable. Using local SQL fallback."
            );

            sql = generateFallbackSQL(question);

            if (sql == null) {

                return Map.of(
                        "error",
                        "Gemini quota exceeded and this question is not supported by the local fallback."
                );
            }

            System.out.println(
                    "Fallback Generated SQL: " + sql
            );
        }

        // ==========================================
        // SECURITY CHECK
        // ==========================================

        String upperSql =
                sql.toUpperCase().trim();

        if (!upperSql.startsWith("SELECT")
        && !upperSql.startsWith("DELETE")) {

    return Map.of(
            "error",
            "Only SELECT and DELETE queries are allowed.",
            "sql",
            sql
    );
}

        String[] blockedWords = {
        "INSERT",
        "UPDATE",
        "DROP",
        "ALTER",
        "TRUNCATE",
        "CREATE"
};

        for (String word : blockedWords) {

            if (upperSql.matches(
                    ".*\\b" + word + "\\b.*")) {

                return Map.of(
                        "error",
                        "Unsafe SQL detected: " + word,
                        "sql",
                        sql
                );
            }
        }

        // ==========================================
        // EXECUTE SQL
        // ==========================================
        
        try {

    // ==========================================
    // SELECT QUERY
    // ==========================================

    if (upperSql.startsWith("SELECT")) {

        List<Map<String, Object>> result =
                jdbcTemplate.queryForList(sql);

        return Map.of(
                "question",
                question,
                "sql",
                sql,
                "result",
                result
        );
    }


    // ==========================================
    // DELETE QUERY
    // ==========================================

    if (upperSql.startsWith("DELETE")) {

        int deletedRows =
                jdbcTemplate.update(sql);

        return Map.of(
                "question",
                question,
                "sql",
                sql,
                "deletedRows",
                deletedRows,
                "message",
                deletedRows > 0
                        ? "Student deleted successfully."
                        : "No matching student found.",
                "result",
                List.of()
        );
    }


    return Map.of(
            "error",
            "Unsupported SQL operation.",
            "sql",
            sql
    );


} catch (Exception e) {

    e.printStackTrace();

    return Map.of(
            "error",
            "Database error: " +
                    (e.getMessage() != null
                            ? e.getMessage()
                            : "Unknown error"),
            "sql",
            sql
    );
}
    }

    
    // =====================================================
    // LOCAL FALLBACK SQL GENERATOR
    // =====================================================

    private String generateFallbackSQL(String question) {

        String q =
                question.toLowerCase().trim();


        // ---------------------------------------------
        // TOPPER / HIGHEST-MARKS STUDENT
        // ---------------------------------------------
        if (q.contains("topper") ||
                q.contains("highest student") ||
                q.contains("highest marks") ||
                q.contains("highest mark") ||
                q.contains("maximum marks") ||
                q.contains("most marks") ||
                q.contains("student who scored the highest") ||
                q.contains("student with the highest")) {
            return "SELECT * FROM students ORDER BY marks DESC LIMIT 1";
        }

        // ---------------------------------------------
        // STUDENTS IN ASCENDING ORDER (LOWEST TO HIGHEST MARKS)
        // ---------------------------------------------
        if ((q.contains("ascending") || q.contains("asc order") || q.contains("low to high")) &&
                (q.contains("student") || q.contains("table") || q.contains("marks"))) {
            return "SELECT * FROM students ORDER BY marks ASC";
        }

        // ---------------------------------------------
        // STUDENTS IN DESCENDING ORDER (HIGHEST TO LOWEST MARKS)
        // ---------------------------------------------
        if ((q.contains("descending") || q.contains("desc order") || q.contains("high to low")) &&
                (q.contains("student") || q.contains("table") || q.contains("marks"))) {
            return "SELECT * FROM students ORDER BY marks DESC";
        }

        // ---------------------------------------------
        // SHOW ALL STUDENTS
        // ---------------------------------------------
        if (q.contains("show all students") ||
                q.contains("list all students") ||
                q.equals("show students") ||
                q.equals("list students") ||
                q.contains("show student table") ||
                q.contains("list student table") ||
                q.equals("show all records")) {
            return "SELECT * FROM students";
        }


        // ---------------------------------------------
        // MARKS ABOVE
        // Example:
        // Show students with marks above 85
        // ---------------------------------------------

        if (
                q.contains("marks above") ||
                q.contains("marks greater than") ||
                q.contains("marks more than")
        ) {

            Integer marks =
                    extractNumber(q);

            if (marks != null) {

                return
                        "SELECT * FROM students " +
                        "WHERE marks > " + marks;
            }
        }


        // ---------------------------------------------
        // MARKS BELOW
        // ---------------------------------------------

        if (
                q.contains("marks below") ||
                q.contains("marks less than") ||
                q.contains("marks under")
        ) {

            Integer marks =
                    extractNumber(q);

            if (marks != null) {

                return
                        "SELECT * FROM students " +
                        "WHERE marks < " + marks;
            }
        }


        // ---------------------------------------------
        // MARKS EQUAL TO
        // ---------------------------------------------

        if (
                q.contains("marks equal") ||
                q.contains("marks =")
        ) {

            Integer marks =
                    extractNumber(q);

            if (marks != null) {

                return
                        "SELECT * FROM students " +
                        "WHERE marks = " + marks;
            }
        }


        // ---------------------------------------------
        // CSE STUDENTS
        // ---------------------------------------------

        if (q.contains("cse students")) {

            return
                    "SELECT * FROM students " +
                    "WHERE branch = 'CSE'";
        }
        // ---------------------------------------------
        // AI-ML STUDENTS
        // ---------------------------------------------
         if (q.contains("ai-ml students")) {

            return
                    "SELECT * FROM students " +
                    "WHERE branch = 'AI-ML'";
        }


        // ---------------------------------------------
        // IT STUDENTS
        // ---------------------------------------------

        if (q.contains("it students")) {

            return
                    "SELECT * FROM students " +
                    "WHERE branch = 'IT'";
        }


        // ---------------------------------------------
        // ECE STUDENTS
        // ---------------------------------------------

        if (q.contains("ece students")) {

            return
                    "SELECT * FROM students " +
                    "WHERE branch = 'ECE'";
        }


        // ---------------------------------------------
        // ME STUDENTS
        // ---------------------------------------------

        if (q.contains("me students")) {

            return
                    "SELECT * FROM students " +
                    "WHERE branch = 'ME'";
        }


        // ---------------------------------------------
        // ATTENDANCE ABOVE
        // Example:
        // Show students with attendance above 80
        // ---------------------------------------------

        if (
                q.contains("attendance above") ||
                q.contains("attendance greater than") ||
                q.contains("attendance more than")
        ) {

            Integer attendance =
                    extractNumber(q);

            if (attendance != null) {

                return
                        "SELECT * FROM students " +
                        "WHERE attendance > " +
                        attendance;
            }
        }

        // ---------------------------------------------
// =============================================
// DELETE STUDENT BY NAME
// Examples:
// delete prem
// delete rahul
// delete student prem
// delete student rahul
// delete Priya
// =============================================

if (q.startsWith("delete")) {

    String name = q.substring("delete".length()).trim();

    // Remove the word "student" if present
    if (name.startsWith("student ")) {
        name = name.substring("student".length()).trim();
    }

    if (!name.isEmpty()) {

        // Escape single quotes
        String safeName = name.replace("'", "''");

        return "DELETE FROM students " +
               "WHERE LOWER(name) = LOWER('" +
               safeName +
               "')";
    }
}
        // ---------------------------------------------
        // ATTENDANCE BELOW
        // ---------------------------------------------

        if (
                q.contains("attendance below") ||
                q.contains("attendance less than") ||
                q.contains("attendance under")
        ) {

            Integer attendance =
                    extractNumber(q);

            if (attendance != null) {

                return
                        "SELECT * FROM students " +
                        "WHERE attendance < " +
                        attendance;
            }
        }


        return null;
    }


    // =====================================================
    // EXTRACT NUMBER FROM QUESTION
    // =====================================================

    private Integer extractNumber(String text) {

        String[] words =
                text.split("\\s+");

        for (String word : words) {

            String number =
                    word.replaceAll(
                            "[^0-9]",
                            ""
                    );

            if (!number.isEmpty()) {

                try {

                    return Integer.parseInt(number);

                } catch (NumberFormatException ignored) {

                }
            }
        }

        return null;
    }
}
// package com.example.demo;

// import dev.langchain4j.model.chat.ChatModel;
// import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;
// import java.util.Map;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

// @RestController
// @RequestMapping("/api")
// public class QueryController {

//     private final ChatModel chatModel;
//     private final JdbcTemplate jdbcTemplate;

//     public QueryController(
//             ChatModel chatModel,
//             JdbcTemplate jdbcTemplate) {

//         this.chatModel = chatModel;
//         this.jdbcTemplate = jdbcTemplate;
//     }

//     // =====================================================
//     // MAIN QUERY ENDPOINT
//     // =====================================================

//     @PostMapping("/query")
//     public Map<String, Object> query(
//             @RequestBody Map<String, String> request) {

//         String question = request.get("question");

//         if (question == null || question.isBlank()) {
//             return Map.of(
//                     "error",
//                     "Please enter a question."
//             );
//         }

//         String sql;

//         =================================================
//         STEP 1: TRY LOCAL SQL FIRST
//         No Gemini API call for supported questions
//         =================================================

//         sql = generateFallbackSQL(question);

//         if (sql != null) {

//             System.out.println(
//                     "Using local SQL: " + sql
//             );

//         } else {

//             // =============================================
//             // STEP 2: USE GEMINI ONLY WHEN NEEDED
//             // =============================================

//             try {

//                 String prompt = """
//                         You are a PostgreSQL SQL generator.

//                         Database table:
//                         students(
//                             id BIGINT,
//                             roll_no INTEGER,
//                             name VARCHAR,
//                             branch VARCHAR,
//                             marks INTEGER,
//                             attendance DOUBLE PRECISION
//                         )

//                         Convert the user's question into exactly
//                         one PostgreSQL SQL statement.

//                         Allowed operations:
//                         - SELECT
//                         - DELETE

//                         Rules:
//                         - Use only the students table.
//                         - Use only the listed columns.
//                         - DELETE must include a specific WHERE condition.
//                         - Never generate INSERT, UPDATE, DROP, ALTER,
//                           TRUNCATE, CREATE, or other operations.
//                         - Never generate multiple SQL statements.
//                         - Return SQL only.
//                         - Do not use markdown.
//                         - Do not explain anything.

//                         User question:
//                         """ + question;

//                 sql = chatModel.chat(prompt).trim();

//                 sql = sql
//                         .replace("```sql", "")
//                         .replace("```SQL", "")
//                         .replace("```", "")
//                         .trim();

//                 System.out.println(
//                         "Gemini Generated SQL: " + sql
//                 );

//             } catch (Exception e) {

//                 System.out.println(
//                         "Gemini unavailable: " + e.getMessage()
//                 );

//                 return Map.of(
//                         "error",
//                         "Gemini is unavailable, and this question is not supported by the local fallback."
//                 );
//             }
//         }

//         // =================================================
//         // STEP 3: VALIDATE SQL
//         // =================================================

//         sql = sql.trim();

//         // Remove one optional trailing semicolon.
//         if (sql.endsWith(";")) {
//             sql = sql.substring(0, sql.length() - 1).trim();
//         }

//         String upperSql = sql.toUpperCase();

//         // Reject multiple statements.
//         if (sql.contains(";")) {
//             return Map.of(
//                     "error",
//                     "Multiple SQL statements are not allowed.",
//                     "sql",
//                     sql
//             );
//         }

//         // Allow only SELECT and DELETE.
//         if (!upperSql.startsWith("SELECT ")
//                 && !upperSql.startsWith("SELECT*")
//                 && !upperSql.startsWith("DELETE ")) {

//             return Map.of(
//                     "error",
//                     "Only SELECT and DELETE queries are allowed.",
//                     "sql",
//                     sql
//             );
//         }

//         // Block SQL comments.
//         if (sql.contains("--")
//                 || sql.contains("/*")
//                 || sql.contains("*/")) {

//             return Map.of(
//                     "error",
//                     "SQL comments are not allowed.",
//                     "sql",
//                     sql
//             );
//         }

//         // Block dangerous operations.
//         String[] blockedWords = {
//                 "INSERT",
//                 "UPDATE",
//                 "DROP",
//                 "ALTER",
//                 "TRUNCATE",
//                 "CREATE",
//                 "GRANT",
//                 "REVOKE",
//                 "EXECUTE",
//                 "CALL"
//         };

//         for (String word : blockedWords) {

//             if (Pattern.compile(
//                     "\\b" + word + "\\b",
//                     Pattern.CASE_INSENSITIVE
//             ).matcher(sql).find()) {

//                 return Map.of(
//                         "error",
//                         "Unsafe SQL detected: " + word,
//                         "sql",
//                         sql
//                 );
//             }
//         }

//         // Restrict queries to the students table.
//         if (!Pattern.compile(
//                 "\\bFROM\\s+students\\b",
//                 Pattern.CASE_INSENSITIVE
//         ).matcher(sql).find()) {

//             return Map.of(
//                     "error",
//                     "Queries must use the students table.",
//                     "sql",
//                     sql
//             );
//         }

//         // DELETE must contain a WHERE condition.
//         if (upperSql.startsWith("DELETE")
//                 && !Pattern.compile(
//                         "\\bWHERE\\b",
//                         Pattern.CASE_INSENSITIVE
//                 ).matcher(sql).find()) {

//             return Map.of(
//                     "error",
//                     "DELETE requires a WHERE condition.",
//                     "sql",
//                     sql
//             );
//         }

//         // =================================================
//         // STEP 4: EXECUTE SQL
//         // =================================================

//         try {

//             // ---------------------------------------------
//             // SELECT
//             // ---------------------------------------------

//             if (upperSql.startsWith("SELECT")) {

//                 List<Map<String, Object>> result =
//                         jdbcTemplate.queryForList(sql);

//                 return Map.of(
//                         "question", question,
//                         "sql", sql,
//                         "result", result,
//                         "message", "Query executed successfully."
//                 );
//             }

//             // ---------------------------------------------
//             // DELETE
//             // ---------------------------------------------

//             if (upperSql.startsWith("DELETE")) {

//                 int deletedRows =
//                         jdbcTemplate.update(sql);

//                 return Map.of(
//                         "question", question,
//                         "sql", sql,
//                         "deletedRows", deletedRows,
//                         "message",
//                         deletedRows > 0
//                                 ? deletedRows
//                                     + " student record(s) deleted successfully."
//                                 : "No matching student found.",
//                         "result", List.of()
//                 );
//             }

//             return Map.of(
//                     "error",
//                     "Unsupported SQL operation."
//             );

//         } catch (Exception e) {

//             e.printStackTrace();

//             return Map.of(
//                     "error",
//                     "Database error: "
//                             + (e.getMessage() != null
//                             ? e.getMessage()
//                             : "Unknown error"),
//                     "sql",
//                     sql
//             );
//         }
//     }


//     // =====================================================
//     // LOCAL SQL GENERATOR
//     // =====================================================

//     private String generateFallbackSQL(String question) {

//         String q = question.toLowerCase().trim();

//         // =============================================
//         // DELETE STUDENT BY NAME
//         // Examples:
//         // Delete Prem
//         // Delete student Rahul
//         // Delete Abhishk
//         // =============================================

//         if (q.startsWith("delete ")) {

//             String name = q.substring("delete ".length()).trim();

//             if (name.startsWith("student ")) {
//                 name = name.substring("student ".length()).trim();
//             }

//             // Only accept a single student name here.
//             // Do not interpret broader delete commands as names.
//             if (!name.isEmpty()
//                     && name.matches("[\\p{L}][\\p{L} .'-]*")
//                     && !name.contains(" from ")
//                     && !name.equals("all")
//                     && !name.equals("students")) {

//                 String safeName = name.replace("'", "''");

//                 return "DELETE FROM students "
//                         + "WHERE LOWER(name) = LOWER('"
//                         + safeName + "')";
//             }
//         }

//         // =============================================
//         // SHOW ALL STUDENTS
//         // =============================================

//         if (q.contains("show all students")
//                 || q.contains("list all students")
//                 || q.equals("show students")
//                 || q.equals("list students")) {

//             return "SELECT * FROM students";
//         }

//         // =============================================
//         // MARKS ABOVE
//         // =============================================

//         if (q.contains("marks above")
//                 || q.contains("marks greater than")
//                 || q.contains("marks more than")) {

//             Integer number = extractNumber(q);

//             if (number != null) {
//                 return "SELECT * FROM students WHERE marks > " + number;
//             }
//         }

//         // =============================================
//         // MARKS BELOW
//         // =============================================

//         if (q.contains("marks below")
//                 || q.contains("marks less than")
//                 || q.contains("marks under")) {

//             Integer number = extractNumber(q);

//             if (number != null) {
//                 return "SELECT * FROM students WHERE marks < " + number;
//             }
//         }

//         // =============================================
//         // MARKS EQUAL TO
//         // =============================================

//         if (q.contains("marks equal")
//                 || q.contains("marks =")) {

//             Integer number = extractNumber(q);

//             if (number != null) {
//                 return "SELECT * FROM students WHERE marks = " + number;
//             }
//         }

//         // =============================================
//         // ATTENDANCE ABOVE
//         // =============================================

//         if (q.contains("attendance above")
//                 || q.contains("attendance greater than")
//                 || q.contains("attendance more than")) {

//             Integer number = extractNumber(q);

//             if (number != null) {
//                 return "SELECT * FROM students WHERE attendance > " + number;
//             }
//         }

//         // =============================================
//         // ATTENDANCE BELOW
//         // =============================================

//         if (q.contains("attendance below")
//                 || q.contains("attendance less than")
//                 || q.contains("attendance under")) {

//             Integer number = extractNumber(q);

//             if (number != null) {
//                 return "SELECT * FROM students WHERE attendance < " + number;
//             }
//         }

//         // =============================================
//         // BRANCH FILTERS
//         // =============================================

//         String[] branches = {
//                 "CSE",
//                 "AI-ML",
//                 "IT",
//                 "ECE",
//                 "ME",
//                 "EEE",
//                 "CIVIL"
//         };

//         for (String branch : branches) {

//             String lowerBranch = branch.toLowerCase();

//             if (q.contains(lowerBranch + " students")) {

//                 return "SELECT * FROM students WHERE branch = '"
//                         + branch + "'";
//             }
//         }

//         // =============================================
//         // NO LOCAL MATCH
//         // =============================================

//         return null;
//     }


//     // =====================================================
//     // EXTRACT NUMBER FROM QUESTION
//     // =====================================================

//     private Integer extractNumber(String text) {

//         Matcher matcher =
//                 Pattern.compile("\\b\\d+\\b").matcher(text);

//         if (matcher.find()) {

//             try {
//                 return Integer.parseInt(matcher.group());
//             } catch (NumberFormatException e) {
//                 return null;
//             }
//         }

//         return null;
//     }
// }
