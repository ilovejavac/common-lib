package com.dev.lib.local.task.message.domain.service.impl;

import com.dev.lib.local.task.message.domain.adapter.ILocalTaskMessageAdapt;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import com.dev.lib.local.task.message.domain.service.ILocalTaskDataService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LocalTaskDataService implements ILocalTaskDataService {

    @Resource
    private ILocalTaskMessageAdapt adapt;

    @Override
    public List<TaskMessageEntityCommand> selectByHouseNumber(List<Integer> houseNumbers, String taskId, Integer limit) {

        return adapt.selectByHouseNumber(
                houseNumbers,
                taskId,
                limit
        );
    }

    @Override
    public String selectMinIdByHouseNumber(List<Integer> houseNumbers) {

        return adapt.selectMinIdByHouseNumber(houseNumbers);
    }

}
