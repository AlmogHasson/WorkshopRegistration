package patmal.workshops.logic.impl;

import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Team;
import patmal.workshops.api.model.Workshop;
import patmal.workshops.model.impl.InstructorImpl;
import patmal.workshops.model.impl.StudentImpl;
import patmal.workshops.model.impl.TeamImpl;
import patmal.workshops.model.impl.WorkShopImpl;

import java.util.*;
import java.util.stream.Collectors;

public class WorkshopSystemImpl implements WorkshopSystem {
    private final Map<String, Instructor> instructors = new HashMap<>();
    private final Map<String, Student> students = new HashMap<>();
    private final Map<Integer, Workshop> workshops = new HashMap<>();
    private final Map<Integer, Team> teams = new HashMap<>();

    private int nextWorkshopId = 1;
    private int nextTeamId = 1;

    @Override
    public Instructor createInstructor(String id, String name, String email) {
        if (instructors.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Instructor with ID " + id + " already exists."
            );
        }

        Instructor instructor = new InstructorImpl(id, name, email);
        instructors.put(id, instructor);
        return instructor;
    }

    @Override
    public Workshop createWorkshop(String name, String description) {
        Workshop workshop = new WorkShopImpl(nextWorkshopId, name, description, null);
        workshops.put(nextWorkshopId, workshop);
        nextWorkshopId++;
        return workshop;
    }

    @Override
    public void associateWorkshopWithInstructor(Instructor instructor, Workshop workshop) {
        ((WorkShopImpl) workshop).setInstructor(instructor);
        ((InstructorImpl) instructor).addWorkshop(workshop);
    }

    @Override
    public Student createStudent(String id, String name, String email) {
        if (students.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Student with ID " + id + " already exists."
            );
        }

        Student student = new StudentImpl(id, name, email);
        students.put(id, student);
        return student;
    }

    @Override
    public Team createTeam(String name, Workshop workshop, List<String> members) {
        Set<String> seenIds = new HashSet<>();
        List<Student> studentList = new ArrayList<>();

        // Validate every member before changing any state.
        for (String studentId : members) {
            if (!seenIds.add(studentId)) {
                throw new IllegalArgumentException(
                        "Duplicate student ID: " + studentId);
            }

            Student student = students.get(studentId);
            if (student == null) {
                throw new IllegalArgumentException(
                        "Unknown student ID: " + studentId);
            }

            boolean alreadyAssigned = teams.values().stream()
                    .anyMatch(team -> team.getStudents().contains(student));

            if (alreadyAssigned) {
                throw new IllegalArgumentException(
                        "Student already belongs to a team: " + studentId);
            }

            studentList.add(student);
        }

        TeamImpl team = new TeamImpl(nextTeamId, name, studentList, workshop);

        for (Student student : studentList) {
            ((StudentImpl) student).setTeam(team);
        }

        teams.put(nextTeamId, team);
        nextTeamId++;

        return team;
    }

    @Override
    public List<Team> getTeams() {
        return List.copyOf(teams.values());
    }

    @Override
    public Team getTeam(Integer id) {
        return teams.get(id);
    }

    @Override
    public List<Student> getStudents() {
        return List.copyOf(students.values());
    }

    @Override
    public Student getStudent(String id) {
        return students.get(id);
    }

    @Override
    public List<Workshop> getWorkshops() {
        return List.copyOf(workshops.values());
    }

    @Override
    public Workshop getWorkshop(Integer id) {
        return workshops.get(id);
    }

    @Override
    public List<Instructor> getInstructors() {
        return List.copyOf(instructors.values());
    }

    @Override
    public Instructor getInstructor(String id) {
        return instructors.get(id);
    }

    @Override
    public List<Student> getFreeStudents() {
        return students.values().stream()
                .filter(student -> teams.values().stream()
                        .noneMatch(team -> team.getStudents().contains(student)))
                .toList();
    }

    @Override
    public List<Student> getStudentsOfInstructor(String instructorId) {
        return teams.values().stream()
                .filter(team -> {Instructor instructor = team.getWorkshop().getInstructor();
                    return instructor != null && Objects.equals(instructor.getId(), instructorId);
                })
                .flatMap(team -> team.getStudents().stream())
                .toList();
    }

    @Override
    public List<Student> getStudentsOfWorkshop(Integer workshopId) {
        return teams.values().stream()
                .filter(team ->
                        Objects.equals(team.getWorkshop().getId(), workshopId))
                .flatMap(team -> team.getStudents().stream())
                .toList();
    }
}
