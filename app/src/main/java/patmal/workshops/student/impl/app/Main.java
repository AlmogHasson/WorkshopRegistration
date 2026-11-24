package patmal.workshops.student.impl.app;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Workshop;
import patmal.workshops.student.impl.logic.WorkshopSystemImpl;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar workshop-cli.jar <students.csv>");
            return;
        }

        WorkshopSystem system = new WorkshopSystemImpl();
        List<Student> students = system.importStudentDataFromFile(new File(args[0]));
        if (students.size() < 3) {
            throw new IllegalArgumentException("The demo needs at least three students.");
        }

        Instructor instructor = system.createInstructor("i1", "Dana Levi", "dana@univ.edu");
        Workshop java = system.createWorkshop("Java Workshop", "Practice Java basics");
        Workshop design = system.createWorkshop("Design Workshop", "Practice class design");
        system.associateWorkshopWithInstructor(instructor, java);
        system.associateWorkshopWithInstructor(instructor, design);

        List<String> memberIds = List.of(students.get(0).getId(), students.get(1).getId());
        system.createTeam("Alpha", java, memberIds);
        LOGGER.info("Imported {} students and assigned two to team Alpha.", students.size());

        system.exportInstructorsDataToJson(new File("instructors.json"));
        system.exportStudentsDataToJson(new File("students.json"));
        system.exportTeamsDataToJson(new File("teams.json"));
        LOGGER.info("Wrote instructors.json, students.json and teams.json to {}",
                Path.of("").toAbsolutePath());
    }
}