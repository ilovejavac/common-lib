package com.dev.lib.local.task.message.domain.service;

import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;

import java.util.List;

public interface ILocalTaskDataService {

    List<TaskMessageEntityCommand> selectByHouseNumber(List<Integer> houseNumbers, String taskId, Integer limit);

    String selectMinIdByHouseNumber(List<Integer> houseNumbers);

}
