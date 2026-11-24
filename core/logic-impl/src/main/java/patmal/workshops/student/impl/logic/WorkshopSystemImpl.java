package patmal.workshops.student.impl.logic;

import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Team;
import patmal.workshops.api.model.Workshop;
import patmal.workshops.student.impl.model.InstructorImpl;
import patmal.workshops.student.impl.model.StudentImpl;
import patmal.workshops.student.impl.model.TeamImpl;
import patmal.workshops.student.impl.model.WorkShopImpl;
import java.io.File;
import java.util.*;
import org.apache.commons.validator.routines.EmailValidator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import patmal.workshops.api.model.Person;
import org.apache.commons.csv.CSVException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class WorkshopSystemImpl implements WorkshopSystem {
    //serialize nulls to ensure that all fields are included in the JSON output, even if they are null
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final Logger LOGGER = LogManager.getLogger(WorkshopSystemImpl.class);

    // Using LinkedHashMap to maintain insertion order
    private final Map<String, Instructor> instructors = new LinkedHashMap<>();
    private final Map<String, Student> students = new LinkedHashMap<>();
    private final Map<Integer, Workshop> workshops = new LinkedHashMap<>();
    private final Map<Integer, Team> teams = new LinkedHashMap<>();

    private int nextWorkshopId = 1;
    private int nextTeamId = 1;

    @Override
    public void exportInstructorsDataToJson(File targetFileName) {
        JsonArray result = new JsonArray();

        for (Instructor instructor : instructors.values()) {
            JsonObject instructorJson = personDetails(instructor);
            JsonArray workshopArray = new JsonArray();

            for (Workshop workshop : workshops.values()) {

                if (!instructor.equals(workshop.getInstructor())) {
                    continue;
                }

                JsonObject workshopJson = workshopDetails(workshop);
                JsonArray teamArray = new JsonArray();
                for (Team team : teams.values()) {
                    if (workshop.getId().equals(team.getWorkshop().getId())) {
                        teamArray.add(teamWithStudents(team));
                    }
                }
                workshopJson.add("teams", teamArray);
                workshopArray.add(workshopJson);
            }

            instructorJson.add("workshops", workshopArray);
            result.add(instructorJson);
        }

        writeJson(targetFileName, result);
    }

    @Override
    public void exportStudentsDataToJson(File targetFileName) {
        JsonArray result = new JsonArray();

        for (Student student : students.values()) {
            JsonObject studentJson = personDetails(student);
            studentJson.add("team", JsonNull.INSTANCE);

            for (Team team : teams.values()) {
                if (!team.getStudents().contains(student)) {
                    continue;
                }

                JsonObject teamJson = new JsonObject();
                teamJson.addProperty("id", team.getId());
                teamJson.addProperty("name", team.getName());

                JsonArray studentIds = new JsonArray();
                for (Student member : team.getStudents()) {
                    studentIds.add(member.getId());
                }

                teamJson.add("studentIds", studentIds);
                teamJson.add("workshop", workshopWithInstructor(team.getWorkshop()));
                studentJson.add("team", teamJson);
                break;
            }

            result.add(studentJson);
        }

        writeJson(targetFileName, result);
    }

    @Override
    public void exportTeamsDataToJson(File targetFileName) {
        JsonArray result = new JsonArray();

        for (Team team : teams.values()) {
            JsonObject teamJson = teamWithStudents(team);
            teamJson.add("workshop", workshopWithInstructor(team.getWorkshop()));
            result.add(teamJson);
        }

        writeJson(targetFileName, result);
    }

    @Override
    public Instructor createInstructor(String id, String name, String email) {
        if (instructors.containsKey(id)) {
            throw rejected(
                    "Instructor with ID " + id + " already exists."
            );
        }

        validateEmail(email);

        Instructor instructor = new InstructorImpl(id, name, email);
        instructors.put(id, instructor);

        LOGGER.info("Created instructor {}", instructor.getId());
        return instructor;
    }

    @Override
    public Workshop createWorkshop(String name, String description) {
        Workshop workshop = new WorkShopImpl(nextWorkshopId, name, description, null);
        workshops.put(nextWorkshopId, workshop);
        nextWorkshopId++;
        LOGGER.info("Created workshop {}", workshop.getId());
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
            throw rejected(
                    "Student with ID " + id + " already exists."
            );
        }

        validateEmail(email);

        Student student = new StudentImpl(id, name, email);
        LOGGER.info("Created student {}", student.getId());
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
                throw rejected(
                        "Duplicate student ID: " + studentId);
            }

            Student student = students.get(studentId);
            if (student == null) {
                throw rejected(
                        "Unknown student ID: " + studentId);
            }

            boolean alreadyAssigned = teams.values().stream()
                    .anyMatch(team -> team.getStudents().contains(student));

            if (alreadyAssigned) {
                throw rejected(
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

        LOGGER.info("Created team {}", team.getId());
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

    private static void validateEmail(String email) {
        if (!EmailValidator.getInstance().isValid(email)) {
            throw rejected("Invalid email address.", null);
        }
    }

    private static void writeJson(File targetFile, JsonArray data) {
        try {
            Files.writeString(targetFile.toPath(), GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write JSON to " + targetFile, e);
        }
    }

    private static JsonObject personDetails(Person person) {
        JsonObject json = new JsonObject();
        json.addProperty("id", person.getId());
        json.addProperty("name", person.getName());
        json.addProperty("email", person.getEmail());
        return json;
    }

    private static JsonObject workshopDetails(Workshop workshop) {
        JsonObject json = new JsonObject();
        json.addProperty("id", workshop.getId());
        json.addProperty("name", workshop.getName());
        json.addProperty("description", workshop.getDescription());
        return json;
    }

    private static JsonObject teamWithStudents(Team team) {
        JsonObject json = new JsonObject();
        json.addProperty("id", team.getId());
        json.addProperty("name", team.getName());

        JsonArray members = new JsonArray();
        for (Student student : team.getStudents()) {
            members.add(personDetails(student));
        }
        json.add("students", members);
        return json;
    }

    private static JsonObject workshopWithInstructor(Workshop workshop) {
        JsonObject json = workshopDetails(workshop);
        Instructor instructor = workshop.getInstructor();
        json.add("instructor", instructor == null ? JsonNull.INSTANCE : personDetails(instructor));
        return json;
    }

    @Override
    public List<Student> importStudentDataFromFile(File source) {
        List<Student> imported = new ArrayList<>();

        try (var reader = Files.newBufferedReader(source.toPath(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.parse(reader)) {

            Iterator<CSVRecord> rows = parser.iterator();

            if (!rows.hasNext() || !rows.next().toList().equals(List.of("id", "name", "email"))) {
                throw rejected("Expected CSV header: id,name,email");
            }

            while (rows.hasNext()) {
                CSVRecord row = rows.next();
                if (row.size() != 3) {
                    throw rejected(
                            "Expected 3 values in CSV record " + row.getRecordNumber());
                }

                Student student = createStudent(row.get(0), row.get(1), row.get(2));
                imported.add(student);
            }
        } catch (UncheckedIOException e) {
            if (e.getCause() instanceof CSVException) {
                throw rejected("Malformed CSV in " + source, e.getCause());
            }
            throw e;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + source, e);
        }

        return imported;
    }

    private static IllegalArgumentException rejected(String message) {
        LOGGER.warn("Operation rejected: {}", message);
        return rejected(message);
    }

    private static IllegalArgumentException rejected(String message, Throwable cause) {
        LOGGER.warn("Operation rejected: {}", message);
        return new IllegalArgumentException(message, cause);
    }
}
