package com.onlinedrive.staas.mapper;

import com.onlinedrive.staas.dto.FolderDTO;
import com.onlinedrive.staas.dto.UserDTO;
import com.onlinedrive.staas.model.Folder;
import com.onlinedrive.staas.model.User;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-17T22:15:39-0800",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251118-1623, environment: Java 25.0.1 (Eclipse Adoptium)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Autowired
    private FolderMapper folderMapper;

    @Override
    public UserDTO toDto(User user) {
        if ( user == null ) {
            return null;
        }

        String id = null;
        String username = null;
        long storageQuotaBytes = 0L;
        long usedStorageBytes = 0L;
        List<FolderDTO> folders = null;

        id = user.getId();
        username = user.getUsername();
        storageQuotaBytes = user.getStorageQuotaBytes();
        usedStorageBytes = user.getUsedStorageBytes();
        folders = folderListToFolderDTOList( user.getFolders() );

        UserDTO userDTO = new UserDTO( id, username, storageQuotaBytes, usedStorageBytes, folders );

        return userDTO;
    }

    protected List<FolderDTO> folderListToFolderDTOList(List<Folder> list) {
        if ( list == null ) {
            return null;
        }

        List<FolderDTO> list1 = new ArrayList<FolderDTO>( list.size() );
        for ( Folder folder : list ) {
            list1.add( folderMapper.toDto( folder ) );
        }

        return list1;
    }
}
