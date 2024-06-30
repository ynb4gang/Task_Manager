package hexlet.code.service;

import hexlet.code.dto.TaskCreateDTO;
import hexlet.code.dto.TaskDTO;
import hexlet.code.dto.TaskUpdateDTO;
import hexlet.code.exception.ResourceNotFoundException;
import hexlet.code.mapper.TaskMapper;
import hexlet.code.model.Label;
import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.model.User;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.repository.UserRepository;
import hexlet.code.dto.TaskFilterSearchParameters;
import hexlet.code.service.filter.TaskFilterSearchParametersSpecification;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final TaskMapper taskMapper;

    public List<TaskDTO> getAll(TaskFilterSearchParameters params) {
        TaskFilterSearchParametersSpecification specification =
                new TaskFilterSearchParametersSpecification(params);

        return taskRepository.findAll(specification).stream().map(taskMapper::map).toList();
    }

    public TaskDTO findById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id %d not found".formatted(id)));
        return taskMapper.map(task);
    }

    public TaskDTO create(TaskCreateDTO taskCreateDTO) {
        Task task = taskMapper.create(taskCreateDTO);
        taskRepository.save(task);
        TaskStatus taskStatus = task.getTaskStatus();
        taskStatus.addTask(task);
        User user = task.getUser();
        if (user != null) {
            user.addTask(task);
        }
        List<Label> labels = task.getLabels();
        if (labels != null || !labels.isEmpty()) {
            labels.stream()
                    .forEach(l -> {
                        l.addTask(task);
                    });
        }
        return taskMapper.map(task);
    }

    public TaskDTO update(Long id, TaskUpdateDTO taskUpdateDTO) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id %d not found".formatted(id)));
        final User oldUser = task.getUser();
        final TaskStatus oldTaskStatus = task.getTaskStatus();
        final List<Label> oldLabels = task.getLabels();

        taskMapper.update(taskUpdateDTO, task);

        final User newUser = task.getUser();
        final TaskStatus newTaskStatus = task.getTaskStatus();
        final List<Label> newLabels = task.getLabels();

        taskRepository.save(task);

        if (!Objects.equals(newUser, oldUser)) {
            if (oldUser != null) {
                oldUser.removeTask(task);
            }
            if (newUser != null) {
                newUser.addTask(task);
            }
        }
        if (!Objects.equals(newTaskStatus, oldTaskStatus)) {
            oldTaskStatus.removeTask(task);
            newTaskStatus.addTask(task);
        }
        if (newLabels.retainAll(oldLabels)) {
            if (!oldLabels.isEmpty()) {
                oldLabels.stream()
                        .forEach(l -> {
                            l.removeTask(task);
                        });
            }
            if (!newLabels.isEmpty()) {
                newLabels.stream()
                        .forEach(l -> {
                            l.addTask(task);
                        });
            }
        }
        return taskMapper.map(task);
    }

    public void delete(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id %d not found".formatted(id)));
        User user = task.getUser();
        if (user != null) {
            user.removeTask(task);
            userRepository.save(user);
        }
        TaskStatus taskStatus = task.getTaskStatus();
        taskStatus.removeTask(task);
        taskStatusRepository.save(taskStatus);
        taskRepository.deleteById(id);
    }
}