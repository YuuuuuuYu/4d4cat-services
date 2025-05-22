package com.services.common.presentation;

import com.services.common.domain.DataStorage;
import com.services.common.util.RandomUtils;
import com.services.pixabay.domain.PixabayMusic;
import com.services.pixabay.domain.PixabayVideo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class MainController {

    @GetMapping("/video")
    public String video(Model model) {
        List<PixabayVideo> videos = (List<PixabayVideo>) DataStorage.getData("pixabayVideos");
        if (videos != null) {
            model.addAttribute("pixabayVideo", videos.get(RandomUtils.generateRandomInt(videos.size())));
        }
        return "video";
    }

    @GetMapping("/music")
    public String musicPage(Model model) {
        List<PixabayMusic> music = (List<PixabayMusic>) DataStorage.getData("pixabayMusic");
        if (music != null) {
            model.addAttribute("pixabayMusic", music.get(RandomUtils.generateRandomInt(music.size())));
        }
        return "music";
    }
}
