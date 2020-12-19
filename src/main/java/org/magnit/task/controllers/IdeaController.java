package org.magnit.task.controllers;

import org.magnit.task.entities.*;
import org.magnit.task.repositories.IdeaRepository;
import org.magnit.task.repositories.NotificationRepository;
import org.magnit.task.repositories.UserRepository;
import org.magnit.task.services.MailSender;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping("ideas")
public class IdeaController {

    private final IdeaRepository ideaRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final MailSender mailSender;

    public IdeaController(IdeaRepository ideaRepository, UserRepository userRepository, NotificationRepository notificationRepository, MailSender mailSender) {
        this.ideaRepository = ideaRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
    }

    @GetMapping
    public String getIdeas(@PageableDefault(sort = {"id"}, direction = Sort.Direction.DESC, size = 5) Pageable pageable, Model model){

        Page<Idea> ideas = ideaRepository.findAll(pageable);
        model.addAttribute("ideas", ideas);
        model.addAttribute("pageable", pageable);

        return "ideas";
    }

    @GetMapping("/idea-{idea}")
    public String getIdea(@PathVariable Idea idea, Model model){
        model.addAttribute("idea", idea);

        return "idea";
    }

    @GetMapping("/new")
    public String getNew(Model model){
        model.addAttribute("idea", new Idea());
        return "new";
    }

    @GetMapping("/idea-{idea}/edit")
    public String getEdit(@PathVariable Idea idea, Model model){
        model.addAttribute("idea", idea);

        return "edit";
    }

    // It need for optimization

    private final String UPLOAD_IMAGE_DIR = "/home/koshey/Документы/task/src/main/resources/uploads/images/";
    private final String UPLOAD_FILE_DIR = "/home/koshey/Документы/task/src/main/resources/uploads/files/";

    @PostMapping("/add")
    public String uploadFile(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) List<MultipartFile> images,
            @RequestParam(required = false) List<MultipartFile> files,
            Principal principal
    ) {

        User user = userRepository.findByUsername(principal.getName());

        Idea idea = new Idea(title, description, IdeaStatus.LOOKING, new Date(), user);

        // Upload Images

        for(MultipartFile pair : images) {
            if (Objects.requireNonNull(pair.getOriginalFilename()).isEmpty())
                continue;

            // normalize the file path
            String fileName = UUID.randomUUID() + "_" + StringUtils.cleanPath(Objects.requireNonNull(pair.getOriginalFilename()));

            // save the file on the local file system
            try {
                Path path = Paths.get(UPLOAD_IMAGE_DIR + fileName);
                Files.copy(pair.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                idea.addImage(pair.getOriginalFilename(), fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Upload Files

        for(MultipartFile pair : files) {
            if (Objects.requireNonNull(pair.getOriginalFilename()).isEmpty())
                continue;

            // normalize the file path
            String fileName = UUID.randomUUID() + "_" + StringUtils.cleanPath(Objects.requireNonNull(pair.getOriginalFilename()));

            // save the file on the local file system
            try {
                Path path = Paths.get(UPLOAD_FILE_DIR + fileName);
                Files.copy(pair.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                idea.addFile(pair.getOriginalFilename(), fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ideaRepository.save(idea);
        ideaRepository.flush();

        return "redirect:/";
    }

    @PostMapping("setStatus-{idea}")
    public String setStatus(@PathVariable Idea idea, @RequestParam String status){
        IdeaStatus ideaStatus = IdeaStatus.getValueByName(status);
        idea.setStatus(IdeaStatus.getValueByName(status));
        ideaRepository.save(idea);

        User user = idea.getUser();

        Notification notification = new Notification(
                "Изменение статуса",
                "Статус вашей идеи с заголовком " + idea.getTitle() + " был изменен.",
                user, idea.getId());

        notificationRepository.save(notification);

        mailSender.send(
                idea.getUser().getUsername(),
                "Статус идеи изменен",
                "Статус вашей идеи " + idea.getTitle()
                        + " #" + idea.getId() + " изменен на " + ideaStatus.getName()
                        + ". Просмотреть идею: " + "http://localhost:4040/ideas/idea-" + idea.getId()
        );

        return "redirect:idea-" + idea.getId();
    }

    @PostMapping("setLike-{idea}")
    public String setLike(@PathVariable Idea idea, Principal principal){
        User user = userRepository.findByUsername(principal.getName());

        idea.like(user);
        ideaRepository.save(idea);

        return "redirect:";
    }

    @ModelAttribute
    public void getHeader(Principal principal, Model model){

        User user = userRepository.findByUsername(principal.getName());
        model.addAttribute("user", user);

        model.addAttribute("userNotifies", user.getNotifications());

        List<Notification> notifications = notificationRepository.findByLookAndUser(false, user);

        model.addAttribute("userNotifyCount", notifications.size());

        for(IdeaStatus pair : IdeaStatus.values()){
            model.addAttribute("status", pair);
        }
        for(Roles pair : Roles.values()){
            model.addAttribute("roles", pair);
        }
    }
}
