package com.onlinedrive.staas.mapper;

import com.onlinedrive.staas.dto.FileItemDTO;
import com.onlinedrive.staas.model.FileItem;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-17T22:15:39-0800",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251118-1623, environment: Java 25.0.1 (Eclipse Adoptium)"
)
@Component
public class FileItemMapperImpl implements FileItemMapper {

    @Override
    public FileItemDTO toDto(FileItem fileItem) {
        if ( fileItem == null ) {
            return null;
        }

        String id = null;
        String name = null;
        long actualSize = 0L;
        String fileExtension = null;
        String mimeType = null;

        id = fileItem.getId();
        name = fileItem.getName();
        actualSize = fileItem.getActualSize();
        fileExtension = fileItem.getFileExtension();
        mimeType = fileItem.getMimeType();

        FileItemDTO fileItemDTO = new FileItemDTO( id, name, actualSize, fileExtension, mimeType );

        return fileItemDTO;
    }
}
