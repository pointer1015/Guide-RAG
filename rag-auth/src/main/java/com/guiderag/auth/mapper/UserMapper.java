package com.guiderag.auth.mapper;

import com.guiderag.auth.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface UserMapper {

    User selectByEmail(@Param("email") String email);
    
    User selectByDisplayName(@Param("displayName") String displayName);
    
    User selectById(@Param("id") Long id);
    
    int insert(User user);

    int updateById(User user);
}
