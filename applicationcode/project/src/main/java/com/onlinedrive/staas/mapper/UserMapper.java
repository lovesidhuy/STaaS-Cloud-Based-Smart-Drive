package com.onlinedrive.staas.mapper;

import com.onlinedrive.staas.dto.UserDTO;
import com.onlinedrive.staas.model.User;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring", uses = {FolderMapper.class})
public interface UserMapper {


    UserDTO toDto(User user);
}
