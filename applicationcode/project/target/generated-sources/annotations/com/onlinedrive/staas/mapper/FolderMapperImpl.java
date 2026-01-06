package com.onlinedrive.staas.mapper;

import com.onlinedrive.staas.dto.FileItemDTO;
import com.onlinedrive.staas.dto.FolderDTO;
import com.onlinedrive.staas.model.FileItem;
import com.onlinedrive.staas.model.Folder;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-17T22:15:38-0800",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251118-1623, environment: Java 25.0.1 (Eclipse Adoptium)"
)
@Component
public class FolderMapperImpl implements FolderMapper {

    @Autowired
    private FileItemMapper fileItemMapper;

    @Override
    public FolderDTO toDto(Folder folder) {
        if ( folder == null ) {
            return null;
        }

        String id = null;
        String name = null;
        long creationTimestamp = 0L;
        List<FileItemDTO> files = null;

        id = folder.getId();
        name = folder.getName();
        creationTimestamp = folder.getCreationTimestamp();
        files = fileItemListToFileItemDTOList( folder.getFiles() );

        FolderDTO folderDTO = new FolderDTO( id, name, creationTimestamp, files );

        return folderDTO;
    }

    protected List<FileItemDTO> fileItemListToFileItemDTOList(List<FileItem> list) {
        if ( list == null ) {
            return null;
        }

        List<FileItemDTO> list1 = new ArrayList<FileItemDTO>( list.size() );
        for ( FileItem fileItem : list ) {
            list1.add( fileItemMapper.toDto( fileItem ) );
        }

        return list1;
    }
}
