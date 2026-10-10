package com.example.demo;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GeminiController {

    private final ChatModel chatModel;

    public GeminiController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String question) {

        String prompt = """
                You are an expert PostgreSQL SQL generator.

                Database table:
                students(
                    id BIGINT,
                    name VARCHAR,
                    branch VARCHAR,
                    marks INTEGER,
                    attendance DOUBLE PRECISION
                )

                Convert the user's question into a PostgreSQL SELECT query.

                Rules:
                - Return ONLY the SQL query.
                - Use only SELECT statements.
                - Do not use INSERT, UPDATE, DELETE, DROP, ALTER, TRUNCATE or CREATE.
                - Use the table name students.
                - Do not add markdown or explanations.

                User question:
                """ + question;

        return chatModel.chat(prompt);
    }
}
// $env:GEMINI_API_KEY="AQ.Ab8RN6JN-txV8UWAiri5nJppAA-8h6iYtZnKT66LDoh-1xFORA"
// cd E:\demo\demo
// .\mvnw.cmd spring-boot:run
//http://localhost:8080/
//if ($env:GEMINI_API_KEY) { "API KEY IS SET" } else { "API KEY IS MISSING" }