// =====================================================
// ADD STUDENT
// =====================================================

const studentForm = document.getElementById("studentForm");

if (studentForm) {

    studentForm.addEventListener("submit", async function (event) {

        event.preventDefault();

        const button =
            document.getElementById("addStudentButton");

        const message =
            document.getElementById("addMessage");

        const rollNo =
            document.getElementById("rollNo").value.trim();

        const name =
            document.getElementById("studentName").value.trim();

        const branch =
            document.getElementById("studentBranch").value;

        const marks =
            Number(
                document.getElementById("studentMarks").value
            );

        const attendance =
            Number(
                document.getElementById("studentAttendance").value
            );


        // Validation

        if (
            !rollNo ||
            !name ||
            !branch ||
            isNaN(marks) ||
            isNaN(attendance)
        ) {

            message.textContent =
                "Please fill all fields.";

            message.className =
                "error";

            return;
        }


        if (marks < 0 || marks > 100) {

            message.textContent =
                "Marks must be between 0 and 100.";

            message.className =
                "error";

            return;
        }


        if (attendance < 0 || attendance > 100) {

            message.textContent =
                "Attendance must be between 0 and 100.";

            message.className =
                "error";

            return;
        }


        // Student object

        const student = {

            rollNo: Number(rollNo),

            name: name,

            branch: branch,

            marks: marks,

            attendance: attendance

        };


        console.log(
            "Sending student:",
            student
        );


        button.disabled = true;

        button.innerHTML =
            "Adding...";


        try {

            const response =
                await fetch("/students", {

                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body:
                        JSON.stringify(student)

                });


            const text =
                await response.text();


            if (!response.ok) {

                throw new Error(
                    text ||
                    "Failed to add student."
                );

            }


            message.textContent =
                "✓ Student added successfully!";

            message.className =
                "success";


            clearStudentForm();


        } catch (error) {

            console.error(
                "Add student error:",
                error
            );


            message.textContent =
                "✕ " + error.message;

            message.className =
                "error";


        } finally {

            button.disabled = false;

            button.innerHTML =
                "<span>＋</span> Add Student";

        }

    });

}



// =====================================================
// CLEAR STUDENT FORM
// =====================================================

function clearStudentForm() {

    document.getElementById("rollNo").value = "";

    document.getElementById("studentName").value = "";

    document.getElementById("studentBranch").value = "";

    document.getElementById("studentMarks").value = "";

    document.getElementById("studentAttendance").value = "";

}



// =====================================================
// RUN QUERY
// =====================================================

async function runQuery() {

    const question =
        document.getElementById("question").value.trim();

    const runButton =
        document.getElementById("runButton");

    const loading =
        document.getElementById("loading");

    const errorBox =
        document.getElementById("error");

    const sqlBox =
        document.getElementById("sql");

    const resultsBox =
        document.getElementById("results");

    const resultCount =
        document.getElementById("resultCount");


    // Check question

    if (!question) {

        errorBox.textContent =
            "Please enter a question.";

        errorBox.style.display =
            "block";

        return;
    }


    // Reset old messages

    errorBox.textContent = "";

    errorBox.style.display = "none";

    sqlBox.textContent =
        "Generating SQL...";

    resultsBox.innerHTML =
        "Loading results...";

    resultCount.textContent =
        "Loading...";


    loading.style.display =
        "block";

    runButton.disabled =
        true;

    runButton.innerHTML =
        "⏳ Running...";


    try {

        const response =
            await fetch("/api/query", {

                method: "POST",

                headers: {
                    "Content-Type":
                        "application/json"
                },

                body: JSON.stringify({
                    question: question
                })

            });


        const data =
            await response.json();


        console.log(
            "Query response:",
            data
        );


        // Backend error

        if (!response.ok || data.error) {

            throw new Error(
                data.error ||
                "Query failed."
            );

        }


        // Show generated SQL

        sqlBox.textContent =
            data.sql ||
            "SQL not generated.";


        // Show results

        const results =
            data.result || [];


        resultCount.textContent =
            results.length +
            (results.length === 1
                ? " record"
                : " records");


        if (results.length === 0) {

            resultsBox.innerHTML =
                "<div class='empty'>No records found.</div>";

        } else {

            createResultTable(
                resultsBox,
                results
            );

        }


    } catch (error) {

        console.error(
            "Query error:",
            error
        );


        sqlBox.textContent =
            "Unable to generate SQL.";


        resultsBox.innerHTML =
            "<div class='empty'>Query could not be executed.</div>";


        resultCount.textContent =
            "0 records";


        errorBox.textContent =
            "❌ " + error.message;


        errorBox.style.display =
            "block";

    } finally {

        loading.style.display =
            "none";

        runButton.disabled =
            false;

        runButton.innerHTML =
            "⚡ Run Query";

    }

}



// =====================================================
// CREATE RESULT TABLE
// =====================================================

function createResultTable(
    container,
    data
) {

    if (!data || data.length === 0) {

        container.innerHTML =
            "No records found.";

        return;

    }


    const table =
        document.createElement("table");


    table.className =
        "result-table";


    // Table header

    const thead =
        document.createElement("thead");


    const headerRow =
        document.createElement("tr");


    Object.keys(data[0]).forEach(
        function (column) {

            const th =
                document.createElement("th");

            th.textContent =
                column;

            headerRow.appendChild(th);

        }
    );


    thead.appendChild(headerRow);

    table.appendChild(thead);


    // Table body

    const tbody =
        document.createElement("tbody");


    data.forEach(
        function (row) {

            const tr =
                document.createElement("tr");


            Object.keys(data[0]).forEach(
                function (column) {

                    const td =
                        document.createElement("td");

                    td.textContent =
                        row[column] ?? "";

                    tr.appendChild(td);

                }
            );


            tbody.appendChild(tr);

        }
    );


    table.appendChild(tbody);


    container.innerHTML = "";

    container.appendChild(table);

}



// =====================================================
// COPY SQL
// =====================================================

async function copySQL() {

    const sql =
        document.getElementById("sql").textContent;


    if (
        !sql ||
        sql === "SQL query will appear here..."
    ) {

        return;

    }


    try {

        await navigator.clipboard.writeText(sql);

        alert("SQL copied successfully!");

    } catch (error) {

        console.error(
            "Copy error:",
            error
        );

    }

}



// =====================================================
// ENTER KEY SUPPORT
// =====================================================

const questionBox =
    document.getElementById("question");


if (questionBox) {

    questionBox.addEventListener(
        "keydown",
        function (event) {

            if (
                event.key === "Enter" &&
                event.ctrlKey
            ) {

                runQuery();

            }

        }
    );

}