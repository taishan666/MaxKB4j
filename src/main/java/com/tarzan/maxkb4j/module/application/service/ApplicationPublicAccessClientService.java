package com.tarzan.maxkb4j.module.application.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationPublicAccessClientEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationPublicAccessClientMapper;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationPublicAccessClientStatisticsVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-29 10:34:03
 */
@Service
public class ApplicationPublicAccessClientService extends ServiceImpl<ApplicationPublicAccessClientMapper, ApplicationPublicAccessClientEntity>{

    public List<ApplicationPublicAccessClientStatisticsVO> statistics(String appId, ChatQueryDTO query) {
        return baseMapper.statistics(appId,query);
    }
}
