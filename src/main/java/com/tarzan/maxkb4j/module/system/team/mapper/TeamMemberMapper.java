package com.tarzan.maxkb4j.module.system.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.system.team.domain.entity.TeamMemberEntity;
import com.tarzan.maxkb4j.module.system.team.domain.vo.MemberVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 12:42:39
 */
@Mapper
public interface TeamMemberMapper extends BaseMapper<TeamMemberEntity>{

    List<MemberVO> getByUserId(String userId);
}
