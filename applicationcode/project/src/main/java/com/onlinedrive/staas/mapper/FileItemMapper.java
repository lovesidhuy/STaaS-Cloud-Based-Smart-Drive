package com.onlinedrive.staas.mapper;

import com.onlinedrive.staas.dto.FileItemDTO;
import com.onlinedrive.staas.model.FileItem;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface FileItemMapper {


    FileItemDTO toDto(FileItem fileItem);
}
