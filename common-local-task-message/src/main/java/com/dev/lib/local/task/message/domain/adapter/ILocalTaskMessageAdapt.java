package com.dev.lib.local.task.message.domain.adapter;

import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;

import java.util.List;

public interface ILocalTaskMessageAdapt {
    void saveMessage(TaskMessageEntityCommand cmd);

    void updateTaskStatusToSuccess(String taskId);

    void updateTaskStatusToFailed(String taskId);

    List<TaskMessageEntityCommand> selectByHouseNumber(List<Integer> houseNumbers, String taskId, Integer limit);

    String selectMinIdByHouseNumber(List<Integer> houseNumbers);
}
