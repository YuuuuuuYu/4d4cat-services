package com.services.common.presentation;

import com.services.common.domain.DataStorage;
import com.services.common.util.RandomUtils;
import com.services.pixabay.domain.PixabayVideo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class MainController {

    @GetMapping("/")
    public String index(Model model) {
        List<PixabayVideo> videos = (List<PixabayVideo>) DataStorage.getData("pixabayVideos");
        int target = RandomUtils.generateRandomInt(videos.size());

        model.addAttribute("pixabayVideo", videos.get(target));
        return "index";
    }
}
