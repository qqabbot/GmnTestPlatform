package com.testing.automation.controller;

import com.testing.automation.model.ScheduledTask;
import com.testing.automation.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "*")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @GetMapping
    public List<ScheduledTask> getAll() {
        return scheduleService.findAll();
    }

    @PostMapping
    public ScheduledTask create(@RequestBody ScheduledTask task) {
        return scheduleService.create(task);
    }

    @PutMapping("/{id}")
    public ScheduledTask update(@PathVariable Long id, @RequestBody ScheduledTask task) {
        return scheduleService.update(id, task);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pause")
    public ScheduledTask pause(@PathVariable Long id) {
        return scheduleService.pause(id);
    }

    @PostMapping("/{id}/resume")
    public ScheduledTask resume(@PathVariable Long id) {
        return scheduleService.resume(id);
    }
}
