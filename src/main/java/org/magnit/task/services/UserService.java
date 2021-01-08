package org.magnit.task.services;

import org.magnit.task.entities.Idea;
import org.magnit.task.entities.IdeaStatus;
import org.magnit.task.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserService{

    @Value("${upload.avatar.path}")
    private String AVATAR_PATH;

    public void uploadAvatar(MultipartFile avatar, User user){
        if(!Objects.requireNonNull(avatar.getOriginalFilename()).isEmpty()) {

            String fileName = UUID.randomUUID() + "_"
                    + StringUtils.cleanPath(Objects.requireNonNull(avatar.getOriginalFilename()));

            try {
                Path path = Paths.get(AVATAR_PATH + fileName);
                //Files.copy(avatar.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
avatar.transferTo(path);
                if(Files.exists(Paths.get(AVATAR_PATH + user.getAvatarPath())))
                    Files.delete(Paths.get(AVATAR_PATH + user.getAvatarPath()));

                user.setAvatarPath("/uploads/avatar/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int ideasCount(User user){

        return user.getIdeas().size();
    }

    public int ideaStatusCount(User user, IdeaStatus status){

        int count = 0;

        for(Idea idea : user.getIdeas())
            if(idea.getStatus() == status)
                count++;

        return count;
    }
}
