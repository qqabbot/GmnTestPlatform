package com.testing.automation.service;

import com.testing.automation.mapper.ProjectMapper;
import com.testing.automation.mapper.TestModuleMapper;
import com.testing.automation.model.Project;
import com.testing.automation.model.TestModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectService {

    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private TestModuleMapper moduleMapper;

    public List<Project> getAllProjects() {
        List<Project> projects = projectMapper.findAll();
        for (Project project : projects) {
            project.setModules(moduleMapper.findByProjectId(project.getId()));
        }
        return projects;
    }

    public Project createProject(Project project) {
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.insert(project);
        return project;
    }

    public Project getProjectById(Long id) {
        Project project = projectMapper.findById(id);
        if (project != null) {
            project.setModules(moduleMapper.findByProjectId(id));
        }
        return project;
    }

    public Project updateProject(Long id, Project projectDetails) {
        Project project = getProjectById(id);
        if (project != null) {
            project.setProjectName(projectDetails.getProjectName());
            project.setDescription(projectDetails.getDescription());
            project.setUpdatedAt(LocalDateTime.now());
            projectMapper.update(project);
        }
        return project;
    }

    public void deleteProject(Long id) {
        projectMapper.deleteById(id);
    }
}
