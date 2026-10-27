package src;

public class Student {
    private int student_id;
    private String name;
    private String email;
    private String course;
    private int semester;

    public Student(int student_id, String name, String email, String course, int semester) {
        this.student_id = student_id;
        this.name = name;
        this.email = email;
        this.course = course;
        this.semester = semester;
    }

    // Getters and Setters
    public int getStudentId() { return student_id; }
    public void setStudentId(int student_id) { this.student_id = student_id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }
}
