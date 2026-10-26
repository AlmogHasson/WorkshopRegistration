package patmal.workshops.model.impl;

import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Workshop;

public class WorkShopImpl implements Workshop {
    private final Integer id;
    private final String name;
    private final String description;
    private Instructor instructor;

    public WorkShopImpl(Integer id, String name, String description, Instructor instructor) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.instructor = instructor;
    }

    public void setInstructor(Instructor instructor) {
        this.instructor = instructor;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Instructor getInstructor() {
        return instructor;
    }
}
