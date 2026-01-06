package com.onlinedrive.staas.mapper;

import com.onlinedrive.staas.dto.FolderDTO;
import com.onlinedrive.staas.model.Folder;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring", uses = {FileItemMapper.class})
public interface FolderMapper {


    FolderDTO toDto(Folder folder);
}
