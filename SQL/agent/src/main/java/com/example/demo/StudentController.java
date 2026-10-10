package com.example.demo;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StudentController {

    private final StudentRepository studentRepository;

    public StudentController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }
    @DeleteMapping("/students/{id}")
public void deleteStudent(@PathVariable Long id) {
    studentRepository.deleteById(id);
}
    @GetMapping("/students")
    public List<Student> getStudents() {
        return studentRepository.findAll();
    }

    @PostMapping("/students")
    public Student addStudent(@RequestBody Student student) {

        System.out.println(
                "Adding student: "
                        + student.getRollNo()
                        + " - "
                        + student.getName()
                        + " - Attendance: "
                        + student.getAttendance()
        );

        return studentRepository.save(student);
    }
}