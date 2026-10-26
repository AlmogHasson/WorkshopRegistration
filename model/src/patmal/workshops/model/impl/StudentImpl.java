package patmal.workshops.model.impl;

import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StudentImpl extends PersonImpl implements Student {
    private TeamImpl team;

    public StudentImpl(String id, String name, String email) {
        super(id, name, email);
        this.team = null;
    }

    public void setTeam(TeamImpl team) {
        this.team = team;
    }

    private List<Student> getTeamMatesList() {
        if (team != null) {
            return team.getStudents().stream()
                    .filter(s -> !s.equals(this))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public String getTeamName() {
        return team != null ? team.getName() : null;
    }

    @Override
    public Instructor getInstructor() {
        if (team == null) {
            return null;
        }

        return team.getWorkshop().getInstructor();
    }

    @Override
    public List<Student> getTeamMates() {
        if (team == null) {
            return List.of();
        }

        return team.getStudents().stream()
                .filter(student -> !Objects.equals(student.getId(), getId()))
                .toList();
    }
}
